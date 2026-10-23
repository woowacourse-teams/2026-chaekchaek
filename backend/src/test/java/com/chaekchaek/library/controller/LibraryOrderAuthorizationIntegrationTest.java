package com.chaekchaek.library.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.auth.token.access.AccessTokenProvider;
import com.chaekchaek.auth.token.cookie.AuthCookieProvider;
import com.chaekchaek.library.domain.Library;
import com.chaekchaek.library.repository.LibraryRepository;
import com.chaekchaek.member.domain.AccountStatus;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LibraryOrderAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MemberRepository members;
    @Autowired
    private LibraryRepository libraries;
    @Autowired
    private AccessTokenProvider accessTokens;
    private Member member;
    private Library library;

    @BeforeEach
    void setUp() {
        member = members.saveAndFlush(Member.create("순서인증", null, LocalDateTime.now()));
        library = libraries.saveAndFlush(Library.create(member.getId(), "책장", Instant.now()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("실제 Bearer 토큰으로 별도 CSRF 토큰 없이 순서를 변경한다")
    void should_ReorderWithoutCsrf_When_BearerTokenIsValid(boolean books) throws Exception {
        // when & then
        mockMvc.perform(request(books).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokens.issue(member)))
                .andExpect(status().isNoContent());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("실제 인증 쿠키로 별도 CSRF 토큰 없이 순서를 변경한다")
    void should_ReorderWithoutCsrf_When_AccessTokenCookieIsValid(boolean books) throws Exception {
        // when & then
        mockMvc.perform(request(books).cookie(new Cookie(
                        AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME,
                        accessTokens.issue(member)
                )))
                .andExpect(status().isNoContent());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("무인증 순서 변경은 인증 오류 본문을 반환한다")
    void should_ReturnUnauthorized_When_NoTokenIsProvided(boolean books) throws Exception {
        // when & then
        mockMvc.perform(request(books))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("정지된 회원은 순서 변경 권한 오류를 반환한다")
    void should_ReturnForbidden_When_MemberIsSuspended(boolean books) throws Exception {
        // given
        String token = accessTokens.issue(member);
        ReflectionTestUtils.setField(member, "accountStatus", AccountStatus.SUSPENDED);
        members.saveAndFlush(member);

        // when & then
        mockMvc.perform(request(books).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private MockHttpServletRequestBuilder request(boolean books) {
        if (books) {
            return patch("/api/v1/libraries/{libraryId}/books/order", library.getId())
                    .contentType(MediaType.APPLICATION_JSON).content("{\"bookIds\":[]}");
        }
        return patch("/api/v1/libraries/order").contentType(MediaType.APPLICATION_JSON)
                .content("{\"libraryIds\":[" + library.getId() + "]}");
    }
}
