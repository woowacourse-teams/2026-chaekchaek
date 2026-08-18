package com.chaekchaek.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.dto.BookDetailResponse;
import com.chaekchaek.book.repository.BookRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BookServiceTest {

    @Test
    @DisplayName("등록된 도서 ID로 상세 정보를 조회한다")
    void should_ReturnStoredBookDetail_When_BookIdIsRegistered() {
        // given
        BookRepository repository = mock(BookRepository.class);
        BookDetailAssembler detailAssembler = mock(BookDetailAssembler.class);
        BookService service = new BookService(repository, detailAssembler);
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
        when(repository.findDetailById(1L)).thenReturn(Optional.of(detailBook));
        when(detailAssembler.assemble(detailBook)).thenReturn(detailResponse);

        // when
        BookDetailResponse response = service.getDetail(1L);

        // then
        assertThat(response).extracting(BookDetailResponse::title, BookDetailResponse::totalPages)
                .containsExactly("마션", 308);
    }
}
