package com.chaekchaek.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.client.BookDetailItem;
import com.chaekchaek.book.client.BookSearchClient;
import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.repository.BookRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@ActiveProfiles("test")
class BookResolveConcurrencyTest {

    private static final Isbn13 ISBN13 = new Isbn13("9788925568683");

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("동시에 같은 ISBN13으로 책을 조회하거나 생성하면 하나의 책만 등록한다")
    void should_ReturnSameBookIdAndPersistOneBook_When_ResolveIsConcurrent() throws Exception {
        // given
        BookSearchClient client = mock(BookSearchClient.class);
        CyclicBarrier fetchedByBothRequests = new CyclicBarrier(2);
        when(client.findBookByIsbn13(ISBN13)).thenAnswer(invocation -> {
            fetchedByBothRequests.await(5, TimeUnit.SECONDS);
            return new BookDetailItem(
                    "마션", "https://image.example/martian.jpg", "책 설명",
                    List.of("앤디 위어"), List.of(), LocalDate.of(2026, 1, 1),
                    ISBN13.value(), "SF", "알에이치코리아", 308
            );
        });
        BookResolver resolver = new BookResolver(client, bookRepository, transactionManager);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            // when
            CompletableFuture<Book> first = CompletableFuture.supplyAsync(
                    () -> resolver.findOrCreate(ISBN13), executor
            );
            CompletableFuture<Book> second = CompletableFuture.supplyAsync(
                    () -> resolver.findOrCreate(ISBN13), executor
            );

            Book firstResponse = first.get(10, TimeUnit.SECONDS);
            Book secondResponse = second.get(10, TimeUnit.SECONDS);

            // then
            assertThat(firstResponse.getId()).isEqualTo(secondResponse.getId());
            assertThat(firstResponse.getDescription()).isEqualTo("책 설명");
            assertThat(secondResponse.getDescription()).isEqualTo("책 설명");
            assertThat(bookRepository.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }
}
