package com.chaekchaek.review.book;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.exception.BookNotFoundException;
import com.chaekchaek.book.repository.BookRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PersistentReviewBookReaderTest {

    @Test
    @DisplayName("감상 대상 도서가 존재하지 않으면 책 없음 오류를 반환한다")
    void should_ThrowBookNotFound_When_ReviewBookDoesNotExist() {
        // given
        BookRepository bookRepository = mock(BookRepository.class);
        when(bookRepository.existsById(42L)).thenReturn(false);
        PersistentReviewBookReader reader = new PersistentReviewBookReader(bookRepository);

        // when & then
        assertThatThrownBy(() -> reader.validateBookExists(42L))
                .isInstanceOf(BookNotFoundException.class);
    }
}
