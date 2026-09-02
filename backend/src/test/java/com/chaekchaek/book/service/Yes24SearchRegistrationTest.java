package com.chaekchaek.book.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaekchaek.book.client.BookSearchResult;
import com.chaekchaek.book.client.Yes24BookClient;
import com.chaekchaek.book.client.fixture.Yes24MockServer;
import com.chaekchaek.book.client.fixture.Yes24ResponseFixture;
import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.repository.BookRepository;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@DataJpaTest
class Yes24SearchRegistrationTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Yes24MockServer yes24Server;

    @BeforeEach
    void setUp() throws IOException {
        yes24Server = new Yes24MockServer();
    }

    @AfterEach
    void tearDown() throws IOException {
        yes24Server.close();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("YES24 검색 결과의 ISBN13으로 알라딘 없이 도서를 등록한다")
    void should_RegisterBookWithoutAladin_When_ResolvingYes24SearchResult() throws InterruptedException {
        // given
        yes24Server.응답한다(200, Yes24ResponseFixture.헤르만_헤세_검색_결과());
        yes24Server.응답한다(200, Yes24ResponseFixture.데미안_상세_결과());
        Yes24BookClient client = new Yes24BookClient(
                RestClient.builder(), yes24Server.baseUrl(), yes24Server.apiKey()
        );
        BookResolver resolver = new BookResolver(client, bookRepository, transactionManager);

        // when
        BookSearchResult searchResult = client.search("헤르만 헤세", 1);
        Isbn13 isbn13 = new Isbn13(searchResult.items().get(0).isbn13());
        Book book = resolver.findOrCreate(isbn13);

        // then
        yes24Server.검색_요청을_검증한다("헤르만 헤세", 1);
        yes24Server.상세_요청을_검증한다(isbn13.value());
        assertThat(bookRepository.findByIsbn13(isbn13))
                .map(Book::getId)
                .contains(book.getId());
        assertThat(book.getTitle()).isEqualTo("데미안");
        assertThat(book.getDescription()).isEqualTo("내면의 길을 찾아가는 성장 소설");
        assertThat(book.getAuthors()).containsExactly("헤르만 헤세");
        assertThat(book.getTranslators()).containsExactly("전영애");
        assertThat(book.getTotalPages()).isEqualTo(240);
    }
}
