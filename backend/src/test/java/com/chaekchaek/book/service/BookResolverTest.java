package com.chaekchaek.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.client.BookDetailItem;
import com.chaekchaek.book.client.BookSearchClient;
import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.repository.BookRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

class BookResolverTest {

    private static final Isbn13 ISBN13 = new Isbn13("9788925568683");

    @Test
    @DisplayName("등록된 ISBN13으로 책을 조회하거나 생성하면 기존 책을 반환한다")
    void should_ReturnStoredBookWithoutCallingClient_When_BookIsRegistered() {
        // given
        BookSearchClient client = mock(BookSearchClient.class);
        BookRepository repository = mock(BookRepository.class);
        BookResolver resolver = new BookResolver(client, repository, mock(PlatformTransactionManager.class));
        Book storedBook = mock(Book.class);
        when(repository.findByIsbn13(ISBN13)).thenReturn(Optional.of(storedBook));

        // when
        Book book = resolver.findOrCreate(ISBN13);

        // then
        assertThat(book).isSameAs(storedBook);
        verify(repository).findByIsbn13(ISBN13);
        verifyNoInteractions(client);
    }

    @Test
    @DisplayName("미등록 ISBN13을 조회하면 외부 상세 결과를 저장하지 않고 반환한다")
    void should_ReturnUnpersistedBookWithoutSaving_When_LookingUpUnregisteredIsbn13() {
        // given
        BookSearchClient client = mock(BookSearchClient.class);
        BookRepository repository = mock(BookRepository.class);
        BookResolver resolver = new BookResolver(client, repository, mock(PlatformTransactionManager.class));
        when(repository.findByIsbn13(ISBN13)).thenReturn(Optional.empty());
        when(client.findBookByIsbn13(ISBN13)).thenReturn(bookDetailItem());

        // when
        Book book = resolver.lookup(ISBN13);

        // then
        assertThat(book.getId()).isNull();
        assertThat(book.getTitle()).isEqualTo("마션");
        assertThat(book.getDescription()).isEqualTo("책 설명");
        verify(client).findBookByIsbn13(ISBN13);
        verify(repository).findByIsbn13(ISBN13);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any(Book.class));
        verify(repository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any(Book.class));
    }

    @Test
    @DisplayName("외부 도서 설명에 HTML 엔티티가 있으면 디코딩해서 반환한다")
    void should_UnescapeHtmlEntities_When_DescriptionContainsHtmlEntities() {
        // given
        BookSearchClient client = mock(BookSearchClient.class);
        BookRepository repository = mock(BookRepository.class);
        BookResolver resolver = new BookResolver(client, repository, mock(PlatformTransactionManager.class));
        when(repository.findByIsbn13(ISBN13)).thenReturn(Optional.empty());
        when(client.findBookByIsbn13(ISBN13)).thenReturn(
                bookDetailItem("&lt;프리즘&gt;은 &quot;빛&quot;을 나눈다 &amp; 다시 합친다. &#39;끝&#39;"));

        // when
        Book book = resolver.lookup(ISBN13);

        // then
        assertThat(book.getDescription()).isEqualTo("<프리즘>은 \"빛\"을 나눈다 & 다시 합친다. '끝'");
    }

    @Test
    @DisplayName("외부 도서 설명에 HTML 엔티티가 없으면 원문을 그대로 반환한다")
    void should_KeepDescriptionAsIs_When_DescriptionHasNoHtmlEntities() {
        // given
        BookSearchClient client = mock(BookSearchClient.class);
        BookRepository repository = mock(BookRepository.class);
        BookResolver resolver = new BookResolver(client, repository, mock(PlatformTransactionManager.class));
        when(repository.findByIsbn13(ISBN13)).thenReturn(Optional.empty());
        when(client.findBookByIsbn13(ISBN13)).thenReturn(bookDetailItem("화성에 홀로 남은 식물학자의 이야기"));

        // when
        Book book = resolver.lookup(ISBN13);

        // then
        assertThat(book.getDescription()).isEqualTo("화성에 홀로 남은 식물학자의 이야기");
    }

    private BookDetailItem bookDetailItem() {
        return bookDetailItem("책 설명");
    }

    private BookDetailItem bookDetailItem(String description) {
        return new BookDetailItem(
                "마션", "https://image.example/martian.jpg", description,
                List.of("앤디 위어"), List.of(), LocalDate.of(2026, 1, 1),
                ISBN13.value(), "SF", "알에이치코리아", 308
        );
    }
}
