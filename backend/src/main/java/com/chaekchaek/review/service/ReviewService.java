package com.chaekchaek.review.service;

import com.chaekchaek.common.auth.CurrentMemberIdProvider;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.library.service.BookCommentCountReader;
import com.chaekchaek.review.book.ReviewBookReader;
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
import com.chaekchaek.review.library.ReadingRecordCoordinator;
import com.chaekchaek.review.member.ReviewMemberProfile;
import com.chaekchaek.review.member.ReviewMemberReader;
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
public class ReviewService implements BookCommentCountReader {

    private static final int PAGE_SIZE = 10;
    private static final int RECENT_REPLY_LIMIT = 3;

    private final ReviewRepository reviewRepository;
    private final ReplyRepository replyRepository;
    private final ReviewReactionRepository reviewReactionRepository;
    private final ReplyReactionRepository replyReactionRepository;
    private final CurrentMemberIdProvider currentMemberIdProvider;
    private final ReadingRecordCoordinator readingRecordCoordinator;
    private final ReviewBookReader reviewBookReader;
    private final ReviewMemberReader reviewMemberReader;

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> findReviews(long bookId, int page, Feed feed, ReviewSort sort) {
        reviewBookReader.validateBookExists(bookId);
        Long memberId = currentMemberIdOrNull();
        if (feed == Feed.MINE && memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
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
        reviewBookReader.validateBookExists(bookId);
        validateReviewCreate(request);
        validateRequestPage(request.currentPage(), request.totalPages());
        readingRecordCoordinator.recordReview(memberId, bookId, request.currentPage(), request.totalPages());
        ReviewMemberProfile memberProfile = memberProfileOf(memberId);
        Review review = reviewRepository.save(Review.create(bookId, memberId, request.content(), request.quote(),
                request.chapter(), request.currentPage(), request.spoiler(), memberProfile.anonymousEnabled()));
        return toReviewResponse(review, memberId, Map.of(), Map.of(), Map.of(), Map.of(), Set.of(), Set.of(),
                Map.of(memberId, memberProfile));
    }

    @Transactional
    public ReviewResponse updateReview(long reviewId, ReviewUpdateRequest request) {
        if (!request.hasUpdate()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        validateReviewUpdate(request);
        long memberId = currentMemberIdProvider.getCurrentMemberId();
        Review review = getReview(reviewId);
        review.assertModifiableBy(memberId);
        if (request.isCurrentPagePresent() && request.getCurrentPage() != null) {
            readingRecordCoordinator.validateReviewPage(review.getBookId(), request.getCurrentPage(),
                    request.isTotalPagesPresent() ? request.getTotalPages() : null);
        }
        review.update(request.isContentPresent() ? request.getContent() : review.getContent(),
                request.isQuotePresent() ? request.getQuote() : review.getQuote(),
                request.isChapterPresent() ? request.getChapter() : review.getChapter(),
                request.isCurrentPagePresent() ? request.getCurrentPage() : review.getCurrentPage(),
                request.isSpoilerPresent() ? request.getIsSpoiler() : review.isSpoiler());
        return toReviewResponse(review, memberId, Map.of(), Map.of(), Map.of(), Map.of(), Set.of(), Set.of(),
                Map.of(review.getMemberId(), memberProfileOf(review.getMemberId())));
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
            throw new BusinessException(ErrorCode.DELETED_RESOURCE);
        }
        ReviewMemberProfile memberProfile = memberProfileOf(memberId);
        Reply reply = replyRepository.save(Reply.create(
                reviewId, memberId, request.content(), memberProfile.anonymousEnabled()
        ));
        return toReplyResponse(reply, memberId, 0, false, Map.of(memberId, memberProfile));
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
            throw new BusinessException(ErrorCode.DELETED_RESOURCE);
        }
        ReviewReaction.ReviewReactionId id = reviewReactionId(reviewId, memberId);
        if (reviewReactionRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.REACTION_ALREADY_EXISTS);
        }
        try {
            reviewReactionRepository.saveAndFlush(new ReviewReaction(reviewId, memberId));
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.REACTION_ALREADY_EXISTS);
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
            throw new BusinessException(ErrorCode.DELETED_RESOURCE);
        }
        ReplyReaction.ReplyReactionId id = replyReactionId(replyId, memberId);
        if (replyReactionRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.REACTION_ALREADY_EXISTS);
        }
        try {
            replyReactionRepository.saveAndFlush(new ReplyReaction(replyId, memberId));
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.REACTION_ALREADY_EXISTS);
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
        List<Reply> replyList = reviewIds.isEmpty() ? List.of() : replyRepository.findRecentThreeByReviewIdIn(reviewIds);
        Map<Long, List<Reply>> recentReplies = replyList.stream().collect(Collectors.groupingBy(Reply::getReviewId));
        Map<Long, Long> replyReactionCounts = replyReactionCounts(replyList.stream().map(Reply::getId).toList());
        Set<Long> likedReviews = likedReviewIds(reviewIds, memberId);
        Set<Long> likedReplies = likedReplyIds(replyList.stream().map(Reply::getId).toList(), memberId);
        Map<Long, ReviewMemberProfile> memberProfiles = memberProfilesOf(reviews, replyList);
        return reviews.stream().map(review -> toReviewResponse(review, memberId, replyCounts, reactionCounts,
                recentReplies, replyReactionCounts, likedReviews, likedReplies, memberProfiles)).toList();
    }

    private ReviewResponse toReviewResponse(Review review, Long memberId, Map<Long, Long> replyCounts,
                                            Map<Long, Long> reactionCounts, Map<Long, List<Reply>> recentReplies,
                                            Map<Long, Long> replyReactionCounts, Set<Long> likedReviews,
                                            Set<Long> likedReplies, Map<Long, ReviewMemberProfile> memberProfiles) {
        List<ReplyResponse> replies = recentReplies.getOrDefault(review.getId(), List.of()).stream()
                .limit(RECENT_REPLY_LIMIT)
                .sorted(Comparator.comparing(Reply::getCreatedAt).thenComparing(Reply::getId))
                .map(reply -> toReplyResponse(reply, memberId, replyReactionCounts.getOrDefault(reply.getId(), 0L),
                        likedReplies.contains(reply.getId()), memberProfiles))
                .toList();
        return new ReviewResponse(review.getId(), review.getContent(), review.getQuote(), review.getChapter(),
                review.getCurrentPage(), review.isSpoiler(), review.isDeleted(), review.getCreatedAt(),
                authorOf(review.getMemberId(), review.isAnonymous(), memberId, memberProfiles),
                reactionCounts.getOrDefault(review.getId(), 0L), likedReviews.contains(review.getId()),
                replyCounts.getOrDefault(review.getId(), 0L), replies);
    }

    private List<ReplyResponse> toReplyResponses(List<Reply> replies, Long memberId) {
        Map<Long, Long> reactions = replyReactionCounts(replies.stream().map(Reply::getId).toList());
        Set<Long> liked = likedReplyIds(replies.stream().map(Reply::getId).toList(), memberId);
        Map<Long, ReviewMemberProfile> memberProfiles = memberProfilesOf(List.of(), replies);
        return replies.stream().map(reply -> toReplyResponse(reply, memberId,
                reactions.getOrDefault(reply.getId(), 0L), liked.contains(reply.getId()), memberProfiles)).toList();
    }

    private ReplyResponse toReplyResponse(Reply reply, Long memberId, long likeCount, boolean likedByMe) {
        return toReplyResponse(reply, memberId, likeCount, likedByMe,
                Map.of(reply.getMemberId(), memberProfileOf(reply.getMemberId())));
    }

    private ReplyResponse toReplyResponse(Reply reply, Long memberId, long likeCount, boolean likedByMe,
                                          Map<Long, ReviewMemberProfile> memberProfiles) {
        return new ReplyResponse(reply.getId(), reply.getContent(), reply.isDeleted(), reply.getCreatedAt(),
                authorOf(reply.getMemberId(), reply.isAnonymous(), memberId, memberProfiles), likeCount, likedByMe);
    }

    private AuthorResponse authorOf(long authorId, boolean anonymous, Long currentMemberId,
                                    Map<Long, ReviewMemberProfile> memberProfiles) {
        ReviewMemberProfile profile = memberProfiles.get(authorId);
        if (anonymous) {
            return new AuthorResponse(profile.anonymousHandle(), null, true,
                    currentMemberId != null && authorId == currentMemberId);
        }
        String displayName = profile.withdrawn() ? "탈퇴한 사용자" : profile.displayName();
        String profileImageUrl = profile.withdrawn() ? null : profile.profileImageUrl();
        return new AuthorResponse(displayName, profileImageUrl, false,
                currentMemberId != null && authorId == currentMemberId);
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
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
    }

    private Reply getReply(long replyId) {
        return replyRepository.findById(replyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPLY_NOT_FOUND));
    }

    private Long currentMemberIdOrNull() {
        java.util.OptionalLong currentMemberId = currentMemberIdProvider.findCurrentMemberId();
        return currentMemberId.isPresent() ? currentMemberId.getAsLong() : null;
    }

    private Integer nextPage(long totalCount, int page) {
        return totalCount > (long) page * PAGE_SIZE ? page + 1 : null;
    }

    private void validateReviewUpdate(ReviewUpdateRequest request) {
        if (request.isContentPresent() && (request.getContent() == null || request.getContent().isBlank()
                || request.getContent().length() > 1000)) throw new BusinessException(ErrorCode.INVALID_REQUEST);
        if (request.isQuotePresent() && request.getQuote() != null && (request.getQuote().isBlank()
                || request.getQuote().length() > 500)) throw new BusinessException(ErrorCode.INVALID_REQUEST);
        if (request.isChapterPresent() && request.getChapter() != null && (request.getChapter().isBlank()
                || request.getChapter().length() > 255)) throw new BusinessException(ErrorCode.INVALID_REQUEST);
        if (request.isSpoilerPresent() && request.getIsSpoiler() == null) throw new BusinessException(ErrorCode.INVALID_REQUEST);
        if (request.isCurrentPagePresent() && request.getCurrentPage() != null) {
            validateRequestPage(request.getCurrentPage(), request.isTotalPagesPresent() ? request.getTotalPages() : null);
        }
    }

    private void validateReviewCreate(ReviewCreateRequest request) {
        if (request.quote() != null && request.quote().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (request.chapter() != null && request.chapter().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateRequestPage(Integer currentPage, Integer totalPages) {
        if (currentPage != null && currentPage < 0) throw new BusinessException(ErrorCode.INVALID_REQUEST);
        if (totalPages != null && totalPages <= 0) throw new BusinessException(ErrorCode.INVALID_REQUEST);
    }

    private ReviewMemberProfile memberProfileOf(long memberId) {
        return reviewMemberReader.findByMemberIds(List.of(memberId)).get(memberId);
    }

    private Map<Long, ReviewMemberProfile> memberProfilesOf(List<Review> reviews, List<Reply> replies) {
        Set<Long> memberIds = java.util.stream.Stream.concat(
                        reviews.stream().map(Review::getMemberId), replies.stream().map(Reply::getMemberId))
                .collect(Collectors.toSet());
        return memberIds.isEmpty() ? Map.of() : reviewMemberReader.findByMemberIds(memberIds);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> getCommentCounts(Collection<Long> bookIds) {
        if (bookIds.isEmpty()) return Map.of();
        Map<Long, Long> counts = bookIds.stream().collect(Collectors.toMap(bookId -> bookId, bookId -> 0L));
        reviewRepository.countByBookIdInGroupByBookId(bookIds).forEach(count ->
                counts.merge(count.getBookId(), count.getCount(), Long::sum));
        replyRepository.countByReviewBookIdInGroupByBookId(bookIds).forEach(count ->
                counts.merge(count.getBookId(), count.getCount(), Long::sum));
        return counts;
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
