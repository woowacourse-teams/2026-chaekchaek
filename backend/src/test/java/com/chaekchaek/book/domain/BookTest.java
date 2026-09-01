package com.chaekchaek.book.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BookTest {

    @Test
    @DisplayName("책은 검증된 ISBN13 값 객체를 보관한다")
    void should_ReturnIsbn13ValueObject_When_BookIsCreated() {
        // given
        Book book = bookWithoutTotalPages();

        // when & then
        assertThat(book.getIsbn13()).isEqualTo(new Isbn13("9788925568683"));
    }

    @Test
    @DisplayName("전체 페이지가 없던 책에 처음 입력한 값을 저장한다")
    void should_RecordTotalPages_When_BookDoesNotHaveTotalPages() {
        // given
        Book book = bookWithoutTotalPages();

        // when
        book.rememberTotalPages(308);

        // then
        assertThat(book.getTotalPages()).isEqualTo(308);
    }

    @Test
    @DisplayName("기존 전체 페이지와 다른 값을 입력하면 충돌 예외를 던진다")
    void should_ThrowConflictException_When_RequestedTotalPagesDiffersFromStoredValue() {
        // given
        Book book = bookWithoutTotalPages();
        book.rememberTotalPages(308);

        // when & then
        assertThatThrownBy(() -> book.rememberTotalPages(400))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.TOTAL_PAGES_CONFLICT);
    }

    @Test
    @DisplayName("0 이하의 전체 페이지는 저장할 수 없다")
    void should_ThrowIllegalArgumentException_When_TotalPagesIsNotPositive() {
        // given
        Book book = bookWithoutTotalPages();

        // when & then
        assertThatThrownBy(() -> book.rememberTotalPages(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Book bookWithoutTotalPages() {
        return Book.create(
                new Isbn13("9788925568683"), "마션", "https://image.example/martian.jpg",
                null,
                List.of("앤디 위어"), List.of("박아람"), "알에이치코리아", "SF",
                LocalDate.of(2026, 1, 1), null
        );
    }
}
