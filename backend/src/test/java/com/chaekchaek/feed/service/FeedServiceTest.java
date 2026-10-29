package com.chaekchaek.feed.service;

import static com.chaekchaek.review.fixture.ReviewFixture.savedReview;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.fixture.BookFixture;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.common.auth.ActorType;
import com.chaekchaek.common.auth.CurrentActorProvider;
import com.chaekchaek.member.domain.AccountStatus;
import com.chaekchaek.feed.dto.FeedReviewListResponse;
import com.chaekchaek.feed.dto.FeedReviewResponse;
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

    private static final Instant CREATED_AT = Instant.parse("2026-09-28T10:00:00Z");

    private final ReviewRepository reviewRepository = mock(ReviewRepository.class);
    private final ReviewSummaryReader summaryReader = mock(ReviewSummaryReader.class);
    private final FeedService service = new FeedService(reviewRepository, summaryReader);

    @Test
    @DisplayName("전체 피드의 탈퇴 회원 공개 감상은 기존 익명 닉네임으로 표시한다")
    void should_DisplayAnonymousNickname_When_PublicReviewAuthorIsWithdrawn() {
        // given
        BookRepository books = mock(BookRepository.class);
        CurrentActorProvider currentActor = mock(CurrentActorProvider.class);
        ReviewMemberReader members = mock(ReviewMemberReader.class);
        Review review = review(10L, 7L, "탈퇴 회원 감상", false);
        Book book = book();
        when(reviewRepository.findFeedReviews(any(Pageable.class)))
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
        AuthorResponse author = new FeedService(reviewRepository, reader).getReviews(1).reviews().getFirst().author();

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
        Review review = review(123L, 1L, "스포일러 감상", true);
        Book book = book();
        AuthorResponse author = new AuthorResponse(
                1L,
                "독자",
                null,
                false,
                true,
                ActorType.MEMBER,
                AuthorProfileStatus.AVAILABLE
        );
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
        FeedReviewListResponse expected = new FeedReviewListResponse(
                21,
                2,
                List.of(new FeedReviewResponse(
                        123L,
                        "스포일러 감상",
                        null,
                        null,
                        true,
                        CREATED_AT,
                        author,
                        0,
                        false,
                        3,
                        42L,
                        "9788936433598",
                        "도서 제목",
                        List.of("작가"),
                        "https://example.com/cover.jpg"
                ))
        );
        assertThat(result).isEqualTo(expected);
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

    private Book book() {
        Book book = BookFixture.book(
                new Isbn13("9788936433598"),
                "도서 제목",
                "https://example.com/cover.jpg",
                List.of("작가")
        );
        return BookFixture.savedBook(42L, book);
    }

    private Review review(long id, long actorId, String content, boolean spoiler) {
        Review review = Review.create(42L, actorId, content, null, null, null, spoiler, false);
        return savedReview(id, review, CREATED_AT);
    }
}
