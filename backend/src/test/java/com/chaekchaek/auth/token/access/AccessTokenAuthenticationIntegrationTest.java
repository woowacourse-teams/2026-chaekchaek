package com.chaekchaek.auth.token.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.auth.token.cookie.AuthCookieProvider;
import com.chaekchaek.common.auth.CurrentMemberIdProvider;
import com.chaekchaek.library.domain.Library;
import com.chaekchaek.library.domain.LibraryBook;
import com.chaekchaek.library.repository.LibraryBookRepository;
import com.chaekchaek.library.repository.LibraryRepository;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.LocalDateTime;
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
        memberRepository.findById(memberId).orElseThrow().updateNickname("읽는회원");
        String token = issueAccessToken(memberId.toString());

        // when & then
        mockMvc.perform(post("/api/v1/libraries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"올해읽을책"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.libraryId").isNumber())
                .andExpect(jsonPath("$.name").value("올해읽을책"))
                .andExpect(jsonPath("$.isDefault").value(false))
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.items").isEmpty());
        assertThat(libraryRepository.findAll())
                .filteredOn(library -> library.isOwnedBy(memberId))
                .singleElement()
                .satisfies(library -> assertThat(library.getName()).isEqualTo("올해읽을책"));
    }

    @Test
    @DisplayName("Bearer 토큰으로 인증된 회원이 자신이 소유한 일반 책장을 삭제한다")
    void should_DeleteOwnedLibrary_When_BearerTokenIsValid() throws Exception {
        // given
        String token = issueAccessToken(memberId.toString());
        Library library = libraryRepository.create(Library.create(memberId, "삭제책장", Instant.now()));

        // when & then
        mockMvc.perform(delete("/api/v1/libraries/{libraryId}", library.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(libraryRepository.findById(library.getId())).isEmpty();
    }

    @Test
    @DisplayName("인증 정보가 없으면 책장을 삭제할 수 없다")
    void should_RejectLibraryDeletion_When_AuthenticationIsMissing() throws Exception {
        // given
        Library library = libraryRepository.create(Library.create(memberId, "보존책장", Instant.now()));

        // when & then
        mockMvc.perform(delete("/api/v1/libraries/{libraryId}", library.getId()).with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        assertThat(libraryRepository.findById(library.getId())).isPresent();
    }

    @Test
    @DisplayName("Access Token 쿠키로 인증된 회원의 서재를 생성한다")
    void should_CreateLibraryForMember_When_AccessTokenCookieIsValid() throws Exception {
        // given
        memberRepository.findById(memberId).orElseThrow().updateNickname("읽는회원");
        String token = issueAccessToken(memberId.toString());

        // when & then
        mockMvc.perform(post("/api/v1/libraries")
                        .cookie(new Cookie(AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME, token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"쿠키책장"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.libraryId").isNumber())
                .andExpect(jsonPath("$.name").value("쿠키책장"))
                .andExpect(jsonPath("$.isDefault").value(false))
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.items").isEmpty());
        assertThat(libraryRepository.findAll())
                .filteredOn(library -> library.isOwnedBy(memberId))
                .singleElement()
                .satisfies(library -> assertThat(library.getName()).isEqualTo("쿠키책장"));
    }

    @Test
    @DisplayName("Bearer 토큰으로 기본 책장 이름을 바꾸고 책장 속성을 유지한다")
    void should_RenameDefaultLibrary_When_BearerTokenIsValid() throws Exception {
        // given
        memberRepository.findById(memberId).orElseThrow().updateNickname("읽는회원");
        Library library = libraryRepository.create(
                Library.createDefault(memberId, "기본책장", 4L, Instant.parse("2026-10-01T00:00:00Z"))
        );
        LibraryBook placement = libraryBookRepository.saveAndFlush(
                LibraryBook.create(library.getId(), 42L, 3L)
        );
        String token = issueAccessToken(memberId.toString());

        // when & then
        mockMvc.perform(patch("/api/v1/libraries/{libraryId}", library.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"읽을책"}
                                """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        Library saved = libraryRepository.findById(library.getId()).orElseThrow();
        assertThat(saved.getName()).isEqualTo("읽을책");
        assertThat(saved.isDefault()).isTrue();
        assertThat(saved.getDisplayOrder()).isEqualTo(4L);
        assertThat(saved.getCreatedAt()).isEqualTo(Instant.parse("2026-10-01T00:00:00Z"));
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(memberRepository.findById(memberId).orElseThrow().getNickname()).isEqualTo("읽는회원");
        assertThat(libraryBookRepository.findAllByLibraryId(library.getId()))
                .singleElement()
                .satisfies(savedPlacement -> {
                    assertThat(savedPlacement.getId()).isEqualTo(placement.getId());
                    assertThat(savedPlacement.getBookId()).isEqualTo(42L);
                    assertThat(savedPlacement.getDisplayOrder()).isEqualTo(3L);
                });
    }

    @Test
    @DisplayName("CSRF 토큰 없이도 인증 쿠키로 일반 책장 이름을 변경한다")
    void should_RenameLibrary_When_AccessTokenCookieIsValid() throws Exception {
        // given
        Library library = libraryRepository.create(Library.create(memberId, "가을책장", Instant.now()));
        String token = issueAccessToken(memberId.toString());

        // when & then
        mockMvc.perform(patch("/api/v1/libraries/{libraryId}", library.getId())
                        .cookie(new Cookie(AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME, token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"읽을책"}
                                """))
                .andExpect(status().isNoContent());

        assertThat(libraryRepository.findById(library.getId()).orElseThrow().getName()).isEqualTo("읽을책");
    }

    @Test
    @DisplayName("탈퇴 회원은 인증된 요청이어도 책장 이름을 변경할 수 없다")
    void should_RejectLibraryRename_When_MemberIsWithdrawn() throws Exception {
        // given
        Library library = libraryRepository.create(Library.create(memberId, "가을책장", Instant.now()));
        String token = issueAccessToken(memberId.toString());
        memberRepository.findById(memberId).orElseThrow().withdraw(LocalDateTime.now());

        // when & then
        mockMvc.perform(patch("/api/v1/libraries/{libraryId}", library.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"읽을책"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        assertThat(libraryRepository.findById(library.getId()).orElseThrow().getName()).isEqualTo("가을책장");
    }

    @Test
    @DisplayName("인증 정보가 없으면 책장 이름을 변경할 수 없다")
    void should_RejectLibraryRename_When_AuthenticationIsMissing() throws Exception {
        // given
        Library library = libraryRepository.create(Library.create(memberId, "가을책장", Instant.now()));

        // when & then
        mockMvc.perform(patch("/api/v1/libraries/{libraryId}", library.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"읽을책"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        assertThat(libraryRepository.findById(library.getId()).orElseThrow().getName()).isEqualTo("가을책장");
    }

    @Test
    @DisplayName("닉네임이 없는 회원은 일반 책장을 생성할 수 없다")
    void should_RequireNickname_When_CreatingLibrary() throws Exception {
        String token = issueAccessToken(memberId.toString());

        mockMvc.perform(post("/api/v1/libraries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"올해읽을책"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NICKNAME_REQUIRED"));
        assertThat(libraryRepository.findAll()).filteredOn(library -> library.isOwnedBy(memberId)).isEmpty();
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
    @DisplayName("탈퇴 회원은 인증된 쓰기 요청이어도 일반 책장을 생성할 수 없다")
    void should_RejectLibraryCreation_When_MemberIsWithdrawn() throws Exception {
        String token = issueAccessToken(memberId.toString());
        memberRepository.findById(memberId).orElseThrow().withdraw(LocalDateTime.now());

        // when & then
        mockMvc.perform(post("/api/v1/libraries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"올해읽을책"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
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
