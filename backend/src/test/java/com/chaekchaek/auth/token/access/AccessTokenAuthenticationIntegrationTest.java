package com.chaekchaek.auth.token.access;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.auth.token.cookie.AuthCookieProvider;
import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.common.auth.CurrentMemberIdProvider;
import com.chaekchaek.library.domain.Library;
import com.chaekchaek.library.domain.ReadingStatus;
import com.chaekchaek.library.repository.LibraryBookRepository;
import com.chaekchaek.library.repository.LibraryItemRepository;
import com.chaekchaek.library.repository.LibraryRepository;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@ActiveProfiles("test")
@Import(AccessTokenAuthenticationIntegrationTest.ProtectedTestController.class)
@Transactional
class AccessTokenAuthenticationIntegrationTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private LibraryBookRepository libraryBookRepository;

    @Autowired
    private LibraryItemRepository libraryItemRepository;

    @Autowired
    private BookRepository bookRepository;

    private Long memberId;

    @Test
    @DisplayName("탈퇴 전에 발급된 Bearer 토큰은 탈퇴 직후 거부한다")
    void should_RejectExistingAccessToken_When_MemberWithdraws() throws Exception {
        // given
        String token = issueAccessToken(memberId.toString());
        memberRepository.findById(memberId).orElseThrow().withdraw(LocalDateTime.now());

        // when & then
        mockMvc.perform(get("/test/protected").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @BeforeEach
    void createActiveMember() {
        memberId = memberRepository.save(Member.create(
                "토큰 검증 회원",
                null,
                LocalDateTime.now()
        )).getId();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    @DisplayName("Bearer 토큰으로 인증된 회원의 서재를 생성한다")
    void should_CreateLibraryForMember_When_BearerTokenIsValid() throws Exception {
        // given
        String token = issueAccessToken(memberId.toString());

        // when & then
        mockMvc.perform(post("/api/v1/libraries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"  가을에 읽을 책  "}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.libraryId").isNumber())
                .andExpect(jsonPath("$.name").value("가을에 읽을 책"));
        assertThat(libraryRepository.findAll())
                .filteredOn(library -> library.isOwnedBy(memberId))
                .singleElement()
                .satisfies(library -> assertThat(library.getName()).isEqualTo("가을에 읽을 책"));
    }

    @Test
    @DisplayName("Bearer 토큰으로 인증된 회원은 자신이 소유한 책장에 도서를 배치한다")
    void should_AddBookToOwnedLibrary_When_BearerTokenIsValid() throws Exception {
        // given
        String token = issueAccessToken(memberId.toString());
        Library library = libraryRepository.create(Library.create(memberId, "가을에 읽을 책", Instant.now()));
        Book book = bookRepository.saveAndFlush(Book.create(
                new Isbn13("9788936433598"),
                "책 제목",
                "https://example.com/cover.jpg",
                null,
                null,
                null,
                List.of("작가"),
                List.of(),
                "출판사",
                "분류",
                null,
                368,
                null,
                null
        ));

        // when & then
        mockMvc.perform(post("/api/v1/libraries/{libraryId}", library.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isbn13":"9788936433598","status":"READING","totalPages":368}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookId").value(book.getId()))
                .andExpect(jsonPath("$.status").value("READING"))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(header().exists(HttpHeaders.LOCATION));

        assertThat(libraryBookRepository.findAll())
                .filteredOn(placement -> placement.getLibraryId() == library.getId())
                .singleElement()
                .satisfies(placement -> assertThat(placement.getBookId()).isEqualTo(book.getId()));
        assertThat(libraryItemRepository.findByMemberIdAndBookId(memberId, book.getId()))
                .get()
                .satisfies(item -> assertThat(item.getStatus()).isEqualTo(ReadingStatus.READING));
    }

    @Test
    @DisplayName("인증 정보가 없으면 책장 도서 배치를 추가할 수 없다")
    void should_RejectBookPlacement_When_AuthenticationIsMissing() throws Exception {
        // given
        Library library = libraryRepository.create(Library.create(memberId, "가을에 읽을 책", Instant.now()));

        // when & then
        mockMvc.perform(post("/api/v1/libraries/{libraryId}", library.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isbn13":"9788936433598","status":"READING"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Access Token 쿠키로 인증된 회원의 서재를 생성한다")
    void should_CreateLibraryForMember_When_AccessTokenCookieIsValid() throws Exception {
        // given
        String token = issueAccessToken(memberId.toString());

        // when & then
        mockMvc.perform(post("/api/v1/libraries")
                        .cookie(new Cookie(AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME, token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"쿠키로 만든 서재"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.libraryId").isNumber())
                .andExpect(jsonPath("$.name").value("쿠키로 만든 서재"));
        assertThat(libraryRepository.findAll())
                .filteredOn(library -> library.isOwnedBy(memberId))
                .singleElement()
                .satisfies(library -> assertThat(library.getName()).isEqualTo("쿠키로 만든 서재"));
    }

    @Test
    @DisplayName("인증 정보가 없으면 서재를 생성할 수 없다")
    void should_RejectLibraryCreation_When_AuthenticationIsMissing() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/libraries")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"가을에 읽을 책"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("인증 토큰과 CSRF 토큰이 모두 없으면 서재 생성을 CSRF 검사에서 거부한다")
    void should_RejectLibraryCreation_When_AuthenticationAndCsrfAreMissing() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/libraries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"가을에 읽을 책"}
                                """))
                .andExpect(status().isForbidden())
                .andDo(document(
                        "library-create-forbidden",
                        resource(ResourceSnippetParameters.builder()
                                .summary("서재 생성")
                                .tag("내 서재")
                                .build())
                ));
    }

    @Test
    @DisplayName("유효한 Access Token 쿠키로 보호 API에 접근한다")
    void should_AccessProtectedApi_When_AccessTokenCookieIsValid() throws Exception {
        // given
        String accessToken = issueAccessToken(memberId.toString());

        // when & then
        mockMvc.perform(get("/test/protected")
                        .cookie(new Cookie(
                                AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME,
                                accessToken
                        )))
                .andExpect(status().isOk())
                .andExpect(content().string(memberId.toString()));
    }

    @Test
    @DisplayName("Access Token 쿠키가 없으면 보호 API 접근을 거부한다")
    void should_RejectProtectedApi_When_AccessTokenCookieIsMissing() throws Exception {
        // when & then
        mockMvc.perform(get("/test/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("변조된 Access Token 쿠키로 보호 API에 접근하면 거부한다")
    void should_RejectProtectedApi_When_AccessTokenCookieIsTampered() throws Exception {
        // given
        String tamperedToken = issueAccessToken("1") + "tampered";

        // when & then
        mockMvc.perform(get("/test/protected")
                        .cookie(new Cookie(
                                AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME,
                                tamperedToken
                        )))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("유효한 Bearer Access Token으로 보호 API에 접근한다")
    void should_AccessProtectedApi_When_BearerTokenIsValid()
            throws Exception {
        // given
        String accessToken = issueAccessToken(memberId.toString());

        // when & then
        mockMvc.perform(get("/test/protected")
                        .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + accessToken
        ))
                .andExpect(status().isOk())
                .andExpect(content().string(memberId.toString()));
    }

    @Test
    @DisplayName("변조된 Bearer Access Token으로 보호 API에 접근하면 거부한다")
    void should_RejectProtectedApi_When_BearerTokenIsTampered()
            throws Exception {
        // given
        String tamperedToken =
                issueAccessToken("1") + "tampered";

        // when & then
        mockMvc.perform(get("/test/protected")
                        .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + tamperedToken
        ))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code")
                        .value("UNAUTHORIZED"));
    }

    private String issueAccessToken(String memberId) {
        Instant issuedAt = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("chaekchaek")
                .subject(memberId)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(1_800))
                .claim("memberType", "MEMBER")
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }

    @RestController
    static class ProtectedTestController {

        private final CurrentMemberIdProvider currentMemberIdProvider;

        ProtectedTestController(CurrentMemberIdProvider currentMemberIdProvider) {
            this.currentMemberIdProvider = currentMemberIdProvider;
        }

        @GetMapping("/test/protected")
        String protectedApi() {
            return Long.toString(currentMemberIdProvider.getCurrentMemberId());
        }
    }
}
