package com.chaekchaek.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.client.AladinBookClient;
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
    @DisplayName("등록된 ISBN13을 resolve하면 외부 API를 호출하지 않고 상세 정보를 반환한다")
    void should_ReturnStoredBookWithoutCallingAladin_When_Isbn13IsRegistered() {
        // given
        AladinBookClient client = mock(AladinBookClient.class);
        BookRepository repository = mock(BookRepository.class);
        BookService service = new BookService(client, repository);
        Book book = Book.create(
                "9788925568683", "마션", "https://image.example/martian.jpg",
                List.of("앤디 위어"), List.of("박아람"), "알에이치코리아", "SF",
                LocalDate.of(2026, 1, 1), 308
        );
        when(repository.findByIsbn13("9788925568683")).thenReturn(Optional.of(book));

        // when
        BookDetailResponse response = service.resolve("9788925568683");

        // then
        assertThat(response).extracting(BookDetailResponse::title, BookDetailResponse::totalPages)
                .containsExactly("마션", 308);
        verifyNoInteractions(client);
    }
}
