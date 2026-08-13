package com.chaekchaek.review.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaekchaek.review.exception.ReviewErrorCode;
import com.chaekchaek.review.exception.ReviewException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReviewTest {

    @Test
    @DisplayName("작성자만 감상을 삭제할 수 있다")
    void should_DeleteReview_When_AuthorDeletes() {
        // given
        Review review = Review.create(1L, 2L, "감상", null, null, null, false, false);

        // when
        review.deleteBy(2L);

        // then
        assertThat(review.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("삭제된 감상은 다시 수정할 수 없다")
    void should_ThrowDeletedResource_When_UpdatingDeletedReview() {
        // given
        Review review = Review.create(1L, 2L, "감상", null, null, null, false, false);
        review.deleteBy(2L);

        // when & then
        assertThatThrownBy(() -> review.assertModifiableBy(2L))
                .isInstanceOf(ReviewException.class)
                .extracting(exception -> ((ReviewException) exception).getErrorCode())
                .isEqualTo(ReviewErrorCode.DELETED_RESOURCE);
    }

    @Test
    @DisplayName("다른 회원은 감상을 수정할 수 없다")
    void should_ThrowForbidden_When_NonAuthorUpdatesReview() {
        // given
        Review review = Review.create(1L, 2L, "감상", null, null, null, false, false);

        // when & then
        assertThatThrownBy(() -> review.assertModifiableBy(3L))
                .isInstanceOf(ReviewException.class)
                .extracting(exception -> ((ReviewException) exception).getErrorCode())
                .isEqualTo(ReviewErrorCode.FORBIDDEN);
    }
}
