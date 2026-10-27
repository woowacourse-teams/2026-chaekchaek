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
import com.chaekchaek.review.repository.ReviewReactionRepository;
import com.chaekchaek.review.repository.ReviewRepository;
import com.chaekchaek.review.service.ReviewSummaryReader;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class HomeServiceTest {

    private final ReviewRepository reviewRepository = mock(ReviewRepository.class);
    private final ReplyRepository replyRepository = mock(ReplyRepository.class);
    private final BookRepository bookRepository = mock(BookRepository.class);
    private final CurrentActorProvider currentActorProvider = mock(CurrentActorProvider.class);
    private final ReviewMemberReader reviewMemberReader = mock(ReviewMemberReader.class);
    private final ReviewSummaryReader summaryReader = new ReviewSummaryReader(
            replyRepository,
            mock(ReviewReactionRepository.class),
            bookRepository,
            currentActorProvider,
            reviewMemberReader
    );
    private final HomeService homeService = new HomeService(reviewRepository, bookRepository, summaryReader);

    @Test
    @DisplayName("집계된 인기 순서에 맞춰 책 정보를 반환한다")
    void should_ReturnBooksInPopularOrder_When_FindingPopularBooks() {
        // given
        List<ReviewRepository.PopularBookCount> popularBookCounts = List.of(
                popularBookCount(3L, 2L, 8L),
                popularBookCount(2L, 5L, 5L),
                popularBookCount(1L, 8L, 1L)
        );
        List<Book> books = List.of(
                book(1L, "첫 번째 책"),
                book(2L, "두 번째 책"),
                book(3L, "세 번째 책")
        );
        when(reviewRepository.findTop10PopularBookCounts()).thenReturn(popularBookCounts);
        when(bookRepository.findAllWithAuthorsByIdIn(List.of(3L, 2L, 1L))).thenReturn(books);

        // when
        List<PopularBookResponse> result = homeService.getPopularBooks().books();

        // then
        List<PopularBookResponse> expected = List.of(
                new PopularBookResponse(
                        3L,
                        "9780000000026",
                        "세 번째 책",
                        "https://example.com/3.jpg",
                        List.of("저자 3"),
                        2L,
                        8L
                ),
                new PopularBookResponse(
                        2L,
                        "9780000000019",
                        "두 번째 책",
                        "https://example.com/2.jpg",
                        List.of("저자 2"),
                        5L,
                        5L
                ),
                new PopularBookResponse(
                        1L,
                        "9780000000002",
                        "첫 번째 책",
                        "https://example.com/1.jpg",
                        List.of("저자 1"),
                        8L,
                        1L
                )
        );
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("최신 감상에 책 정보와 유효 답글 수를 결합한다")
    void should_ReturnLatestReviewsWithBookAndReplyCount_When_FindingLatestReviews() {
        // given
        when(reviewMemberReader.findByActorIds(anyCollection())).thenReturn(Map.of(
                1L,
                new ReviewMemberProfile(
                        101L,
                        "책 읽는 사람",
                        "https://example.com/profile-1.jpg",
                        "익명 사용자 1",
                        false,
                        AccountStatus.ACTIVE,
                        ActorType.MEMBER
                ),
                2L,
                new ReviewMemberProfile(
                        102L,
                        "닉네임",
                        "https://example.com/profile.jpg",
                        "다정한 참새",
                        true,
                        AccountStatus.ACTIVE,
                        ActorType.MEMBER
                )
        ));
        Review latestReview = review(101L, 2L, 2L, "최신 감상", Instant.parse("2026-08-18T14:00:00Z"));
        Review previousReview = review(100L, 1L, 1L, "이전 감상", Instant.parse("2026-08-18T13:00:00Z"));
        List<ReplyRepository.ReviewCount> replyCounts = List.of(
                replyCount(101L, 12L),
                replyCount(100L, 3L)
        );
        List<Book> books = List.of(book(1L, "첫 번째 책"), book(2L, "두 번째 책"));
        when(reviewRepository.findTop10ByDeletedAtIsNullAndSpoilerFalseOrderByCreatedAtDescIdDesc())
                .thenReturn(List.of(latestReview, previousReview));
        when(replyRepository.countActiveByReviewIdInGroupByReviewId(List.of(101L, 100L))).thenReturn(replyCounts);
        when(bookRepository.findAllWithAuthorsByIdIn(List.of(2L, 1L))).thenReturn(books);

        // when
        List<LatestReviewResponse> result = homeService.getLatestReviews().reviews();

        // then
        List<LatestReviewResponse> expected = List.of(
                new LatestReviewResponse(
                        "최신 감상",
                        Instant.parse("2026-08-18T14:00:00Z"),
                        new AuthorResponse(
                                null,
                                "다정한 참새",
                                null,
                                true,
                                false,
                                ActorType.MEMBER,
                                AuthorProfileStatus.UNAVAILABLE
                        ),
                        12L,
                        2L,
                        "9780000000019",
                        "두 번째 책",
                        "https://example.com/2.jpg"
                ),
                new LatestReviewResponse(
                        "이전 감상",
                        Instant.parse("2026-08-18T13:00:00Z"),
                        new AuthorResponse(
                                101L,
                                "책 읽는 사람",
                                "https://example.com/profile-1.jpg",
                                false,
                                false,
                                ActorType.MEMBER,
                                AuthorProfileStatus.AVAILABLE
                        ),
                        3L,
                        1L,
                        "9780000000002",
                        "첫 번째 책",
                        "https://example.com/1.jpg"
                )
        );
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("탈퇴 회원의 감상은 공개 프로필에 접근할 수 없다")
    void should_MarkProfileUnavailable_When_ReviewAuthorIsWithdrawn() {
        // given
        Review review = review(100L, 1L, 1L, "탈퇴 회원 감상", Instant.parse("2026-08-18T13:00:00Z"));
        Book book = book(1L, "첫 번째 책");

        when(currentActorProvider.findCurrentActor()).thenReturn(Optional.empty());
        when(reviewRepository.findTop10ByDeletedAtIsNullAndSpoilerFalseOrderByCreatedAtDescIdDesc())
                .thenReturn(List.of(review));
        when(replyRepository.countActiveByReviewIdInGroupByReviewId(List.of(100L))).thenReturn(List.of());
        when(bookRepository.findAllWithAuthorsByIdIn(List.of(1L))).thenReturn(List.of(book));
        when(reviewMemberReader.findByActorIds(List.of(1L))).thenReturn(Map.of(
                1L,
                new ReviewMemberProfile(
                        101L,
                        null,
                        null,
                        "익명 사용자 1",
                        true,
                        AccountStatus.WITHDRAWN,
                        ActorType.MEMBER
                )
        ));

        // when
        AuthorResponse author = homeService.getLatestReviews().reviews().getFirst().author();

        // then
        assertThat(author.displayName()).isEqualTo("익명 사용자 1");
        assertThat(author.memberId()).isNull();
        assertThat(author.profileStatus()).isEqualTo(AuthorProfileStatus.UNAVAILABLE);
    }

    private static ReviewRepository.PopularBookCount popularBookCount(long bookId, long reviewCount, long replyCount) {
        ReviewRepository.PopularBookCount countProjection = mock(ReviewRepository.PopularBookCount.class);
        when(countProjection.getBookId()).thenReturn(bookId);
        when(countProjection.getReviewCount()).thenReturn(reviewCount);
        when(countProjection.getReplyCount()).thenReturn(replyCount);
        return countProjection;
    }

    private static Book book(long id, String title) {
        Book book = Book.create(
                isbn13(id),
                title,
                "https://example.com/" + id + ".jpg",
                null,
                null,
                null,
                List.of("저자 " + id),
                List.of(),
                "출판사",
                null,
                null,
                null,
                null,
                null
        );
        ReflectionTestUtils.setField(book, "id", id);
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

    private static Review review(
            long id,
            long bookId,
            long actorId,
            String content,
            Instant createdAt
    ) {
        Review review = Review.create(bookId, actorId, content, null, null, null, false, actorId == 2L);
        ReflectionTestUtils.setField(review, "id", id);
        ReflectionTestUtils.setField(review, "createdAt", createdAt);
        return review;
    }
}
