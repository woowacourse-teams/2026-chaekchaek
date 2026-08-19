package com.chaekchaek.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.dto.BookDetailResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BookServiceTest {

    @Test
    @DisplayName("등록된 ISBN13으로 상세 정보를 조회한다")
    void should_ReturnStoredBookDetail_When_Isbn13IsRegistered() {
        // given
        BookResolver bookResolver = mock(BookResolver.class);
        BookDetailAssembler detailAssembler = mock(BookDetailAssembler.class);
        BookService service = new BookService(bookResolver, detailAssembler);
        Book detailBook = Book.create(
                "9788925568683", "마션", "https://image.example/martian.jpg",
                List.of("앤디 위어"), List.of("박아람"), "알에이치코리아", "SF",
                LocalDate.of(2026, 1, 1), 308
        );
        BookDetailResponse detailResponse = new BookDetailResponse(
                1L, detailBook.getIsbn13(), detailBook.getTitle(), detailBook.getCoverImageUrl(),
                detailBook.getAuthors(), detailBook.getTranslators(), detailBook.getPublisher(),
                detailBook.getCategory(), "2026-01-01", 308,
                0, null, 0, null);
        when(bookResolver.lookup("9788925568683")).thenReturn(detailBook);
        when(detailAssembler.assemble(detailBook)).thenReturn(detailResponse);

        // when
        BookDetailResponse response = service.getDetail("9788925568683");

        // then
        assertThat(response).extracting(BookDetailResponse::title, BookDetailResponse::totalPages)
                .containsExactly("마션", 308);
    }
}
