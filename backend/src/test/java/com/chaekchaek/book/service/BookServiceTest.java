package com.chaekchaek.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.dto.BookDetailResponse;
import com.chaekchaek.book.repository.BookRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BookServiceTest {

    @Test
    @DisplayName("등록된 ISBN13을 resolve하면 상세 정보를 반환한다")
    void should_ReturnStoredBookDetail_When_Isbn13IsRegistered() {
        // given
        BookResolver bookResolver = mock(BookResolver.class);
        BookRepository repository = mock(BookRepository.class);
        BookDetailAssembler detailAssembler = mock(BookDetailAssembler.class);
        BookService service = new BookService(bookResolver, repository, detailAssembler);
        Book book = Book.create(
                "9788925568683", "마션", "https://image.example/martian.jpg",
                List.of("앤디 위어"), List.of("박아람"), "알에이치코리아", "SF",
                LocalDate.of(2026, 1, 1), 308
        );
        BookDetailResponse detailResponse = new BookDetailResponse(
                1L, book.getIsbn13(), book.getTitle(), book.getCoverImageUrl(), book.getAuthors(),
                book.getTranslators(), book.getPublisher(), book.getCategory(), "2026-01-01", 308,
                0, null, 0, null);
        when(bookResolver.resolve("9788925568683")).thenReturn(book);
        when(detailAssembler.assemble(book)).thenReturn(detailResponse);

        // when
        BookDetailResponse response = service.resolve("9788925568683");

        // then
        assertThat(response).extracting(BookDetailResponse::title, BookDetailResponse::totalPages)
                .containsExactly("마션", 308);
    }
}
