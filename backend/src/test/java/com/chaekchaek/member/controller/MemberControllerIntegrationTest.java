package com.chaekchaek.member.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.auth.token.AccessTokenProvider;
import com.chaekchaek.auth.token.AuthCookieProvider;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.domain.MemberType;
import com.chaekchaek.member.repository.MemberRepository;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class MemberControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AccessTokenProvider accessTokenProvider;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("AccessToken 쿠키로 내 정보를 조회한다")
    void should_GetMyInfo() throws Exception {
        // given
        Member member = memberRepository.save(
                Member.create(
                        MemberType.MEMBER,
                        "약간 우아한 참새",
                        "exUrl",
                        LocalDateTime.of(2026, 8, 13, 12, 0)
                )
        );

        String accessToken = accessTokenProvider.issue(member);

        Cookie cookie = new Cookie(
                AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME,
                accessToken
        );

        // when && then
        mockMvc.perform(get("/api/v1/members/me")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(member.getId()))
                .andExpect(jsonPath("$.memberType").value("MEMBER"))
                .andExpect(jsonPath("$.nickname").value("약간 우아한 참새"))
                .andExpect(jsonPath("$.profileImageUrl")
                        .value("exUrl"))
                .andExpect(jsonPath("$.displayAnonymous").value(false))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));
    }

    @Test
    @DisplayName("AccessToken 쿠키가 없으면 내 정보를 조회가 거부된다")
    void should_RejectGetMyInfo_WithoutAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("변조된 Access Token으로 내 정보를 조회할 시 조회가 거부된다")
    void should_RejectGetMyInfo_WithTamperedAccessToken() throws Exception {
        Cookie cookie = new Cookie(
                AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME,
                "invalid.access.token"
        );

        mockMvc.perform(get("/api/v1/members/me")
                        .cookie(cookie))
                .andExpect(status().isUnauthorized());
    }
}
