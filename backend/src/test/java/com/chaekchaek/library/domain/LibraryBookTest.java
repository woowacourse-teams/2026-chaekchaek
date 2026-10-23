package com.chaekchaek.library.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LibraryBookTest {

    @Test
    @DisplayName("책장 배치가 도서와 양의 순서를 보관한다")
    void should_HoldBookAndDisplayOrder_When_Created() {
        // when
        LibraryBook placement = LibraryBook.create(10L, 20L, 3L);

        // then
        assertThat(placement.getLibraryId()).isEqualTo(10L);
        assertThat(placement.getBookId()).isEqualTo(20L);
        assertThat(placement.getDisplayOrder()).isEqualTo(3L);
    }

    @Test
    @DisplayName("책장 배치 식별값과 순서는 양수여야 한다")
    void should_RejectNonPositiveValues_When_Created() {
        // when & then
        assertThatThrownBy(() -> LibraryBook.create(0L, 20L, 1L))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST)
                );
        assertThatThrownBy(() -> LibraryBook.create(10L, 20L, 0L))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST)
                );
    }
}
