package com.chaekchaek.book.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.auth.token.access.AccessTokenProvider;
import com.chaekchaek.auth.token.cookie.AuthCookieProvider;
import com.chaekchaek.book.client.fixture.Yes24MockServer;
import com.chaekchaek.book.client.fixture.Yes24ResponseFixture;
import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.library.domain.LibraryItem;
import com.chaekchaek.library.domain.ReadingStatus;
import com.chaekchaek.library.repository.LibraryBookRepository;
import com.chaekchaek.library.repository.LibraryItemRepository;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:book-detail-v2;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BookDetailV2IntegrationTest {

    private static final String ISBN = "9788937460449";
    private static final String V1 = "/api/v1/books/by-isbn/" + ISBN;
    private static final String V2 = "/api/v2/books/by-isbn/" + ISBN;
    private static final Instant NOW = Instant.parse("2026-10-26T10:00:00Z");
    private static final LocalDateTime REGISTERED_AT = LocalDateTime.of(2026, 10, 26, 10, 0);
    private static final Yes24MockServer YES24 = startYes24();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private BookRepository books;
    @Autowired
    private MemberRepository members;
    @Autowired
    private LibraryItemRepository records;
    @Autowired
    private LibraryBookRepository placements;
    @Autowired
    private AccessTokenProvider accessTokens;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("yes24.base-url", YES24::baseUrl);
    }

    private static Yes24MockServer startYes24() {
        try {
            return new Yes24MockServer();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @AfterAll
    static void closeYes24() throws IOException {
        YES24.close();
    }

    @ParameterizedTest
    @CsvSource({"false, false", "false, true", "true, false", "true, true"})
    @DisplayName("독서 기록과 별점 조합에 따라 v1은 유지하고 v2는 내 별점만 반환한다")
    void should_PreserveLegacyAndReturnRatingOnly_When_ReadingAndRatingVary(boolean reading, boolean rating)
            throws Exception {
        // given
        Book book = book();
        Member member = members.saveAndFlush(Member.create("회원", null, REGISTERED_AT));
        if (reading || rating) {
            LibraryItem record = LibraryItem.create(
                    member.getId(),
                    book.getId(),
                    ReadingStatus.READING,
                    240,
                    NOW
            );
            if (reading) {
                record.changeCurrentPage(120, 240, NOW);
            }
            if (!reading) {
                record.clearReadingState();
            }
            if (rating) {
                record.rate(new BigDecimal("4.5"), NOW);
            }
            records.saveAndFlush(record);
        }
        String token = accessTokens.issue(member);

        // when
        ObjectNode legacy = response(authenticated(V1, token));
        ObjectNode current = response(authenticated(V2, token));

        // then
        assertThat(current.has("myRecord")).isTrue();
        assertThat(current.has("myRating")).isFalse();
        if (rating) {
            assertThat(current.get("myRecord").size()).isEqualTo(1);
            assertThat(current.at("/myRecord/myRating").decimalValue()).isEqualByComparingTo("4.5");
        } else {
            assertThat(current.get("myRecord").isNull()).isTrue();
        }
        if (reading || rating) {
            assertThat(legacy.get("myRecord").size()).isEqualTo(3);
            assertThat(legacy.at("/myRecord/status").isNull()).isEqualTo(!reading);
            if (reading) {
                assertThat(legacy.at("/myRecord/currentPage").asInt()).isEqualTo(120);
            }
        } else {
            assertThat(legacy.get("myRecord").isNull()).isTrue();
        }
        legacy.remove("myRecord");
        current.remove("myRecord");
        assertThat(current).isEqualTo(legacy);
    }

    @Test
    @DisplayName("별점을 삭제하면 독서 기록은 v1에 남고 v2의 myRecord는 null이 된다")
    void should_ReturnNullRecord_When_RatingIsRemoved() throws Exception {
        // given
        Book book = book();
        Member member = members.saveAndFlush(Member.create("회원", null, REGISTERED_AT));
        LibraryItem record = LibraryItem.create(member.getId(), book.getId(), ReadingStatus.READING, 240, NOW);
        record.rate(new BigDecimal("4.5"), NOW);
        record.removeRating();
        records.saveAndFlush(record);
        String token = accessTokens.issue(member);

        // when & then
        assertThat(response(authenticated(V2, token)).get("myRecord").isNull()).isTrue();
        assertThat(response(authenticated(V1, token)).at("/myRecord/status").asText()).isEqualTo("READING");
    }

    @Test
    @DisplayName("비로그인 상태에서 v2 상세를 조회하고 개인화 정보는 null로 반환한다")
    void should_ReturnPublicDetail_When_NoTokenIsProvided() throws Exception {
        // given
        book();

        // when
        ObjectNode current = response(get(V2));

        // then
        assertThat(current).isEqualTo(response(get(V1)));
        assertThat(current.get("myRecord").isNull()).isTrue();
        assertThat(current.get("myRatingCount").isNull()).isTrue();
        assertThat(current.get("isRegisteredInMyLibrary").isNull()).isTrue();
    }

    @Test
    @DisplayName("Access Token 쿠키로 v2 상세의 내 별점을 조회한다")
    void should_ReturnPersonalRating_When_CookieIsProvided() throws Exception {
        // given
        Book book = book();
        Member member = members.saveAndFlush(Member.create("회원", null, REGISTERED_AT));
        LibraryItem record = LibraryItem.createForRating(
                member.getId(),
                book.getId(),
                new BigDecimal("3.5"),
                NOW
        );
        records.saveAndFlush(record);
        String token = accessTokens.issue(member);

        // when
        JsonNode current = response(get(V2).cookie(new Cookie(AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME, token)));

        // then
        assertThat(current.at("/myRecord/myRating").decimalValue()).isEqualByComparingTo("3.5");
        assertThat(current.get("isRegisteredInMyLibrary").asBoolean()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"BEARER", "COOKIE", "WITHDRAWN"})
    @DisplayName("잘못된 인증이나 탈퇴 회원은 공개 상세에서도 두 버전 모두 거부한다")
    void should_RejectInvalidCredentials_When_PublicDetailIsRequested(String credential) throws Exception {
        // given
        book();
        Member member = members.saveAndFlush(Member.create("회원", null, REGISTERED_AT));
        String token = "invalid-token";
        if (credential.equals("WITHDRAWN")) {
            token = accessTokens.issue(member);
            member.withdraw(REGISTERED_AT.plusDays(1));
            members.flush();
        }

        // when & then
        for (String path : List.of(V1, V2)) {
            MockHttpServletRequestBuilder request = authenticated(path, token);
            if (credential.equals("COOKIE")) {
                request = get(path).cookie(new Cookie(AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME, token));
            }
            mockMvc.perform(request)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }
    }

    @Test
    @DisplayName("미등록 도서를 외부에서 조회해도 도서와 서재 기록을 저장하지 않는다")
    void should_NotPersistBook_When_ExternalDetailIsReturned() throws Exception {
        // given
        YES24.응답한다(200, Yes24ResponseFixture.데미안_상세_결과());

        // when
        JsonNode current = response(get(V2));

        // then
        assertThat(current.get("bookId").isNull()).isTrue();
        assertThat(current.get("myRecord").isNull()).isTrue();
        assertThat(books.count()).isZero();
        assertThat(records.count()).isZero();
        assertThat(placements.count()).isZero();
        YES24.상세_요청을_검증한다(ISBN);
    }

    @ParameterizedTest
    @CsvSource({"404, 404, BOOK_NOT_FOUND", "500, 502, EXTERNAL_API_ERROR"})
    @DisplayName("외부 조회 오류의 상태와 실제 v2 요청 경로를 반환한다")
    void should_ReturnProblemDetail_When_ExternalLookupFails(int upstream, int expected, String code)
            throws Exception {
        // given
        String upstreamCode = upstream == 404 ? "GOODS_002" : "SERVER_ERROR";
        YES24.응답한다(upstream, Yes24ResponseFixture.오류_응답(upstreamCode, "조회 실패"));

        // when & then
        mockMvc.perform(get(V2))
                .andExpect(status().is(expected))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.instance").value(V2));
        assertThat(books.count()).isZero();
        YES24.상세_요청을_검증한다(ISBN);
    }

    private ObjectNode response(MockHttpServletRequestBuilder request) throws Exception {
        String body = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return (ObjectNode) objectMapper.readTree(body);
    }

    private MockHttpServletRequestBuilder authenticated(String path, String token) {
        return get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private Book book() {
        return books.saveAndFlush(Book.create(
                new Isbn13(ISBN),
                "데미안",
                "https://example.com/cover.jpg",
                null,
                null,
                "책 설명",
                List.of("헤르만 헤세"),
                List.of("번역가"),
                "민음사",
                "소설",
                LocalDate.of(2020, 1, 1),
                240,
                "https://www.yes24.com/product/goods/1",
                "yes24"
        ));
    }
}
