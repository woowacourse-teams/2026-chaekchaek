package com.chaekchaek.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.common.auth.ActorType;
import com.chaekchaek.common.auth.CurrentActorProvider;
import com.chaekchaek.member.domain.AccountStatus;
import com.chaekchaek.feed.dto.FeedReviewListResponse;
import com.chaekchaek.member.domain.AccountStatus;
import com.chaekchaek.review.domain.Review;
import com.chaekchaek.review.dto.AuthorProfileStatus;
import com.chaekchaek.review.dto.AuthorResponse;
import com.chaekchaek.review.member.ReviewMemberProfile;
import com.chaekchaek.review.member.ReviewMemberReader;
import com.chaekchaek.review.repository.ReplyRepository;
import com.chaekchaek.review.repository.ReviewReactionRepository;
import com.chaekchaek.review.repository.ReviewRepository;
import com.chaekchaek.review.service.ReviewSummaryReader;
import com.chaekchaek.review.service.ReviewSummaryReader.ReviewSummary;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class FeedServiceTest {

    @Test
    @DisplayName("전체 피드의 탈퇴 회원 공개 감상은 기존 익명 닉네임으로 표시한다")
    void should_DisplayAnonymousNickname_When_PublicReviewAuthorIsWithdrawn() {
        // given
        ReviewRepository reviews = mock(ReviewRepository.class);
        BookRepository books = mock(BookRepository.class);
        CurrentActorProvider currentActor = mock(CurrentActorProvider.class);
        ReviewMemberReader members = mock(ReviewMemberReader.class);
        Review review = mock(Review.class);
        Book book = mock(Book.class);
        when(review.getId()).thenReturn(10L);
        when(review.getBookId()).thenReturn(42L);
        when(review.getActorId()).thenReturn(7L);
        when(book.getId()).thenReturn(42L);
        when(book.getIsbn13()).thenReturn(new Isbn13("9788936433598"));
        when(reviews.findFeedReviews(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review)));
        when(books.findAllWithAuthorsByIdIn(List.of(42L))).thenReturn(List.of(book));
        when(currentActor.findCurrentActor()).thenReturn(Optional.empty());
        when(members.findByActorIds(List.of(7L))).thenReturn(Map.of(
                7L,
                new ReviewMemberProfile(
                        1L,
                        null,
                        null,
                        "탈퇴 전 익명 이름",
                        true,
                        AccountStatus.WITHDRAWN,
                        ActorType.MEMBER
                )
        ));
        ReviewSummaryReader reader = new ReviewSummaryReader(
                mock(ReplyRepository.class),
                mock(ReviewReactionRepository.class),
                books,
                currentActor,
                members
        );

        // when
        AuthorResponse author = new FeedService(reviews, reader).getReviews(1).reviews().getFirst().author();

        // then
        assertThat(author.displayName()).isEqualTo("탈퇴 전 익명 이름");
        assertThat(author.memberId()).isNull();
        assertThat(author.profileImageUrl()).isNull();
        assertThat(author.profileStatus()).isEqualTo(AuthorProfileStatus.UNAVAILABLE);
        assertThat(author.mine()).isFalse();
    }

    @Test
    @DisplayName("다음 페이지가 있으면 스포일러와 작성자·책·답글 정보를 함께 반환한다")
    void should_ReturnReviewDetailsAndNextPage_When_MoreReviewsExist() {
        // given
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        ReviewSummaryReader summaryReader = mock(ReviewSummaryReader.class);
        FeedService service = new FeedService(reviewRepository, summaryReader);
        Review review = mock(Review.class);
        Book book = mock(Book.class);
        AuthorResponse author = new AuthorResponse(
                1L,
                "독자",
                null,
                false,
                true,
                ActorType.MEMBER,
                AuthorProfileStatus.AVAILABLE
        );
        when(review.getId()).thenReturn(123L);
        when(review.getContent()).thenReturn("스포일러 감상");
        when(review.isSpoiler()).thenReturn(true);
        when(review.getCreatedAt()).thenReturn(Instant.parse("2026-09-28T10:00:00Z"));
        when(book.getId()).thenReturn(42L);
        when(book.getIsbn13()).thenReturn(new Isbn13("9788936433598"));
        when(book.getTitle()).thenReturn("도서 제목");
        when(book.getCoverImageUrl()).thenReturn("https://example.com/cover.jpg");
        when(reviewRepository.findFeedReviews(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review), PageRequest.of(0, 20), 21));
        when(summaryReader.read(List.of(review)))
                .thenReturn(List.of(new ReviewSummary(
                        review,
                        book,
                        author,
                        3,
                        0,
                        false
                )));

        // when
        FeedReviewListResponse result = service.getReviews(1);

        // then
        assertThat(result.totalCount()).isEqualTo(21);
        assertThat(result.nextPage()).isEqualTo(2);
        assertThat(result.reviews()).singleElement().satisfies(item -> {
            assertThat(item.reviewId()).isEqualTo(123L);
            assertThat(item.content()).isEqualTo("스포일러 감상");
            assertThat(item.isSpoiler()).isTrue();
            assertThat(item.createdAt()).isEqualTo(Instant.parse("2026-09-28T10:00:00Z"));
            assertThat(item.author()).isEqualTo(author);
            assertThat(item.replyCount()).isEqualTo(3);
            assertThat(item.bookId()).isEqualTo(42L);
            assertThat(item.isbn13()).isEqualTo("9788936433598");
            assertThat(item.bookTitle()).isEqualTo("도서 제목");
            assertThat(item.bookCoverImageUrl()).isEqualTo("https://example.com/cover.jpg");
        });
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(reviewRepository).findFeedReviews(pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageable.getValue().getSort()).containsExactly(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
        );
    }

    @Test
    @DisplayName("범위를 벗어난 페이지는 전체 개수와 빈 목록을 반환한다")
    void should_ReturnEmptyReviewsAndNoNextPage_When_PageIsOutOfRange() {
        // given
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        ReviewSummaryReader summaryReader = mock(ReviewSummaryReader.class);
        FeedService service = new FeedService(reviewRepository, summaryReader);
        when(reviewRepository.findFeedReviews(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 20), 21));
        when(summaryReader.read(List.of())).thenReturn(List.of());

        // when
        FeedReviewListResponse result = service.getReviews(3);

        // then
        assertThat(result.totalCount()).isEqualTo(21);
        assertThat(result.nextPage()).isNull();
        assertThat(result.reviews()).isEmpty();
    }
}
