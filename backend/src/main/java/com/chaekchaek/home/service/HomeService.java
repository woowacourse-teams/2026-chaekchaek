package com.chaekchaek.home.service;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.common.auth.CurrentActor;
import com.chaekchaek.common.auth.CurrentActorProvider;
import com.chaekchaek.home.dto.LatestReviewListResponse;
import com.chaekchaek.home.dto.LatestReviewResponse;
import com.chaekchaek.home.dto.PopularBookListResponse;
import com.chaekchaek.home.dto.PopularBookResponse;
import com.chaekchaek.review.domain.Review;
import com.chaekchaek.review.dto.AuthorResponse;
import com.chaekchaek.review.dto.AuthorProfileStatus;
import com.chaekchaek.review.member.ReviewMemberProfile;
import com.chaekchaek.review.member.ReviewMemberReader;
import com.chaekchaek.review.repository.ReviewRepository;
import com.chaekchaek.review.repository.ReplyRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final ReviewRepository reviewRepository;
    private final ReplyRepository replyRepository;
    private final BookRepository bookRepository;
    private final CurrentActorProvider currentActorProvider;
    private final ReviewMemberReader reviewMemberReader;

    @Transactional(readOnly = true)
    public PopularBookListResponse getPopularBooks() {
        List<ReviewRepository.PopularBookCount> popularBookCounts = reviewRepository.findTop10PopularBookCounts();
        List<Long> bookIds = popularBookCounts.stream().map(ReviewRepository.PopularBookCount::getBookId).toList();
        Map<Long, Book> books = booksWithAuthorsById(bookIds);
        List<PopularBookResponse> responses = popularBookCounts.stream()
                .map(count -> toPopularBookResponse(books.get(count.getBookId()), count))
                .filter(Objects::nonNull)
                .toList();
        return new PopularBookListResponse(responses);
    }

    @Transactional(readOnly = true)
    public LatestReviewListResponse getLatestReviews() {
        List<Review> reviews = reviewRepository.findTop10ByDeletedAtIsNullAndSpoilerFalseOrderByCreatedAtDescIdDesc();
        Map<Long, Long> replyCounts = replyCountsByReviewId(reviews);
        Map<Long, Book> books = booksById(reviews.stream().map(Review::getBookId).distinct().toList());
        Long currentActorId = currentActorIdOrNull();
        Map<Long, ReviewMemberProfile> memberProfiles = memberProfilesByReview(reviews);
        List<LatestReviewResponse> responses = reviews.stream()
                .map(review -> toLatestReviewResponse(review, replyCounts, books, currentActorId, memberProfiles))
                .filter(Objects::nonNull)
                .toList();
        return new LatestReviewListResponse(responses);
    }

    private Map<Long, Book> booksWithAuthorsById(List<Long> bookIds) {
        if (bookIds.isEmpty()) {
            return Map.of();
        }
        return bookRepository.findAllWithAuthorsByIdIn(bookIds).stream()
                .collect(java.util.stream.Collectors.toMap(Book::getId, book -> book));
    }

    private Map<Long, Book> booksById(List<Long> bookIds) {
        if (bookIds.isEmpty()) {
            return Map.of();
        }
        return bookRepository.findAllById(bookIds).stream()
                .collect(java.util.stream.Collectors.toMap(Book::getId, book -> book));
    }

    private PopularBookResponse toPopularBookResponse(Book book, ReviewRepository.PopularBookCount count) {
        if (book == null) {
            return null;
        }
        long bookId = book.getId();
        return new PopularBookResponse(bookId, book.getIsbn13(), book.getTitle(), book.getCoverImageUrl(), book.getAuthors(),
                count.getReviewCount(), count.getReplyCount());
    }

    private Map<Long, Long> replyCountsByReviewId(List<Review> reviews) {
        List<Long> reviewIds = reviews.stream().map(Review::getId).toList();
        if (reviewIds.isEmpty()) {
            return Map.of();
        }
        return replyRepository.countActiveByReviewIdInGroupByReviewId(reviewIds).stream()
                .collect(java.util.stream.Collectors.toMap(ReplyRepository.ReviewCount::getReviewId,
                        ReplyRepository.ReviewCount::getCount));
    }

    private LatestReviewResponse toLatestReviewResponse(Review review, Map<Long, Long> replyCounts,
                                                        Map<Long, Book> books, Long currentMemberId,
                                                        Map<Long, ReviewMemberProfile> memberProfiles) {
        Book book = books.get(review.getBookId());
        if (book == null) {
            return null;
        }
        return new LatestReviewResponse(review.getContent(), review.getCreatedAt(),
                authorOf(review, currentMemberId, memberProfiles), replyCounts.getOrDefault(review.getId(), 0L),
                book.getId(), book.getIsbn13(), book.getTitle(), book.getCoverImageUrl());
    }

    private AuthorResponse authorOf(Review review, Long currentMemberId,
                                    Map<Long, ReviewMemberProfile> memberProfiles) {
        long authorId = review.getActorId();
        ReviewMemberProfile profile = memberProfiles.get(authorId);
        boolean mine = currentMemberId != null && authorId == currentMemberId;
        if (review.isAnonymous()) {
            return new AuthorResponse(null, profile.anonymousNickname(), null, true, mine, profile.actorType(),
                    AuthorProfileStatus.UNAVAILABLE);
        }
        boolean withdrawn = profile.accountStatus() == com.chaekchaek.member.domain.AccountStatus.WITHDRAWN;
        boolean available = profile.accountStatus() == com.chaekchaek.member.domain.AccountStatus.ACTIVE;
        String displayName = withdrawn ? "탈퇴한 사용자" : profile.displayName();
        String profileImageUrl = withdrawn ? null : profile.profileImageUrl();
        AuthorProfileStatus profileStatus = withdrawn ? AuthorProfileStatus.WITHDRAWN
                : available ? AuthorProfileStatus.AVAILABLE : AuthorProfileStatus.UNAVAILABLE;
        return new AuthorResponse(available ? profile.memberId() : null, displayName, profileImageUrl, false, mine,
                profile.actorType(), profileStatus);
    }

    private Long currentActorIdOrNull() {
        return currentActorProvider.findCurrentActor().map(CurrentActor::actorId).orElse(null);
    }

    private Map<Long, ReviewMemberProfile> memberProfilesByReview(List<Review> reviews) {
        List<Long> actorIds = reviews.stream().map(Review::getActorId).distinct().toList();
        if (actorIds.isEmpty()) {
            return Map.of();
        }
        return reviewMemberReader.findByActorIds(actorIds);
    }
}
