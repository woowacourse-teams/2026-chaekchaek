package com.chaekchaek.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.common.auth.ActorType;
import com.chaekchaek.common.auth.CurrentActorProvider;
import com.chaekchaek.home.dto.PopularBookResponse;
import com.chaekchaek.home.dto.LatestReviewResponse;
import com.chaekchaek.review.dto.AuthorResponse;
import com.chaekchaek.review.dto.AuthorProfileStatus;
import com.chaekchaek.member.domain.AccountStatus;
import com.chaekchaek.review.domain.Review;
import com.chaekchaek.review.member.ReviewMemberProfile;
import com.chaekchaek.review.member.ReviewMemberReader;
import com.chaekchaek.review.repository.ReplyRepository;
import com.chaekchaek.review.repository.ReviewRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HomeServiceTest {

    @Test
    @DisplayName("집계된 인기 순서에 맞춰 책 정보를 반환한다")
    void should_ReturnBooksInPopularOrder_When_FindingPopularBooks() {
        // given
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        ReplyRepository replyRepository = mock(ReplyRepository.class);
        BookRepository bookRepository = mock(BookRepository.class);
        HomeService homeService = homeService(reviewRepository, replyRepository, bookRepository);
        List<ReviewRepository.PopularBookCount> popularBookCounts = List.of(
                popularBookCount(3L, 2L, 8L), popularBookCount(2L, 5L, 5L), popularBookCount(1L, 8L, 1L)
        );
        List<Book> books = List.of(
                book(1L, "첫 번째 책"), book(2L, "두 번째 책"), book(3L, "세 번째 책")
        );
        when(reviewRepository.findTop10PopularBookCounts()).thenReturn(popularBookCounts);
        when(bookRepository.findAllWithAuthorsByIdIn(List.of(3L, 2L, 1L))).thenReturn(books);

        // when
        List<PopularBookResponse> result = homeService.getPopularBooks().books();

        // then
        assertThat(result).extracting(PopularBookResponse::bookId).containsExactly(3L, 2L, 1L);
        assertThat(result).extracting(PopularBookResponse::isbn13)
                .containsExactly("9780000000026", "9780000000019", "9780000000002");
        assertThat(result).extracting(PopularBookResponse::reviewCount).containsExactly(2L, 5L, 8L);
        assertThat(result).extracting(PopularBookResponse::replyCount).containsExactly(8L, 5L, 1L);
    }

    @Test
    @DisplayName("최신 감상에 책 정보와 유효 답글 수를 결합한다")
    void should_ReturnLatestReviewsWithBookAndReplyCount_When_FindingLatestReviews() {
        // given
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        ReplyRepository replyRepository = mock(ReplyRepository.class);
        BookRepository bookRepository = mock(BookRepository.class);
        HomeService homeService = homeService(reviewRepository, replyRepository, bookRepository);
        Review latestReview = review(101L, 2L, 2L, "최신 감상", Instant.parse("2026-08-18T14:00:00Z"));
        Review previousReview = review(100L, 1L, 1L, "이전 감상", Instant.parse("2026-08-18T13:00:00Z"));
        List<ReplyRepository.ReviewCount> replyCounts = List.of(
                replyCount(101L, 12L), replyCount(100L, 3L)
        );
        List<Book> books = List.of(book(1L, "첫 번째 책"), book(2L, "두 번째 책"));
        when(reviewRepository.findTop10ByDeletedAtIsNullAndSpoilerFalseOrderByCreatedAtDescIdDesc())
                .thenReturn(List.of(latestReview, previousReview));
        when(replyRepository.countActiveByReviewIdInGroupByReviewId(List.of(101L, 100L))).thenReturn(replyCounts);
        when(bookRepository.findAllById(List.of(2L, 1L))).thenReturn(books);

        // when
        List<LatestReviewResponse> result = homeService.getLatestReviews().reviews();

        // then
        assertThat(result).extracting(LatestReviewResponse::content).containsExactly("최신 감상", "이전 감상");
        assertThat(result).extracting(LatestReviewResponse::createdAt)
                .containsExactly(Instant.parse("2026-08-18T14:00:00Z"), Instant.parse("2026-08-18T13:00:00Z"));
        assertThat(result).extracting(LatestReviewResponse::replyCount).containsExactly(12L, 3L);
        assertThat(result).extracting(LatestReviewResponse::bookTitle).containsExactly("두 번째 책", "첫 번째 책");
        assertThat(result).extracting(LatestReviewResponse::author)
                .containsExactly(new AuthorResponse(null, "다정한 참새", null, true, false, ActorType.MEMBER,
                                AuthorProfileStatus.UNAVAILABLE),
                        new AuthorResponse(101L, "책 읽는 사람", "https://example.com/profile-1.jpg", false,
                                false, ActorType.MEMBER, AuthorProfileStatus.AVAILABLE));
    }

    @Test
    @DisplayName("탈퇴 회원의 감상은 공개 프로필에 접근할 수 없다")
    void should_MarkProfileUnavailable_When_ReviewAuthorIsWithdrawn() {
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        ReplyRepository replyRepository = mock(ReplyRepository.class);
        BookRepository bookRepository = mock(BookRepository.class);
        CurrentActorProvider currentActorProvider = mock(CurrentActorProvider.class);
        ReviewMemberReader reviewMemberReader = mock(ReviewMemberReader.class);
        Review review = review(100L, 1L, 1L, "탈퇴 회원 감상", Instant.parse("2026-08-18T13:00:00Z"));
        Book book = book(1L, "첫 번째 책");

        when(currentActorProvider.findCurrentActor()).thenReturn(Optional.empty());
        when(reviewRepository.findTop10ByDeletedAtIsNullAndSpoilerFalseOrderByCreatedAtDescIdDesc())
                .thenReturn(List.of(review));
        when(replyRepository.countActiveByReviewIdInGroupByReviewId(List.of(100L))).thenReturn(List.of());
        when(bookRepository.findAllById(List.of(1L))).thenReturn(List.of(book));
        when(reviewMemberReader.findByActorIds(List.of(1L))).thenReturn(java.util.Map.of(
                1L, new ReviewMemberProfile(101L, null, null, "익명 사용자 1",
                        true, AccountStatus.WITHDRAWN, ActorType.MEMBER)
        ));
        HomeService homeService = new HomeService(reviewRepository, replyRepository, bookRepository,
                currentActorProvider, reviewMemberReader);

        AuthorResponse author = homeService.getLatestReviews().reviews().getFirst().author();

        assertThat(author.displayName()).isEqualTo("탈퇴한 사용자");
        assertThat(author.memberId()).isNull();
        assertThat(author.profileStatus()).isEqualTo(AuthorProfileStatus.UNAVAILABLE);
    }

    private static HomeService homeService(ReviewRepository reviewRepository, ReplyRepository replyRepository,
                                           BookRepository bookRepository) {
        CurrentActorProvider currentActorProvider = mock(CurrentActorProvider.class);
        ReviewMemberReader reviewMemberReader = mock(ReviewMemberReader.class);
        when(currentActorProvider.findCurrentActor()).thenReturn(Optional.empty());
        when(reviewMemberReader.findByActorIds(anyCollection())).thenReturn(java.util.Map.of(
                1L, new ReviewMemberProfile(101L, "책 읽는 사람", "https://example.com/profile-1.jpg",
                        "익명 사용자 1", false, AccountStatus.ACTIVE, ActorType.MEMBER),
                2L, new ReviewMemberProfile(102L, "닉네임", "https://example.com/profile.jpg",
                        "다정한 참새", true, AccountStatus.ACTIVE, ActorType.MEMBER)
        ));
        return new HomeService(reviewRepository, replyRepository, bookRepository,
                currentActorProvider, reviewMemberReader);
    }

    private static ReviewRepository.PopularBookCount popularBookCount(long bookId, long reviewCount, long replyCount) {
        ReviewRepository.PopularBookCount countProjection = mock(ReviewRepository.PopularBookCount.class);
        when(countProjection.getBookId()).thenReturn(bookId);
        when(countProjection.getReviewCount()).thenReturn(reviewCount);
        when(countProjection.getReplyCount()).thenReturn(replyCount);
        return countProjection;
    }

    private static Book book(long id, String title) {
        Book book = mock(Book.class);
        when(book.getId()).thenReturn(id);
        when(book.getIsbn13()).thenReturn(isbn13(id));
        when(book.getTitle()).thenReturn(title);
        when(book.getCoverImageUrl()).thenReturn("https://example.com/" + id + ".jpg");
        when(book.getAuthors()).thenReturn(List.of("저자 " + id));
        return book;
    }

    private static Isbn13 isbn13(long id) {
        return switch ((int) id) {
            case 1 -> new Isbn13("9780000000002");
            case 2 -> new Isbn13("9780000000019");
            case 3 -> new Isbn13("9780000000026");
            default -> new Isbn13("9788925568683");
        };
    }

    private static ReplyRepository.ReviewCount replyCount(long reviewId, long count) {
        ReplyRepository.ReviewCount countProjection = mock(ReplyRepository.ReviewCount.class);
        when(countProjection.getReviewId()).thenReturn(reviewId);
        when(countProjection.getCount()).thenReturn(count);
        return countProjection;
    }

    private static Review review(long id, long bookId, long actorId, String content, Instant createdAt) {
        Review review = mock(Review.class);
        when(review.getId()).thenReturn(id);
        when(review.getBookId()).thenReturn(bookId);
        when(review.getActorId()).thenReturn(actorId);
        when(review.isAnonymous()).thenReturn(actorId == 2L);
        when(review.getContent()).thenReturn(content);
        when(review.getCreatedAt()).thenReturn(createdAt);
        return review;
    }
}
