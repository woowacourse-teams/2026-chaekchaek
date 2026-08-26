package com.chaekchaek.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.chaekchaek.common.auth.CurrentActor;
import com.chaekchaek.common.auth.CurrentActorProvider;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.library.service.BookActivityCountReader.ActivityCounts;
import com.chaekchaek.review.book.ReviewBookReader;
import com.chaekchaek.review.domain.Review;
import com.chaekchaek.review.domain.Reply;
import com.chaekchaek.review.domain.ReviewReaction;
import com.chaekchaek.review.domain.ReplyReaction;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class ReviewServiceTest {

    @Test
    @DisplayName("게스트는 서재를 변경하지 않고 감상을 작성한다")
    void should_CreateGuestReviewWithoutChangingLibrary() {
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        ReviewBookReader bookReader = mock(ReviewBookReader.class);
        ReadingRecordCoordinator readingRecordCoordinator = mock(ReadingRecordCoordinator.class);
        ReviewMemberReader memberReader = mock(ReviewMemberReader.class);
        when(memberReader.findByActorIds(List.of(7L))).thenReturn(Map.of(
                7L, new ReviewMemberProfile("게스트", null, "다정한 참새", true, false)));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            ReflectionTestUtils.setField(review, "id", 10L);
            return review;
        });
        ReviewService service = new ReviewService(reviewRepository, mock(ReplyRepository.class),
                mock(ReviewReactionRepository.class), mock(ReplyReactionRepository.class),
                () -> CurrentActor.guest(7L), readingRecordCoordinator, bookReader, memberReader);

        ReviewResponse response = service.createReview(5L,
                new ReviewCreateRequest("게스트 감상", null, null, null, null, false));

        assertThat(response.author().displayName()).isEqualTo("다정한 참새");
        verify(readingRecordCoordinator, never()).recordReview(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("게스트는 감상에서 개인 독서 쪽수를 입력할 수 없다")
    void should_RejectReadingPage_When_GuestCreatesReview() {
        ReviewService service = new ReviewService(mock(ReviewRepository.class), mock(ReplyRepository.class),
                mock(ReviewReactionRepository.class), mock(ReplyReactionRepository.class),
                () -> CurrentActor.guest(7L), mock(ReadingRecordCoordinator.class),
                mock(ReviewBookReader.class), memberReader(true));

        assertThatThrownBy(() -> service.createReview(5L,
                new ReviewCreateRequest("게스트 감상", null, null, 10, 100, false)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("감상과 답글 좋아요는 현재 Actor에 귀속된다")
    void should_AssignReactionsToCurrentActor() {
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        ReplyRepository replyRepository = mock(ReplyRepository.class);
        ReviewReactionRepository reviewReactionRepository = mock(ReviewReactionRepository.class);
        ReplyReactionRepository replyReactionRepository = mock(ReplyReactionRepository.class);
        Review review = Review.create(5L, 1L, "감상", null, null, null, false, false);
        Reply reply = Reply.create(10L, 1L, "답글", false);
        ReflectionTestUtils.setField(review, "id", 10L);
        ReflectionTestUtils.setField(reply, "id", 20L);
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));
        when(replyRepository.findById(20L)).thenReturn(Optional.of(reply));
        when(reviewReactionRepository.countByReviewId(10L)).thenReturn(1L);
        when(replyReactionRepository.countByReplyId(20L)).thenReturn(1L);
        CurrentActorProvider currentActorProvider = () -> CurrentActor.member(7L, 99L);
        ReviewService service = new ReviewService(reviewRepository, replyRepository, reviewReactionRepository,
                replyReactionRepository, currentActorProvider,
                mock(ReadingRecordCoordinator.class), mock(ReviewBookReader.class), memberReader(false));

        service.createReviewReaction(10L);
        service.createReplyReaction(20L);

        ArgumentCaptor<ReviewReaction> reviewReaction = ArgumentCaptor.forClass(ReviewReaction.class);
        ArgumentCaptor<ReplyReaction> replyReaction = ArgumentCaptor.forClass(ReplyReaction.class);
        verify(reviewReactionRepository).saveAndFlush(reviewReaction.capture());
        verify(replyReactionRepository).saveAndFlush(replyReaction.capture());
        assertThat(reviewReaction.getValue().getActorId()).isEqualTo(7L);
        assertThat(replyReaction.getValue().getActorId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("감상 작성 시 책을 검증하고 회원 익명 설정을 snapshot으로 저장한다")
    void should_ValidateBookAndSnapshotAnonymousSetting_When_CreatingReview() {
        // given
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        ReviewBookReader bookReader = mock(ReviewBookReader.class);
        ReadingRecordCoordinator readingRecordCoordinator = mock(ReadingRecordCoordinator.class);
        ReviewMemberReader memberReader = mock(ReviewMemberReader.class);
        when(memberReader.findByActorIds(List.of(1L))).thenReturn(Map.of(
                1L, new ReviewMemberProfile("닉네임", "profile", "참새-a1b2c3d4", true, false)
        ));
        ReviewService reviewService = reviewService(reviewRepository, bookReader, readingRecordCoordinator, memberReader);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            ReflectionTestUtils.setField(review, "id", 10L);
            return review;
        });

        // when
        ReviewResponse actual = reviewService.createReview(5L,
                new ReviewCreateRequest("감상", null, null, 10, 100, false));

        // then
        verify(bookReader).validateBookExists(5L);
        verify(readingRecordCoordinator).recordReview(1L, 5L, 10, 100);
        verify(memberReader).findByActorIds(List.of(1L));
        assertThat(actual.author().anonymous()).isTrue();
        assertThat(actual.author().displayName()).isEqualTo("참새-a1b2c3d4");
    }

    @Test
    @DisplayName("감상 페이지 수정은 저장된 전체 페이지를 확인하도록 서재 경계에 위임한다")
    void should_DelegateStoredTotalPagesValidation_When_UpdatingReviewPage() {
        // given
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        ReviewBookReader bookReader = mock(ReviewBookReader.class);
        ReadingRecordCoordinator readingRecordCoordinator = mock(ReadingRecordCoordinator.class);
        ReviewService reviewService = reviewService(reviewRepository, bookReader, readingRecordCoordinator, memberReader(false));
        Review review = Review.create(5L, 1L, "감상", null, null, 10, false, false);
        ReflectionTestUtils.setField(review, "id", 10L);
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));
        ReviewUpdateRequest request = new ReviewUpdateRequest();
        request.setCurrentPage(50);
        request.setTotalPages(100);

        // when
        reviewService.updateReview(10L, request);

        // then
        verify(readingRecordCoordinator).validateReviewPage(5L, 50, 100);
    }

    @Test
    @DisplayName("감상 페이지가 전체 페이지를 초과하면 서재 경계의 독서 상태 오류를 반환한다")
    void should_ReturnInvalidReadingState_When_ReviewPageExceedsTotalPages() {
        // given
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        ReviewBookReader bookReader = mock(ReviewBookReader.class);
        ReadingRecordCoordinator readingRecordCoordinator = mock(ReadingRecordCoordinator.class);
        ReviewService reviewService = reviewService(
                reviewRepository, bookReader, readingRecordCoordinator, memberReader(false));
        doThrow(new BusinessException(ErrorCode.INVALID_READING_STATE))
                .when(readingRecordCoordinator).recordReview(1L, 5L, 101, 100);

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(5L,
                new ReviewCreateRequest("감상", null, null, 101, 100, false)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_READING_STATE));
    }

    @Test
    @DisplayName("도서별 댓글 수는 감상과 답글을 합산하고 항목이 없으면 0을 반환한다")
    void should_SumReviewsAndRepliesAndKeepZero_When_ReadingBookCommentCounts() {
        // given
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        ReplyRepository replyRepository = mock(ReplyRepository.class);
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
        ReviewService reviewService = new ReviewService(
                reviewRepository, replyRepository, mock(ReviewReactionRepository.class),
                mock(ReplyReactionRepository.class),
                currentActorProvider(),
                mock(ReadingRecordCoordinator.class), mock(ReviewBookReader.class), memberReader(false));

        // when
        Map<Long, Long> counts = reviewService.getCommentCounts(List.of(5L, 6L));

        // then
        assertThat(counts).containsEntry(5L, 5L).containsEntry(6L, 0L);
    }

    @Test
    @DisplayName("도서별 감상과 답글 수를 분리해서 반환한다")
    void should_ReturnSeparatedReviewAndReplyCounts_When_ReadingBookActivityCounts() {
        // given
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        ReplyRepository replyRepository = mock(ReplyRepository.class);
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
        ReviewService reviewService = new ReviewService(
                reviewRepository, replyRepository, mock(ReviewReactionRepository.class),
                mock(ReplyReactionRepository.class),
                currentActorProvider(),
                mock(ReadingRecordCoordinator.class), mock(ReviewBookReader.class), memberReader(false));

        // when
        Map<Long, ActivityCounts> actual = reviewService.getActivityCounts(List.of(5L, 6L));

        // then
        assertThat(actual).containsEntry(5L, new ActivityCounts(2L, 3L))
                .containsEntry(6L, new ActivityCounts(0L, 0L));
    }

    private ReviewService reviewService(ReviewRepository reviewRepository, ReviewBookReader bookReader,
                                        ReadingRecordCoordinator readingRecordCoordinator,
                                        ReviewMemberReader memberReader) {
        return new ReviewService(reviewRepository, mock(ReplyRepository.class), mock(ReviewReactionRepository.class),
                mock(ReplyReactionRepository.class), currentActorProvider(),
                readingRecordCoordinator, bookReader, memberReader);
    }

    private ReviewMemberReader memberReader(boolean anonymousEnabled) {
        return actorIds -> actorIds.stream().collect(java.util.stream.Collectors.toMap(
                actorId -> actorId,
                actorId -> new ReviewMemberProfile("닉네임", "profile", "참새-a1b2c3d4", anonymousEnabled, false)
        ));
    }

    private CurrentActorProvider currentActorProvider() {
        return () -> CurrentActor.member(1L, 1L);
    }
}
