package com.chaekchaek.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chaekchaek.common.auth.CurrentMemberIdProvider;
import com.chaekchaek.review.book.ReviewBookReader;
import com.chaekchaek.review.domain.Review;
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
import org.springframework.test.util.ReflectionTestUtils;

class ReviewServiceTest {

    @Test
    @DisplayName("감상 작성 시 책을 검증하고 회원 익명 설정을 snapshot으로 저장한다")
    void should_ValidateBookAndSnapshotAnonymousSetting_When_CreatingReview() {
        // given
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        ReviewBookReader bookReader = mock(ReviewBookReader.class);
        ReadingRecordCoordinator readingRecordCoordinator = mock(ReadingRecordCoordinator.class);
        ReviewMemberReader memberReader = memberReader(true);
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

    private ReviewService reviewService(ReviewRepository reviewRepository, ReviewBookReader bookReader,
                                        ReadingRecordCoordinator readingRecordCoordinator,
                                        ReviewMemberReader memberReader) {
        CurrentMemberIdProvider currentMemberIdProvider = () -> 1L;
        return new ReviewService(reviewRepository, mock(ReplyRepository.class), mock(ReviewReactionRepository.class),
                mock(ReplyReactionRepository.class), currentMemberIdProvider, readingRecordCoordinator, bookReader,
                memberReader);
    }

    private ReviewMemberReader memberReader(boolean anonymousEnabled) {
        return memberIds -> memberIds.stream().collect(java.util.stream.Collectors.toMap(
                memberId -> memberId,
                memberId -> new ReviewMemberProfile("닉네임", "profile", "참새-a1b2c3d4", anonymousEnabled, false)
        ));
    }
}
