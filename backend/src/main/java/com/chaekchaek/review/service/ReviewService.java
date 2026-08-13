package com.chaekchaek.review.service;

import com.chaekchaek.common.auth.CurrentMemberIdProvider;
import com.chaekchaek.review.domain.Reply;
import com.chaekchaek.review.domain.ReplyReaction;
import com.chaekchaek.review.domain.Review;
import com.chaekchaek.review.domain.ReviewReaction;
import com.chaekchaek.review.dto.AuthorResponse;
import com.chaekchaek.review.dto.PageResponse;
import com.chaekchaek.review.dto.ReactionResponse;
import com.chaekchaek.review.dto.ReplyCreateRequest;
import com.chaekchaek.review.dto.ReplyResponse;
import com.chaekchaek.review.dto.ReplyUpdateRequest;
import com.chaekchaek.review.dto.ReviewCreateRequest;
import com.chaekchaek.review.dto.ReviewResponse;
import com.chaekchaek.review.dto.ReviewUpdateRequest;
import com.chaekchaek.review.exception.ReviewErrorCode;
import com.chaekchaek.review.exception.ReviewException;
import com.chaekchaek.review.library.ReadingRecordCoordinator;
import com.chaekchaek.review.repository.ReplyReactionRepository;
import com.chaekchaek.review.repository.ReplyRepository;
import com.chaekchaek.review.repository.ReviewReactionRepository;
import com.chaekchaek.review.repository.ReviewRepository;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final int PAGE_SIZE = 10;
    private static final int RECENT_REPLY_LIMIT = 3;

    private final ReviewRepository reviewRepository;
    private final ReplyRepository replyRepository;
    private final ReviewReactionRepository reviewReactionRepository;
    private final ReplyReactionRepository replyReactionRepository;
    private final CurrentMemberIdProvider currentMemberIdProvider;
    private final ReadingRecordCoordinator readingRecordCoordinator;

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> findReviews(long bookId, int page, Feed feed, ReviewSort sort) {
        Long memberId = currentMemberIdOrNull();
        if (feed == Feed.MINE && memberId == null) {
            throw new ReviewException(ReviewErrorCode.UNAUTHORIZED);
        }
        List<Review> reviews = sort == ReviewSort.POPULAR
                ? findPopularReviews(bookId, memberId, feed, page)
                : findPagedReviews(bookId, memberId, feed, sort, page);
        long totalCount = countReviews(bookId, memberId, feed, sort);
        return new PageResponse<>(totalCount, nextPage(totalCount, page), toReviewResponses(reviews, memberId));
    }

    private List<Review> findPagedReviews(long bookId, Long memberId, Feed feed, ReviewSort sort, int page) {
        Pageable pageable = PageRequest.of(page - 1, PAGE_SIZE, sort.toSpringSort());
        Page<Review> reviews = feed == Feed.MINE
                ? reviewRepository.findByBookIdAndMemberId(bookId, memberId, pageable)
                : reviewRepository.findByBookId(bookId, pageable);
        return reviews.getContent();
    }

    private List<Review> findPopularReviews(long bookId, Long memberId, Feed feed, int page) {
        List<Review> reviews = feed == Feed.MINE
                ? reviewRepository.findByBookIdAndMemberId(bookId, memberId)
                : reviewRepository.findByBookId(bookId);
        Map<Long, Long> replies = replyCounts(reviews.stream().map(Review::getId).toList());
        Map<Long, Long> reactions = reviewReactionCounts(reviews.stream().map(Review::getId).toList());
        return reviews.stream()
                .sorted(Comparator.comparingLong((Review review) -> reactions.getOrDefault(review.getId(), 0L)
                        + replies.getOrDefault(review.getId(), 0L)).reversed()
                        .thenComparing(Review::getId, Comparator.reverseOrder()))
                .skip((long) (page - 1) * PAGE_SIZE)
                .limit(PAGE_SIZE)
                .toList();
    }

    private long countReviews(long bookId, Long memberId, Feed feed, ReviewSort sort) {
        if (sort == ReviewSort.POPULAR) {
            return feed == Feed.MINE
                    ? reviewRepository.findByBookIdAndMemberId(bookId, memberId).size()
                    : reviewRepository.findByBookId(bookId).size();
        }
        Pageable oneItem = PageRequest.of(0, 1);
        return feed == Feed.MINE
                ? reviewRepository.findByBookIdAndMemberId(bookId, memberId, oneItem).getTotalElements()
                : reviewRepository.findByBookId(bookId, oneItem).getTotalElements();
    }

    @Transactional
    public ReviewResponse createReview(long bookId, ReviewCreateRequest request) {
        long memberId = currentMemberIdProvider.getCurrentMemberId();
        validateReviewCreate(request);
        validatePage(request.currentPage(), request.totalPages());
        readingRecordCoordinator.recordReview(memberId, bookId, request.currentPage(), request.totalPages());
        Review review = reviewRepository.save(Review.create(bookId, memberId, request.content(), request.quote(),
                request.chapter(), request.currentPage(), request.spoiler(), false));
        return toReviewResponse(review, memberId, Map.of(), Map.of(), Map.of(), Map.of(), Set.of(), Set.of());
    }

    @Transactional
    public ReviewResponse updateReview(long reviewId, ReviewUpdateRequest request) {
        if (!request.hasUpdate()) {
            throw new ReviewException(ReviewErrorCode.INVALID_REQUEST);
        }
        validateReviewUpdate(request);
        long memberId = currentMemberIdProvider.getCurrentMemberId();
        Review review = getReview(reviewId);
        review.assertModifiableBy(memberId);
        review.update(request.isContentPresent() ? request.getContent() : review.getContent(),
                request.isQuotePresent() ? request.getQuote() : review.getQuote(),
                request.isChapterPresent() ? request.getChapter() : review.getChapter(),
                request.isCurrentPagePresent() ? request.getCurrentPage() : review.getCurrentPage(),
                request.isSpoilerPresent() ? request.getIsSpoiler() : review.isSpoiler());
        return toReviewResponse(review, memberId, Map.of(), Map.of(), Map.of(), Map.of(), Set.of(), Set.of());
    }

    @Transactional
    public void deleteReview(long reviewId) {
        getReview(reviewId).deleteBy(currentMemberIdProvider.getCurrentMemberId());
    }

    @Transactional(readOnly = true)
    public PageResponse<ReplyResponse> findReplies(long reviewId, int page) {
        getReview(reviewId);
        Long memberId = currentMemberIdOrNull();
        Page<Reply> replies = replyRepository.findByReviewId(reviewId, PageRequest.of(page - 1, PAGE_SIZE,
                Sort.by("createdAt").ascending().and(Sort.by("id").ascending())));
        List<ReplyResponse> items = toReplyResponses(replies.getContent(), memberId);
        return new PageResponse<>(replies.getTotalElements(), nextPage(replies.getTotalElements(), page), items);
    }

    @Transactional
    public ReplyResponse createReply(long reviewId, ReplyCreateRequest request) {
        long memberId = currentMemberIdProvider.getCurrentMemberId();
        Review review = getReview(reviewId);
        if (review.isDeleted()) {
            throw new ReviewException(ReviewErrorCode.DELETED_RESOURCE);
        }
        Reply reply = replyRepository.save(Reply.create(reviewId, memberId, request.content(), false));
        return toReplyResponse(reply, memberId, 0, false);
    }

    @Transactional
    public ReplyResponse updateReply(long replyId, ReplyUpdateRequest request) {
        long memberId = currentMemberIdProvider.getCurrentMemberId();
        Reply reply = getReply(replyId);
        reply.updateBy(memberId, request.content());
        return toReplyResponse(reply, memberId, replyReactionRepository.countByReplyId(replyId), false);
    }

    @Transactional
    public void deleteReply(long replyId) {
        getReply(replyId).deleteBy(currentMemberIdProvider.getCurrentMemberId());
    }

    @Transactional
    public ReactionResponse createReviewReaction(long reviewId) {
        long memberId = currentMemberIdProvider.getCurrentMemberId();
        Review review = getReview(reviewId);
        if (review.isDeleted()) {
            throw new ReviewException(ReviewErrorCode.DELETED_RESOURCE);
        }
        ReviewReaction.ReviewReactionId id = reviewReactionId(reviewId, memberId);
        if (reviewReactionRepository.existsById(id)) {
            throw new ReviewException(ReviewErrorCode.REACTION_ALREADY_EXISTS);
        }
        try {
            reviewReactionRepository.saveAndFlush(new ReviewReaction(reviewId, memberId));
        } catch (DataIntegrityViolationException exception) {
            throw new ReviewException(ReviewErrorCode.REACTION_ALREADY_EXISTS);
        }
        return new ReactionResponse(reviewReactionRepository.countByReviewId(reviewId), true);
    }

    @Transactional
    public void deleteReviewReaction(long reviewId) {
        getReview(reviewId);
        long memberId = currentMemberIdProvider.getCurrentMemberId();
        reviewReactionRepository.deleteById(reviewReactionId(reviewId, memberId));
    }

    @Transactional
    public ReactionResponse createReplyReaction(long replyId) {
        long memberId = currentMemberIdProvider.getCurrentMemberId();
        Reply reply = getReply(replyId);
        if (reply.isDeleted() || getReview(reply.getReviewId()).isDeleted()) {
            throw new ReviewException(ReviewErrorCode.DELETED_RESOURCE);
        }
        ReplyReaction.ReplyReactionId id = replyReactionId(replyId, memberId);
        if (replyReactionRepository.existsById(id)) {
            throw new ReviewException(ReviewErrorCode.REACTION_ALREADY_EXISTS);
        }
        try {
            replyReactionRepository.saveAndFlush(new ReplyReaction(replyId, memberId));
        } catch (DataIntegrityViolationException exception) {
            throw new ReviewException(ReviewErrorCode.REACTION_ALREADY_EXISTS);
        }
        return new ReactionResponse(replyReactionRepository.countByReplyId(replyId), true);
    }

    @Transactional
    public void deleteReplyReaction(long replyId) {
        getReply(replyId);
        long memberId = currentMemberIdProvider.getCurrentMemberId();
        replyReactionRepository.deleteById(replyReactionId(replyId, memberId));
    }

    private List<ReviewResponse> toReviewResponses(List<Review> reviews, Long memberId) {
        List<Long> reviewIds = reviews.stream().map(Review::getId).toList();
        Map<Long, Long> replyCounts = replyCounts(reviewIds);
        Map<Long, Long> reactionCounts = reviewReactionCounts(reviewIds);
        List<Reply> replyList = reviewIds.isEmpty() ? List.of() : replyRepository.findByReviewIdInOrderByCreatedAtDescIdDesc(reviewIds);
        Map<Long, List<Reply>> recentReplies = replyList.stream().collect(Collectors.groupingBy(Reply::getReviewId));
        Map<Long, Long> replyReactionCounts = replyReactionCounts(replyList.stream().map(Reply::getId).toList());
        Set<Long> likedReviews = likedReviewIds(reviewIds, memberId);
        Set<Long> likedReplies = likedReplyIds(replyList.stream().map(Reply::getId).toList(), memberId);
        return reviews.stream().map(review -> toReviewResponse(review, memberId, replyCounts, reactionCounts,
                recentReplies, replyReactionCounts, likedReviews, likedReplies)).toList();
    }

    private ReviewResponse toReviewResponse(Review review, Long memberId, Map<Long, Long> replyCounts,
                                            Map<Long, Long> reactionCounts, Map<Long, List<Reply>> recentReplies,
                                            Map<Long, Long> replyReactionCounts, Set<Long> likedReviews,
                                            Set<Long> likedReplies) {
        List<ReplyResponse> replies = recentReplies.getOrDefault(review.getId(), List.of()).stream()
                .limit(RECENT_REPLY_LIMIT)
                .sorted(Comparator.comparing(Reply::getCreatedAt).thenComparing(Reply::getId))
                .map(reply -> toReplyResponse(reply, memberId, replyReactionCounts.getOrDefault(reply.getId(), 0L),
                        likedReplies.contains(reply.getId())))
                .toList();
        return new ReviewResponse(review.getId(), review.getContent(), review.getQuote(), review.getChapter(),
                review.getCurrentPage(), review.isSpoiler(), review.isDeleted(), review.getCreatedAt(),
                authorOf(review.getMemberId(), review.isAnonymous(), memberId),
                reactionCounts.getOrDefault(review.getId(), 0L), likedReviews.contains(review.getId()),
                replyCounts.getOrDefault(review.getId(), 0L), replies);
    }

    private List<ReplyResponse> toReplyResponses(List<Reply> replies, Long memberId) {
        Map<Long, Long> reactions = replyReactionCounts(replies.stream().map(Reply::getId).toList());
        Set<Long> liked = likedReplyIds(replies.stream().map(Reply::getId).toList(), memberId);
        return replies.stream().map(reply -> toReplyResponse(reply, memberId,
                reactions.getOrDefault(reply.getId(), 0L), liked.contains(reply.getId()))).toList();
    }

    private ReplyResponse toReplyResponse(Reply reply, Long memberId, long likeCount, boolean likedByMe) {
        return new ReplyResponse(reply.getId(), reply.getContent(), reply.isDeleted(), reply.getCreatedAt(),
                authorOf(reply.getMemberId(), reply.isAnonymous(), memberId), likeCount, likedByMe);
    }

    private AuthorResponse authorOf(long authorId, boolean anonymous, Long currentMemberId) {
        String displayName = anonymous ? "익명-" + authorId : "회원-" + authorId;
        return new AuthorResponse(displayName, null, anonymous, currentMemberId != null && authorId == currentMemberId);
    }

    private Map<Long, Long> replyCounts(Collection<Long> reviewIds) {
        if (reviewIds.isEmpty()) return Map.of();
        return replyRepository.countByReviewIdInGroupByReviewId(reviewIds).stream()
                .collect(Collectors.toMap(ReplyRepository.ReplyCount::getReviewId, ReplyRepository.ReplyCount::getCount));
    }

    private Map<Long, Long> reviewReactionCounts(Collection<Long> reviewIds) {
        if (reviewIds.isEmpty()) return Map.of();
        return reviewReactionRepository.countByReviewIdInGroupByReviewId(reviewIds).stream()
                .collect(Collectors.toMap(ReviewReactionRepository.ReactionCount::getReviewId,
                        ReviewReactionRepository.ReactionCount::getCount));
    }

    private Map<Long, Long> replyReactionCounts(Collection<Long> replyIds) {
        if (replyIds.isEmpty()) return Map.of();
        return replyReactionRepository.countByReplyIdInGroupByReplyId(replyIds).stream()
                .collect(Collectors.toMap(ReplyReactionRepository.ReactionCount::getReplyId,
                        ReplyReactionRepository.ReactionCount::getCount));
    }

    private Set<Long> likedReviewIds(List<Long> reviewIds, Long memberId) {
        if (memberId == null || reviewIds.isEmpty()) return Set.of();
        return reviewReactionRepository.findByReviewIdInAndMemberId(reviewIds, memberId).stream()
                .map(ReviewReaction::getReviewId).collect(Collectors.toSet());
    }

    private Set<Long> likedReplyIds(List<Long> replyIds, Long memberId) {
        if (memberId == null || replyIds.isEmpty()) return Set.of();
        return replyReactionRepository.findByReplyIdInAndMemberId(replyIds, memberId).stream()
                .map(ReplyReaction::getReplyId).collect(Collectors.toSet());
    }

    private Review getReview(long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.REVIEW_NOT_FOUND));
    }

    private Reply getReply(long replyId) {
        return replyRepository.findById(replyId)
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.REPLY_NOT_FOUND));
    }

    private Long currentMemberIdOrNull() {
        try {
            return currentMemberIdProvider.getCurrentMemberId();
        } catch (ReviewException exception) {
            if (exception.getErrorCode() == ReviewErrorCode.UNAUTHORIZED) return null;
            throw exception;
        }
    }

    private Integer nextPage(long totalCount, int page) {
        return totalCount > (long) page * PAGE_SIZE ? page + 1 : null;
    }

    private void validateReviewUpdate(ReviewUpdateRequest request) {
        if (request.isContentPresent() && (request.getContent() == null || request.getContent().isBlank()
                || request.getContent().length() > 1000)) throw new ReviewException(ReviewErrorCode.INVALID_REQUEST);
        if (request.isQuotePresent() && request.getQuote() != null && (request.getQuote().isBlank()
                || request.getQuote().length() > 500)) throw new ReviewException(ReviewErrorCode.INVALID_REQUEST);
        if (request.isChapterPresent() && request.getChapter() != null && (request.getChapter().isBlank()
                || request.getChapter().length() > 255)) throw new ReviewException(ReviewErrorCode.INVALID_REQUEST);
        if (request.isSpoilerPresent() && request.getIsSpoiler() == null) throw new ReviewException(ReviewErrorCode.INVALID_REQUEST);
        if (request.isCurrentPagePresent() && request.getCurrentPage() != null) {
            validatePage(request.getCurrentPage(), request.isTotalPagesPresent() ? request.getTotalPages() : null);
        }
    }

    private void validateReviewCreate(ReviewCreateRequest request) {
        if (request.quote() != null && request.quote().isBlank()) {
            throw new ReviewException(ReviewErrorCode.INVALID_REQUEST);
        }
        if (request.chapter() != null && request.chapter().isBlank()) {
            throw new ReviewException(ReviewErrorCode.INVALID_REQUEST);
        }
    }

    private void validatePage(Integer currentPage, Integer totalPages) {
        if (currentPage != null && currentPage < 0) throw new ReviewException(ReviewErrorCode.INVALID_REQUEST);
        if (totalPages != null && totalPages <= 0) throw new ReviewException(ReviewErrorCode.INVALID_REQUEST);
        if (currentPage != null && totalPages != null && currentPage > totalPages) {
            throw new ReviewException(ReviewErrorCode.INVALID_REQUEST);
        }
    }

    private ReviewReaction.ReviewReactionId reviewReactionId(long reviewId, long memberId) {
        return new ReviewReaction.ReviewReactionId(reviewId, memberId);
    }

    private ReplyReaction.ReplyReactionId replyReactionId(long replyId, long memberId) {
        return new ReplyReaction.ReplyReactionId(replyId, memberId);
    }

    public enum Feed { ALL, MINE }

    public enum ReviewSort {
        PAGE(Sort.by(Sort.Order.asc("currentPage").nullsLast(), Sort.Order.desc("id"))),
        LATEST(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))),
        OLDEST(Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"))),
        POPULAR(Sort.unsorted());

        private final Sort springSort;

        ReviewSort(Sort springSort) { this.springSort = springSort; }
        public Sort toSpringSort() { return springSort; }
    }
}
