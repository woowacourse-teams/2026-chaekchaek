package com.chaekchaek.library.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.auth.token.access.AccessTokenProvider;
import com.chaekchaek.auth.token.cookie.AuthCookieProvider;
import com.chaekchaek.book.client.BookDetailItem;
import com.chaekchaek.book.client.fixture.Yes24MockServer;
import com.chaekchaek.book.client.fixture.Yes24ResponseFixture;
import com.chaekchaek.book.client.yes24.Yes24BookClient;
import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.library.domain.Library;
import com.chaekchaek.library.domain.LibraryBook;
import com.chaekchaek.library.domain.LibraryItem;
import com.chaekchaek.library.domain.ReadingStatus;
import com.chaekchaek.library.dto.LibraryItemResponse;
import com.chaekchaek.library.repository.LibraryBookRepository;
import com.chaekchaek.library.repository.LibraryItemRepository;
import com.chaekchaek.library.repository.LibraryRepository;
import com.chaekchaek.member.domain.AccountStatus;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:isbn-rating;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IsbnRatingIntegrationTest {

    private static final Isbn13 ISBN13 = new Isbn13("9788937460449");
    private static final Instant NOW = Instant.parse("2026-08-14T00:00:00Z");
    private static final Yes24MockServer YES24 = startYes24();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AccessTokenProvider accessTokens;
    @Autowired
    private BookRepository books;
    @Autowired
    private MemberRepository members;
    @Autowired
    private LibraryItemRepository records;
    @Autowired
    private LibraryRepository libraries;
    @Autowired
    private LibraryBookRepository placements;
    @MockitoBean
    private Clock clock;
    @MockitoSpyBean
    private Yes24BookClient bookClient;

    private Member member;
    private String token;

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

    @BeforeEach
    void setUp() {
        member = members.saveAndFlush(Member.create("익명", null, LocalDateTime.now()));
        // 토큰 만료 검증은 실제 시계를 사용하므로 발급 시각만 현재 시각으로 맞춘다
        when(clock.instant()).thenReturn(Instant.now());
        token = accessTokens.issue(member);
        when(clock.instant()).thenReturn(NOW);
    }

    @AfterEach
    void cleanUp() {
        placements.deleteAllInBatch();
        records.deleteAllInBatch();
        libraries.deleteAllInBatch();
        books.deleteAllInBatch();
        members.deleteAllInBatch();
    }

    @AfterAll
    static void closeYes24() throws IOException {
        YES24.close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("Bearer와 쿠키 인증으로 미등록 도서의 별점만 저장한다")
    void should_CreateBookAndRatingWithoutReadingRecord_When_IsbnIsUnregistered(boolean cookie) throws Exception {
        // given
        YES24.응답한다(200, Yes24ResponseFixture.데미안_상세_결과());
        MockHttpServletRequestBuilder request = isbnRating("4.5");
        if (cookie) {
            request.cookie(new Cookie(AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME, token));
        } else {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }

        // when
        String body = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn13").value(ISBN13.value()))
                .andExpect(jsonPath("$.rating").value(4.5))
                .andExpect(jsonPath("$.status").value(nullValue()))
                .andExpect(jsonPath("$.currentPage").value(nullValue()))
                .andExpect(jsonPath("$.readingUpdatedAt").value(nullValue()))
                .andReturn().getResponse().getContentAsString();

        // then
        LibraryItemResponse response = objectMapper.readValue(body, LibraryItemResponse.class);
        assertThat(objectMapper.readTree(body).size()).isEqualTo(18);
        assertThat(books.findByIsbn13(ISBN13).orElseThrow().getId()).isEqualTo(response.bookId());
        assertThat(records.findByMemberIdAndBookId(member.getId(), response.bookId())).isPresent();
        assertThat(records.count()).isEqualTo(1L);
        assertThat(libraries.count()).isZero();
        assertThat(placements.count()).isZero();
        YES24.상세_요청을_검증한다(ISBN13.value());

        mockMvc.perform(authenticated(delete("/api/v1/library/{bookId}/rating", response.bookId())))
                .andExpect(status().isNoContent());
        assertThat(records.findByMemberIdAndBookId(member.getId(), response.bookId()).orElseThrow().getRating())
                .isNull();
    }

    @Test
    @DisplayName("기존 저장 도서는 ISBN 체크섬을 새 조건으로 추가하지 않고 같은 ID로 재사용한다")
    void should_ReuseStoredBookWithoutExternalLookup_When_StoredIsbnHasInvalidChecksum() throws Exception {
        // given
        Isbn13 legacyIsbn = new Isbn13("9780000000000");
        Book book = book(legacyIsbn);

        // when & then
        mockMvc.perform(authenticated(put("/api/v1/books/by-isbn/{isbn13}/rating", legacyIsbn.value())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":4.5}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookId").value(book.getId()));
        assertThat(books.count()).isEqualTo(1L);
        assertThat(records.count()).isEqualTo(1L);
        verifyNoInteractions(bookClient);
    }

    @ParameterizedTest
    @ValueSource(strings = {"NONE", "READING", "CLEARED"})
    @DisplayName("ISBN 경로와 ID 경로는 같은 응답과 별점 행을 공유하고 독서 기록을 보존한다")
    void should_PreserveResponseAndRecordAcrossRoutes_When_BookExists(String readingState) throws Exception {
        // given
        Book book = book();
        if (!readingState.equals("NONE")) {
            LibraryItem item = LibraryItem.create(member.getId(), book.getId(), ReadingStatus.READING, 240, NOW);
            item.changeCurrentPage(23, 240, NOW);
            if (readingState.equals("CLEARED")) {
                item.clearReadingState();
            }
            records.saveAndFlush(item);
        }

        // when
        String legacyBody = mockMvc.perform(authenticated(put("/api/v1/library/{bookId}/rating", book.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":4.5}")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long recordId = records.findByMemberIdAndBookId(member.getId(), book.getId()).orElseThrow().getId();
        String isbnBody = mockMvc.perform(authenticated(isbnRating("4.5")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        mockMvc.perform(authenticated(isbnRating("3.5"))).andExpect(status().isOk());
        mockMvc.perform(authenticated(put("/api/v1/library/{bookId}/rating", book.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":3.5}")))
                .andExpect(status().isOk());

        // then
        assertThat(objectMapper.readTree(isbnBody)).isEqualTo(objectMapper.readTree(legacyBody));
        LibraryItem item = records.findByMemberIdAndBookId(member.getId(), book.getId()).orElseThrow();
        assertThat(item.getId()).isEqualTo(recordId);
        assertThat(item.getRating()).isEqualByComparingTo("3.5");
        assertThat(item.getStatus()).isEqualTo(readingState.equals("READING") ? ReadingStatus.READING : null);
        assertThat(item.getCurrentPage()).isEqualTo(readingState.equals("READING") ? 23 : null);
        assertThat(records.count()).isEqualTo(1L);
        assertThat(placements.count()).isZero();
        assertThat(records.findRatingStatisticsByBookIdIn(List.of(book.getId()))).singleElement()
                .satisfies(statistics -> assertThat(statistics.getRatingCount()).isEqualTo(1L));
        verifyNoInteractions(bookClient);
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    @DisplayName("ISBN 별점 삭제는 내 별점만 지우고 독서 기록과 책장 배치를 유지한다")
    void should_RemoveOnlyMyRating_When_DeletingByIsbn(boolean cookie, boolean reading) throws Exception {
        // given
        Book book = book();
        LibraryItem item = LibraryItem.createForRating(member.getId(), book.getId(), new BigDecimal("4.5"), NOW);
        if (reading) {
            item = LibraryItem.create(member.getId(), book.getId(), ReadingStatus.READING, 240, NOW);
            item.changeCurrentPage(23, 240, NOW);
            item.rate(new BigDecimal("4.5"), NOW);
            Library library = libraries.saveAndFlush(Library.create(member.getId(), "읽는 책", NOW));
            placements.saveAndFlush(LibraryBook.create(library.getId(), book.getId(), 1L));
        }
        long recordId = records.saveAndFlush(item).getId();
        Member other = members.saveAndFlush(Member.create("다른 회원", null, LocalDateTime.ofInstant(NOW, ZoneOffset.UTC)));
        records.saveAndFlush(LibraryItem.createForRating(other.getId(), book.getId(), new BigDecimal("3.5"), NOW));
        MockHttpServletRequestBuilder request = delete("/api/v1/books/by-isbn/{isbn13}/rating", ISBN13.value());
        if (cookie) {
            request.cookie(new Cookie(AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME, token));
        } else {
            authenticated(request);
        }

        // when
        mockMvc.perform(request).andExpect(status().isNoContent());
        mockMvc.perform(authenticated(delete("/api/v1/books/by-isbn/{isbn13}/rating", ISBN13.value())))
                .andExpect(status().isNoContent());

        // then
        LibraryItem saved = records.findByMemberIdAndBookId(member.getId(), book.getId()).orElseThrow();
        assertThat(saved.getId()).isEqualTo(recordId);
        assertThat(saved.getRating()).isNull();
        assertThat(saved.getRatingUpdatedAt()).isNull();
        assertThat(saved.getStatus()).isEqualTo(reading ? ReadingStatus.READING : null);
        assertThat(saved.getCurrentPage()).isEqualTo(reading ? 23 : null);
        assertThat(saved.getReadingUpdatedAt()).isEqualTo(reading ? NOW : null);
        assertThat(records.findByMemberIdAndBookId(other.getId(), book.getId()).orElseThrow().getRating())
                .isEqualByComparingTo("3.5");
        assertThat(records.count()).isEqualTo(2L);
        assertThat(placements.count()).isEqualTo(reading ? 1L : 0L);
        assertThat(books.count()).isEqualTo(1L);
        assertThat(records.findRatingStatisticsByBookIdIn(List.of(book.getId()))).singleElement()
                .satisfies(statistics -> assertThat(statistics.getRatingCount()).isEqualTo(1L));
        verifyNoInteractions(bookClient);
    }

    @Test
    @DisplayName("ISBN 별점 삭제는 내 공유 기록이 없으면 새 기록 없이 성공한다")
    void should_ReturnNoContentWithoutCreatingRecord_When_DeletingWithoutRecord() throws Exception {
        // given
        book();

        // when & then
        mockMvc.perform(authenticated(delete("/api/v1/books/by-isbn/{isbn13}/rating", ISBN13.value())))
                .andExpect(status().isNoContent());
        assertThat(records.count()).isZero();
        assertThat(books.count()).isEqualTo(1L);
        verifyNoInteractions(bookClient);
    }

    @Test
    @DisplayName("미등록 ISBN 삭제는 외부 조회와 도서 생성 없이 도서 없음 오류를 반환한다")
    void should_ReturnNotFoundWithoutCreatingBook_When_DeletingUnregisteredIsbn() throws Exception {
        // when & then
        mockMvc.perform(authenticated(delete("/api/v1/books/by-isbn/{isbn13}/rating", ISBN13.value())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOK_NOT_FOUND"));
        assertThat(books.count()).isZero();
        assertThat(records.count()).isZero();
        verifyNoInteractions(bookClient);
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = {"SUSPENDED", "WITHDRAWN"})
    @DisplayName("비활성 회원의 ISBN 삭제 요청은 기존 별점을 보존한다")
    void should_PreserveRating_When_InactiveMemberDeletesByIsbn(AccountStatus accountStatus) throws Exception {
        // given
        Book book = book();
        records.saveAndFlush(LibraryItem.createForRating(member.getId(), book.getId(), new BigDecimal("4.5"), NOW));
        ReflectionTestUtils.setField(member, "accountStatus", accountStatus);
        members.saveAndFlush(member);

        // when & then
        mockMvc.perform(authenticated(delete("/api/v1/books/by-isbn/{isbn13}/rating", ISBN13.value())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        assertThat(records.findByMemberIdAndBookId(member.getId(), book.getId()).orElseThrow().getRating())
                .isEqualByComparingTo("4.5");
        verifyNoInteractions(bookClient);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "5.1", "4.55", "null"})
    @DisplayName("잘못된 별점은 도서 조회 전에 기존 API와 같은 오류로 거절한다")
    void should_RejectInvalidRatingBeforeLookup_When_RatingIsInvalid(String rating) throws Exception {
        // when & then
        mockMvc.perform(authenticated(isbnRating(rating)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        assertThat(books.count()).isZero();
        assertThat(records.count()).isZero();
        verifyNoInteractions(bookClient);
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = {"SUSPENDED", "WITHDRAWN"})
    @DisplayName("비활성 회원은 외부 도서 조회 전에 별점 저장을 거절한다")
    void should_RejectBeforeLookup_When_MemberIsInactive(AccountStatus accountStatus) throws Exception {
        // given
        ReflectionTestUtils.setField(member, "accountStatus", accountStatus);
        members.saveAndFlush(member);

        // when & then
        mockMvc.perform(authenticated(isbnRating("4.5")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        assertThat(books.count()).isZero();
        assertThat(records.count()).isZero();
        verifyNoInteractions(bookClient);
    }

    @Test
    @DisplayName("외부 조회 중 회원이 탈퇴하면 도서만 남고 별점은 저장하지 않는다")
    void should_RecheckMemberBeforeSaving_When_MemberWithdrawsDuringExternalLookup() throws Exception {
        // given
        YES24.응답한다(200, Yes24ResponseFixture.데미안_상세_결과());
        doAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            BookDetailItem detail = (BookDetailItem) invocation.callRealMethod();
            member.withdraw(LocalDateTime.now());
            members.saveAndFlush(member);
            return detail;
        }).when(bookClient).findBookByIsbn13(ISBN13);

        // when & then
        mockMvc.perform(authenticated(isbnRating("4.5")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        assertThat(books.findByIsbn13(ISBN13)).isPresent();
        assertThat(records.count()).isZero();
        assertThat(placements.count()).isZero();
        YES24.상세_요청을_검증한다(ISBN13.value());
    }

    @ParameterizedTest
    @ValueSource(ints = {404, 500})
    @DisplayName("외부 도서 없음과 외부 서버 오류를 구분하고 저장하지 않는다")
    void should_PreserveEmptyDatabase_When_ExternalLookupFails(int upstreamStatus) throws Exception {
        // given
        String errorCode = upstreamStatus == 404 ? "GOODS_002" : "SERVER_ERROR";
        YES24.응답한다(upstreamStatus, Yes24ResponseFixture.오류_응답(errorCode, "도서 조회 실패"));

        // when & then
        mockMvc.perform(authenticated(isbnRating("4.5")))
                .andExpect(status().is(upstreamStatus == 404 ? 404 : 502))
                .andExpect(jsonPath("$.code").value(upstreamStatus == 404 ? "BOOK_NOT_FOUND" : "EXTERNAL_API_ERROR"));
        assertThat(books.count()).isZero();
        assertThat(records.count()).isZero();
        YES24.상세_요청을_검증한다(ISBN13.value());
    }

    @ParameterizedTest
    @CsvSource({"NONE,false", "INVALID,false", "GUEST,false", "NONE,true", "INVALID,true", "GUEST,true"})
    @DisplayName("인증 실패는 ID 경로와 ISBN 경로에서 같은 상태와 오류 본문을 반환한다")
    void should_ReturnSameAuthenticationError_When_CredentialsAreInvalid(
            String credential,
            boolean deleting
    ) throws Exception {
        // given
        MockHttpServletRequestBuilder legacy = put("/api/v1/library/1/rating")
                .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":4.5}");
        MockHttpServletRequestBuilder isbn = isbnRating("4.5");
        if (deleting) {
            legacy = delete("/api/v1/library/1/rating");
            isbn = delete("/api/v1/books/by-isbn/{isbn13}/rating", ISBN13.value());
        }
        if (credential.equals("INVALID")) {
            legacy.header(HttpHeaders.AUTHORIZATION, "Bearer invalid");
            isbn.header(HttpHeaders.AUTHORIZATION, "Bearer invalid");
        }
        if (credential.equals("GUEST")) {
            String body = mockMvc.perform(post("/api/v1/auth/guest-token"))
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            String guestToken = objectMapper.readTree(body).get("guestToken").asText();
            legacy.header("X-Guest-Token", guestToken);
            isbn.header("X-Guest-Token", guestToken);
        }

        // when
        var legacyResponse = mockMvc.perform(legacy).andExpect(status().is4xxClientError()).andReturn().getResponse();
        var isbnResponse = mockMvc.perform(isbn).andExpect(status().is4xxClientError()).andReturn().getResponse();

        // then
        assertThat(isbnResponse.getStatus()).isEqualTo(legacyResponse.getStatus());
        assertThat(isbnResponse.getContentType()).isEqualTo(legacyResponse.getContentType());
        assertThat(isbnResponse.getContentAsString()).isEqualTo(legacyResponse.getContentAsString());
        assertThat(books.count()).isZero();
        assertThat(records.count()).isZero();
        verifyNoInteractions(bookClient);
    }

    private MockHttpServletRequestBuilder isbnRating(String rating) {
        return put("/api/v1/books/by-isbn/{isbn13}/rating", ISBN13.value())
                .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":" + rating + "}");
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private Book book() {
        return book(ISBN13);
    }

    private Book book(Isbn13 isbn13) {
        return books.saveAndFlush(Book.create(
                isbn13,
                "데미안",
                "https://example.com/cover.jpg",
                null,
                null,
                null,
                List.of("헤르만 헤세"),
                List.of(),
                "민음사",
                "국내도서",
                null,
                240,
                null,
                null
        ));
    }
}
