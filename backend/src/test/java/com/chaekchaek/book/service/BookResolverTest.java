package com.chaekchaek.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.client.AladinBookClient;
import com.chaekchaek.book.client.dto.AladinBookItem;
import com.chaekchaek.book.client.dto.AladinBookSubInfo;
import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

class BookResolverTest {

    private static final String ISBN13 = "9788925568683";

    @Test
    @DisplayName("등록된 ISBN13으로 책을 조회하거나 생성하면 기존 책을 반환한다")
    void should_ReturnStoredBookWithoutCallingAladin_When_BookIsRegistered() {
        // given
        AladinBookClient client = mock(AladinBookClient.class);
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
    @DisplayName("잘못된 ISBN13으로 책을 조회하거나 생성하면 입력 오류를 던진다")
    void should_ThrowInvalidRequestException_When_ResolvingMalformedIsbn13() {
        // given
        AladinBookClient client = mock(AladinBookClient.class);
        BookRepository repository = mock(BookRepository.class);
        BookResolver resolver = new BookResolver(client, repository, mock(PlatformTransactionManager.class));

        // when & then
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> resolver.findOrCreate("9788925568682"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
        verifyNoInteractions(client, repository);
    }

    @Test
    @DisplayName("미등록 ISBN13을 조회하면 알라딘 결과를 저장하지 않고 반환한다")
    void should_ReturnUnpersistedBookWithoutSaving_When_LookingUpUnregisteredIsbn13() {
        // given
        AladinBookClient client = mock(AladinBookClient.class);
        BookRepository repository = mock(BookRepository.class);
        BookResolver resolver = new BookResolver(client, repository, mock(PlatformTransactionManager.class));
        when(repository.findByIsbn13(ISBN13)).thenReturn(Optional.empty());
        when(client.findBookByIsbn13(ISBN13)).thenReturn(aladinBook());

        // when
        Book book = resolver.lookup(ISBN13);

        // then
        assertThat(book.getId()).isNull();
        assertThat(book.getTitle()).isEqualTo("마션");
        verify(client).findBookByIsbn13(ISBN13);
        verify(repository).findByIsbn13(ISBN13);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any(Book.class));
        verify(repository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any(Book.class));
    }

    private AladinBookItem aladinBook() {
        return new AladinBookItem(
                "마션", "https://image.example/martian.jpg", "앤디 위어 (지은이)",
                "2026-01-01", ISBN13, "SF", "알에이치코리아", new AladinBookSubInfo(308)
        );
    }
}
