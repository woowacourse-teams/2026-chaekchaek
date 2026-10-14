package com.chaekchaek.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.actor.domain.Actor;
import com.chaekchaek.actor.repository.ActorRepository;
import com.chaekchaek.admin.domain.RecommendedBook;
import com.chaekchaek.admin.repository.RecommendedBookRepository;
import com.chaekchaek.auth.principal.SecurityContextCurrentActorProvider;
import com.chaekchaek.auth.token.access.AccessTokenProvider;
import com.chaekchaek.auth.token.cookie.AuthCookieProvider;
import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RecommendedBookAuthorizationIntegrationTest {

    private static final String RECOMMENDED_BOOKS_PATH = "/api/v1/recommended-books";
    private static final String ISBN13 = "9788925568683";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private RecommendedBookRepository recommendedBookRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ActorRepository actorRepository;

    @Autowired
    private AccessTokenProvider accessTokenProvider;

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

    @ParameterizedTest
    @ValueSource(strings = {"POST", "DELETE"})
    @DisplayName("관리자의 Access Token 쿠키로 추천 도서를 변경한다")
    void should_MutateRecommendedBook_When_AdminCookieIsProvided(String method) throws Exception {
        // given
        String accessToken = issueAccessToken(true);
        MockHttpServletRequestBuilder request = mutationRequest(method)
                .cookie(new Cookie(AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME, accessToken));

        // when & then
        mockMvc.perform(request).andExpect(status().is(successStatus(method)));
        assertThat(recommendedBookRepository.existsByBookId(book.getId())).isEqualTo(method.equals("POST"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "DELETE"})
    @DisplayName("관리자의 Bearer 헤더로도 추천 도서를 변경한다")
    void should_MutateRecommendedBook_When_AdminBearerHeaderIsProvided(String method) throws Exception {
        // given
        String accessToken = issueAccessToken(true);
        MockHttpServletRequestBuilder request = mutationRequest(method)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

        // when & then
        mockMvc.perform(request).andExpect(status().is(successStatus(method)));
        assertThat(recommendedBookRepository.existsByBookId(book.getId())).isEqualTo(method.equals("POST"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "DELETE"})
    @DisplayName("인증 토큰 없이 추천 도서 변경을 요청하면 거절한다")
    void should_RejectMutation_When_NoTokenIsProvided(String method) throws Exception {
        // when & then
        mockMvc.perform(mutationRequest(method))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        assertRecommendationUnchanged(method);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "DELETE"})
    @DisplayName("변조된 Access Token 쿠키로 추천 도서 변경을 요청하면 거절한다")
    void should_RejectMutation_When_AccessTokenCookieIsTampered(String method) throws Exception {
        // given
        String accessToken = issueAccessToken(true) + "tampered";

        // when & then
        mockMvc.perform(mutationRequest(method)
                        .cookie(new Cookie(AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME, accessToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        assertRecommendationUnchanged(method);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "DELETE"})
    @DisplayName("일반 회원의 Access Token 쿠키로 추천 도서 변경을 요청하면 거절한다")
    void should_RejectMutation_When_MemberCookieIsProvided(String method) throws Exception {
        // given
        String accessToken = issueAccessToken(false);

        // when & then
        mockMvc.perform(mutationRequest(method)
                        .cookie(new Cookie(AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME, accessToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        assertRecommendationUnchanged(method);
    }

    private String issueAccessToken(boolean admin) {
        LocalDateTime now = LocalDateTime.now();
        Member member = memberRepository.save(Member.create("추천 도서 인증 회원", null, now));
        Actor actor = Actor.member(member, now);
        if (admin) {
            actor.grantAdmin();
        }
        actorRepository.save(actor);
        return accessTokenProvider.issue(member);
    }

    private MockHttpServletRequestBuilder mutationRequest(String method) {
        if (method.equals("POST")) {
            return post(RECOMMENDED_BOOKS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"isbn13\":\"" + ISBN13 + "\"}");
        }
        recommendedBookRepository.save(RecommendedBook.create(book.getId(), Instant.now()));
        return delete(RECOMMENDED_BOOKS_PATH + "/{bookId}", book.getId());
    }

    private int successStatus(String method) {
        if (method.equals("POST")) {
            return 201;
        }
        return 204;
    }

    private void assertRecommendationUnchanged(String method) {
        assertThat(recommendedBookRepository.existsByBookId(book.getId())).isEqualTo(method.equals("DELETE"));
    }
}
