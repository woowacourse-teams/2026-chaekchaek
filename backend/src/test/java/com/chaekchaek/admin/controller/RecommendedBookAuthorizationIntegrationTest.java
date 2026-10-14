package com.chaekchaek.admin.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.admin.domain.RecommendedBook;
import com.chaekchaek.admin.repository.RecommendedBookRepository;
import com.chaekchaek.auth.principal.SecurityContextCurrentActorProvider;
import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.repository.BookRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RecommendedBookAuthorizationIntegrationTest {

    private static final String RECOMMENDED_BOOKS_PATH = "/api/v1/admin/recommended-books";
    private static final String ISBN13 = "9788925568683";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private RecommendedBookRepository recommendedBookRepository;

    private Book book;

    @BeforeEach
    void setUpBook() {
        book = bookRepository.save(Book.create(
                new Isbn13(ISBN13),
                "마션",
                "https://example.com/martian.jpg",
                null,
                null,
                "책 설명",
                List.of("앤디 위어"),
                List.of(),
                "알에이치코리아",
                "SF",
                LocalDate.of(2015, 7, 10),
                308
        ));
    }

    @Test
    @DisplayName("회원과 게스트 인증 없이 추천 도서 목록을 조회한다")
    void should_ReturnRecommendedBooks_When_NoTokenIsProvided() throws Exception {
        // given
        recommendedBookRepository.save(RecommendedBook.create(book.getId(), Instant.now()));

        // when & then
        mockMvc.perform(get(RECOMMENDED_BOOKS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books[0].bookId").value(book.getId()))
                .andExpect(jsonPath("$.books[0].isbn13").value(ISBN13));
    }

    @Test
    @DisplayName("추천 도서 조회에 게스트 토큰 검증을 요구하지 않는다")
    void should_ReturnRecommendedBooks_When_GuestTokenIsInvalid() throws Exception {
        // when & then
        mockMvc.perform(get(RECOMMENDED_BOOKS_PATH)
                        .header(SecurityContextCurrentActorProvider.GUEST_TOKEN_HEADER, "invalid-guest-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books").isEmpty());
    }

}
