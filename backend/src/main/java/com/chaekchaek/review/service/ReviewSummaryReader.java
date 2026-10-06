package com.chaekchaek.review.service;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.common.auth.CurrentActor;
import com.chaekchaek.common.auth.CurrentActorProvider;
import com.chaekchaek.member.domain.AccountStatus;
import com.chaekchaek.review.domain.Review;
import com.chaekchaek.review.domain.ReviewReaction;
import com.chaekchaek.review.dto.AuthorProfileStatus;
import com.chaekchaek.review.dto.AuthorResponse;
import com.chaekchaek.review.member.ReviewMemberProfile;
import com.chaekchaek.review.member.ReviewMemberReader;
import com.chaekchaek.review.repository.ReplyRepository;
import com.chaekchaek.review.repository.ReviewReactionRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewSummaryReader {

    private final ReplyRepository replyRepository;
    private final ReviewReactionRepository reviewReactionRepository;
    private final BookRepository bookRepository;
    private final CurrentActorProvider currentActorProvider;
    private final ReviewMemberReader reviewMemberReader;

    public List<ReviewSummary> read(List<Review> reviews) {
        if (reviews.isEmpty()) {
            return List.of();
        }
        List<Long> bookIds = reviews.stream()
                .map(Review::getBookId)
                .distinct()
                .toList();
        Map<Long, Book> books = bookRepository.findAllWithAuthorsByIdIn(bookIds)
                .stream()
                .collect(Collectors.toMap(Book::getId, book -> book));

        List<Long> reviewIds = reviews.stream()
                .map(Review::getId)
                .toList();
        Map<Long, Long> replyCounts = replyRepository.countActiveByReviewIdInGroupByReviewId(reviewIds)
                .stream()
                .collect(Collectors.toMap(ReplyRepository.ReviewCount::getReviewId,
                        ReplyRepository.ReviewCount::getCount));
        Map<Long, Long> likeCounts = reviewReactionRepository.countByReviewIdInGroupByReviewId(reviewIds)
                .stream()
                .collect(Collectors.toMap(ReviewReactionRepository.ReactionCount::getReviewId,
                        ReviewReactionRepository.ReactionCount::getCount));

        List<Long> actorIds = reviews.stream()
                .map(Review::getActorId)
                .distinct()
                .toList();
        Map<Long, ReviewMemberProfile> profiles = reviewMemberReader.findByActorIds(actorIds);
        Long currentActorId = currentActorProvider.findCurrentActor()
                .map(CurrentActor::actorId)
                .orElse(null);

        Set<Long> likedReviewIds = likedReviewIds(reviewIds, currentActorId);

        return reviews.stream()
                .map(review -> toSummary(review, books, replyCounts, likeCounts, likedReviewIds, profiles,
                        currentActorId))
                .filter(Objects::nonNull)
                .toList();
    }

    private ReviewSummary toSummary(
            Review review,
            Map<Long, Book> books,
            Map<Long, Long> replyCounts,
            Map<Long, Long> likeCounts,
            Set<Long> likedReviewIds,
            Map<Long, ReviewMemberProfile> profiles, Long currentActorId
    ) {
        Book book = books.get(review.getBookId());
        if (book == null) {
            return null;
        }
        return new ReviewSummary(
                review,
                book,
                authorOf(review, profiles.get(review.getActorId()), currentActorId),
                replyCounts.getOrDefault(review.getId(), 0L),
                likeCounts.getOrDefault(review.getId(), 0L),
                likedReviewIds.contains(review.getId())
        );
    }

    private Set<Long> likedReviewIds(List<Long> reviewIds, Long currentActorId) {
        if (currentActorId == null) {
            return Set.of();
        }
        return reviewReactionRepository.findByReviewIdInAndActorId(reviewIds, currentActorId)
                .stream()
                .map(ReviewReaction::getReviewId)
                .collect(Collectors.toSet());
    }

    private AuthorResponse authorOf(Review review, ReviewMemberProfile profile, Long currentActorId) {
        long authorId = review.getActorId();
        boolean mine = currentActorId != null && authorId == currentActorId;
        if (review.isAnonymous()) {
            return new AuthorResponse(
                    null,
                    profile.anonymousNickname(),
                    null,
                    true,
                    mine,
                    profile.actorType(),
                    AuthorProfileStatus.UNAVAILABLE
            );
        }
        String displayName = profile.displayName();
        String profileImageUrl = profile.profileImageUrl();
        if (profile.accountStatus() == AccountStatus.WITHDRAWN) {
            displayName = profile.anonymousNickname();
            profileImageUrl = null;
        }

        Long memberId = null;
        AuthorProfileStatus profileStatus = AuthorProfileStatus.UNAVAILABLE;
        if (profile.accountStatus() == AccountStatus.ACTIVE) {
            memberId = profile.memberId();
            profileStatus = AuthorProfileStatus.AVAILABLE;
        }

        return new AuthorResponse(
                memberId,
                displayName,
                profileImageUrl,
                false,
                mine,
                profile.actorType(),
                profileStatus
        );
    }

    public record ReviewSummary(Review review, Book book, AuthorResponse author, long replyCount, long likeCount,
                                boolean likedByMe) {
    }
}
