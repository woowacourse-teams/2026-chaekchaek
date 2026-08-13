package com.chaekchaek.library.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LibraryItemTest {

    private static final Instant NOW = Instant.parse("2026-08-14T00:00:00Z");

    @Test
    @DisplayName("완독 상태로 변경하면 마지막 페이지가 된다")
    void should_SetCurrentPageToTotalPages_When_ChangingStatusToFinished() {
        // given
        LibraryItem item = LibraryItem.create(1L, 2L, ReadingStatus.READING, 300, NOW);

        // when
        item.changeStatus(ReadingStatus.FINISHED, null, NOW.plusSeconds(1));

        // then
        assertThat(item.getStatus()).isEqualTo(ReadingStatus.FINISHED);
        assertThat(item.getCurrentPage()).isEqualTo(300);
    }

    @Test
    @DisplayName("완독 상태에서 진도를 낮추면 읽는 중 상태가 된다")
    void should_ChangeStatusToReading_When_ChangingPageBelowLastPageFromFinished() {
        // given
        LibraryItem item = LibraryItem.create(1L, 2L, ReadingStatus.FINISHED, 300, NOW);

        // when
        item.changeCurrentPage(120, null, NOW.plusSeconds(1));

        // then
        assertThat(item.getStatus()).isEqualTo(ReadingStatus.READING);
        assertThat(item.getCurrentPage()).isEqualTo(120);
    }

    @Test
    @DisplayName("읽는 중 상태에서 0페이지로 바꾸면 읽는 중 상태를 유지한다")
    void should_KeepReading_When_ChangingPageToZeroFromReading() {
        // given
        LibraryItem item = LibraryItem.create(1L, 2L, ReadingStatus.READING, 300, NOW);

        // when
        item.changeCurrentPage(0, null, NOW.plusSeconds(1));

        // then
        assertThat(item.getStatus()).isEqualTo(ReadingStatus.READING);
        assertThat(item.getCurrentPage()).isZero();
    }

    @Test
    @DisplayName("서재 상태가 바뀌지 않으면 읽기 갱신 시각을 유지한다")
    void should_KeepReadingUpdatedAt_When_StatusAndPageAreUnchanged() {
        // given
        LibraryItem item = LibraryItem.create(1L, 2L, ReadingStatus.WANT_TO_READ, null, NOW);

        // when
        item.changeCurrentPage(0, null, NOW.plusSeconds(1));

        // then
        assertThat(item.getReadingUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("허용 범위 밖 별점은 거부한다")
    void should_ThrowException_When_RatingIsOutsideAllowedRange() {
        // given
        LibraryItem item = LibraryItem.create(1L, 2L, ReadingStatus.WANT_TO_READ, null, NOW);

        // when & then
        assertThatThrownBy(() -> item.rate(new BigDecimal("4.25"), NOW))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    @DisplayName("전체 페이지 없이 완독 상태로 변경하면 읽기 상태 오류를 반환한다")
    void should_ThrowInvalidReadingState_When_FinishingWithoutTotalPages() {
        // given
        LibraryItem item = LibraryItem.create(1L, 2L, ReadingStatus.WANT_TO_READ, null, NOW);

        // when & then
        assertThatThrownBy(() -> item.changeStatus(ReadingStatus.FINISHED, null, NOW))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_READING_STATE));
    }
}
