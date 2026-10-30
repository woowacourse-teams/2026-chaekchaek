package com.chaekchaek.review.service;

import static com.chaekchaek.review.fixture.ReviewFixture.savedReview;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.fixture.BookFixture;
import com.chaekchaek.book.service.BookResolver;
import com.chaekchaek.common.auth.ActorType;
import com.chaekchaek.common.auth.CurrentActor;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.library.service.BookActivityCountReader.ActivityCounts;
import com.chaekchaek.review.book.ReviewBookReader;
import com.chaekchaek.review.domain.Reply;
import com.chaekchaek.review.domain.ReplyReaction;
import com.chaekchaek.review.domain.Review;
import com.chaekchaek.review.domain.ReviewReaction;
import com.chaekchaek.review.dto.AuthorProfileStatus;
import com.chaekchaek.review.dto.AuthorResponse;
import com.chaekchaek.review.dto.ReviewCreateByIsbnResponse;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class ReviewServiceTest {

    private final ReviewRepository reviewRepository = mock(ReviewRepository.class);
    private final ReplyRepository replyRepository = mock(ReplyRepository.class);
    private final ReviewReactionRepository reviewReactionRepository = mock(ReviewReactionRepository.class);
    private final ReplyReactionRepository replyReactionRepository = mock(ReplyReactionRepository.class);
    private final ReviewBookReader bookReader = mock(ReviewBookReader.class);
    private final ReadingRecordCoordinator readingRecordCoordinator = mock(ReadingRecordCoordinator.class);
    private final ReviewMemberReader memberReader = mock(ReviewMemberReader.class);
    private final BookResolver bookResolver = mock(BookResolver.class);

    @Test
    @DisplayName("게스트는 서재를 변경하지 않고 감상을 작성한다")
    void should_CreateReviewWithoutChangingLibrary_When_ActorIsGuest() {
        // given
        when(memberReader.findByActorIds(List.of(7L))).thenReturn(Map.of(
                7L,
                new ReviewMemberProfile(
                        "게스트",
                        null,
                        "다정한 참새",
                        true,
                        false,
                        ActorType.GUEST
                )
        ));
        when(reviewRepository.save(any(Review.class)))
                .thenAnswer(invocation -> savedReview(10L, invocation.getArgument(0)));
        ReviewService service = reviewService(CurrentActor.guest(7L), memberReader);

        // when
        ReviewResponse response = service.createReview(
                5L,
                new ReviewCreateRequest(
                        "게스트 감상",
                        null,
                        null,
                        null,
                        null,
                        false
                )
        );

        // then
        AuthorResponse expectedAuthor = new AuthorResponse(
                null,
                "다정한 참새",
                null,
                true,
                true,
                ActorType.GUEST,
                AuthorProfileStatus.UNAVAILABLE
        );
        assertThat(response.author()).isEqualTo(expectedAuthor);
        verify(readingRecordCoordinator, never()).recordReview(
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
        );
    }

    @Test
    @DisplayName("게스트는 감상에서 개인 독서 쪽수를 입력할 수 없다")
    void should_RejectReadingPage_When_GuestCreatesReview() {
        // given
        ReviewService service = reviewService(CurrentActor.guest(7L), memberReader(true));

        // when & then
        assertThatThrownBy(() -> service.createReview(
                5L,
                new ReviewCreateRequest(
                        "게스트 감상",
                        null,
                        null,
                        10,
                        100,
                        false
                )
        ))
                .isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN)
        );
    }

    @Test
    @DisplayName("게스트는 기존 감상의 개인 독서 쪽수를 수정할 수 없다")
    void should_RejectReadingPage_When_GuestUpdatesReview() {
        // given
        Review review = savedReview(10L, Review.create(5L, 7L, "게스트 감상", null, null, null, false, true));
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));
        ReviewService service = reviewService(CurrentActor.guest(7L), memberReader(true));
        ReviewUpdateRequest request = new ReviewUpdateRequest();
        request.setCurrentPage(10);
        request.setTotalPages(100);

        // when & then
        assertThatThrownBy(() -> service.updateReview(10L, request))
                .isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN)
        );
    }

    @Test
    @DisplayName("감상과 답글 좋아요는 현재 Actor에 귀속된다")
    void should_AssignReactions_When_CurrentActorLikesReviewOrReply() {
        // given
        Review review = savedReview(10L, Review.create(5L, 1L, "감상", null, null, null, false, false));
        Reply reply = Reply.create(10L, 1L, "답글", false);
        ReflectionTestUtils.setField(reply, "id", 20L);
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));
        when(replyRepository.findById(20L)).thenReturn(Optional.of(reply));
        when(reviewReactionRepository.countByReviewId(10L)).thenReturn(1L);
        when(replyReactionRepository.countByReplyId(20L)).thenReturn(1L);
        ReviewService service = reviewService(CurrentActor.member(7L, 99L), memberReader(false));

        ArgumentCaptor<ReviewReaction> reviewReaction = ArgumentCaptor.forClass(ReviewReaction.class);
        ArgumentCaptor<ReplyReaction> replyReaction = ArgumentCaptor.forClass(ReplyReaction.class);

        // when
        service.createReviewReaction(10L);
        service.createReplyReaction(20L);

        // then
        verify(reviewReactionRepository).saveAndFlush(reviewReaction.capture());
        verify(replyReactionRepository).saveAndFlush(replyReaction.capture());
        assertThat(reviewReaction.getValue().getActorId()).isEqualTo(7L);
        assertThat(replyReaction.getValue().getActorId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("감상 작성 시 책을 검증하고 회원 익명 설정을 snapshot으로 저장한다")
    void should_ValidateBookAndSnapshotAnonymousSetting_When_CreatingReview() {
        // given
        when(memberReader.findByActorIds(List.of(1L))).thenReturn(Map.of(
                1L,
                new ReviewMemberProfile(
                        "닉네임",
                        "profile",
                        "참새-a1b2c3d4",
                        true,
                        false,
                        ActorType.MEMBER
                )
        ));
        ReviewService reviewService = reviewService(CurrentActor.member(1L, 1L), memberReader);
        when(reviewRepository.save(any(Review.class)))
                .thenAnswer(invocation -> savedReview(10L, invocation.getArgument(0)));

        // when
        ReviewResponse actual = reviewService.createReview(
                5L,
                new ReviewCreateRequest(
                        "감상",
                        null,
                        null,
                        10,
                        100,
                        false
                )
        );

        // then
        verify(bookReader).validateBookExists(5L);
        verify(readingRecordCoordinator).recordReview(1L, 5L, 10, 100);
        verify(memberReader).findByActorIds(List.of(1L));
        assertThat(actual.author().anonymous()).isTrue();
        assertThat(actual.author().displayName()).isEqualTo("참새-a1b2c3d4");
        assertThat(actual.author().memberId()).isNull();
        assertThat(actual.author().profileStatus()).isEqualTo(AuthorProfileStatus.UNAVAILABLE);
    }

    @Test
    @DisplayName("ISBN13으로 감상을 작성하면 해소된 도서 ID와 감상을 반환한다")
    void should_ReturnResolvedBookIdAndReview_When_CreatingReviewByIsbn13() {
        // given
        Book book = book();
        Isbn13 isbn13 = new Isbn13("9788925568683");
        when(bookResolver.findOrCreate(isbn13)).thenReturn(book);
        when(reviewRepository.save(any(Review.class)))
                .thenAnswer(invocation -> savedReview(10L, invocation.getArgument(0)));
        ReviewService reviewService = reviewService(CurrentActor.member(1L, 1L), memberReader(false));

        // when
        ReviewCreateByIsbnResponse actual = reviewService.createReviewByIsbn13(
                isbn13,
                new ReviewCreateRequest(
                        "감상",
                        null,
                        null,
                        null,
                        null,
                        false
                )
        );

        // then
        assertThat(actual.bookId()).isEqualTo(5L);
        assertThat(actual.review().reviewId()).isEqualTo(10L);
        verify(bookResolver).findOrCreate(isbn13);
        verify(bookReader).validateBookExists(5L);
    }

    @Test
    @DisplayName("게스트의 ISBN13 감상 페이지 입력은 도서를 등록하기 전에 거절한다")
    void should_RejectGuestReadingPageBeforeResolvingBook_When_CreatingReviewByIsbn13() {
        // given
        ReviewService reviewService = reviewService(CurrentActor.guest(7L), memberReader(false));

        // when & then
        assertThatThrownBy(() -> reviewService.createReviewByIsbn13(
                new Isbn13("9788925568683"),
                new ReviewCreateRequest(
                        "감상",
                        null,
                        null,
                        10,
                        100,
                        false
                )
        ))
                .isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN)
        );
        verify(bookResolver, never()).findOrCreate(any(Isbn13.class));
    }

    @Test
    @DisplayName("감상 페이지 수정은 저장된 전체 페이지를 확인하도록 서재 경계에 위임한다")
    void should_DelegateStoredTotalPagesValidation_When_UpdatingReviewPage() {
        // given
        ReviewService reviewService = reviewService(CurrentActor.member(1L, 1L), memberReader(false));
        Review review = savedReview(10L, Review.create(5L, 1L, "감상", null, null, 10, false, false));
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));
        ReviewUpdateRequest request = new ReviewUpdateRequest();
        request.setCurrentPage(50);
        request.setTotalPages(100);

        // when
        reviewService.updateReview(10L, request);

        // then
        verify(readingRecordCoordinator).validateReviewPage(1L, 5L, 50, 100);
    }

    @Test
    @DisplayName("감상 페이지가 전체 페이지를 초과하면 서재 경계의 독서 상태 오류를 반환한다")
    void should_ReturnInvalidReadingState_When_ReviewPageExceedsTotalPages() {
        // given
        ReviewService reviewService = reviewService(CurrentActor.member(1L, 1L), memberReader(false));
        doThrow(new BusinessException(ErrorCode.INVALID_READING_STATE))
                .when(readingRecordCoordinator).recordReview(1L, 5L, 101, 100);

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(
                5L,
                new ReviewCreateRequest(
                        "감상",
                        null,
                        null,
                        101,
                        100,
                        false
                )
        ))
                .isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_READING_STATE)
        );
    }

    @Test
    @DisplayName("도서별 댓글 수는 감상과 답글을 합산하고 항목이 없으면 0을 반환한다")
    void should_SumReviewsAndRepliesAndKeepZero_When_ReadingBookCommentCounts() {
        // given
        ReviewRepository.BookCommentCount reviewCount = mock(ReviewRepository.BookCommentCount.class);
        ReplyRepository.BookCommentCount replyCount = mock(ReplyRepository.BookCommentCount.class);
        when(reviewCount.getBookId()).thenReturn(5L);
        when(reviewCount.getCount()).thenReturn(2L);
        when(replyCount.getBookId()).thenReturn(5L);
        when(replyCount.getCount()).thenReturn(3L);
        when(reviewRepository.countByBookIdInGroupByBookId(List.of(5L, 6L)))
                .thenReturn(List.of(reviewCount));
        when(replyRepository.countByReviewBookIdInGroupByBookId(List.of(5L, 6L)))
                .thenReturn(List.of(replyCount));
        ReviewService reviewService = reviewService(CurrentActor.member(1L, 1L), memberReader(false));

        // when
        Map<Long, Long> counts = reviewService.getCommentCounts(List.of(5L, 6L));

        // then
        assertThat(counts).containsEntry(5L, 5L).containsEntry(6L, 0L);
    }

    @Test
    @DisplayName("도서별 감상과 답글 수를 분리해서 반환한다")
    void should_ReturnSeparatedReviewAndReplyCounts_When_ReadingBookActivityCounts() {
        // given
        ReviewRepository.BookCommentCount reviewCount = mock(ReviewRepository.BookCommentCount.class);
        ReplyRepository.BookCommentCount replyCount = mock(ReplyRepository.BookCommentCount.class);
        when(reviewCount.getBookId()).thenReturn(5L);
        when(reviewCount.getCount()).thenReturn(2L);
        when(replyCount.getBookId()).thenReturn(5L);
        when(replyCount.getCount()).thenReturn(3L);
        when(reviewRepository.countByBookIdInGroupByBookId(List.of(5L, 6L)))
                .thenReturn(List.of(reviewCount));
        when(replyRepository.countByReviewBookIdInGroupByBookId(List.of(5L, 6L)))
                .thenReturn(List.of(replyCount));
        ReviewService reviewService = reviewService(CurrentActor.member(1L, 1L), memberReader(false));

        // when
        Map<Long, ActivityCounts> actual = reviewService.getActivityCounts(List.of(5L, 6L));

        // then
        assertThat(actual).containsEntry(
                5L,
                new ActivityCounts(
                        2L,
                        3L
                )
        )
                .containsEntry(
                6L,
                new ActivityCounts(
                        0L,
                        0L
                )
        );
    }

    private Book book() {
        Book book = BookFixture.book(
                new Isbn13("9788925568683"),
                "마션",
                null,
                List.of("앤디 위어")
        );
        return BookFixture.savedBook(5L, book);
    }

    private ReviewService reviewService(CurrentActor actor, ReviewMemberReader profileReader) {
        return new ReviewService(
                reviewRepository,
                replyRepository,
                reviewReactionRepository,
                replyReactionRepository,
                () -> actor,
                readingRecordCoordinator,
                bookReader,
                profileReader,
                bookResolver,
                transactionManager()
        );
    }

    private PlatformTransactionManager transactionManager() {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        return transactionManager;
    }

    private ReviewMemberReader memberReader(boolean anonymousEnabled) {
        return actorIds -> actorIds.stream()
                .collect(Collectors.toMap(
                        actorId -> actorId,
                        actorId -> new ReviewMemberProfile(
                                "닉네임",
                                "profile",
                                "참새-a1b2c3d4",
                                anonymousEnabled,
                                false,
                                ActorType.MEMBER
                        )
                ));
    }
}
