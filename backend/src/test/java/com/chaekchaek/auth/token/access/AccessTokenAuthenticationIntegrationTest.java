package com.chaekchaek.auth.token.access;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.auth.token.cookie.AuthCookieProvider;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AccessTokenAuthenticationIntegrationTest.ProtectedTestController.class)
class AccessTokenAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    @DisplayName("유효한 Access Token 쿠키로 보호 API에 접근한다")
    void should_Access_ProtectedApi_With_ValidAccessTokenCookie() throws Exception {
        String accessToken = issueAccessToken("1");

        mockMvc.perform(get("/test/protected")
                        .cookie(new Cookie(
                                AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME,
                                accessToken
                        )))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }

    @Test
    @DisplayName("Access Token 쿠키가 없으면 보호 API 접근을 거부한다")
    void should_Reject_ProtectedApi_Without_AccessTokenCookie() throws Exception {
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
    void should_Reject_ProtectedApi_With_TamperedAccessTokenCookie() throws Exception {
        String tamperedToken = issueAccessToken("1") + "tampered";

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
        String accessToken = issueAccessToken("1");

        // when & then
        mockMvc.perform(get("/test/protected")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + accessToken
                        ))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }

    @Test
    @DisplayName("변조된 Bearer Access Token으로 보호 API에 접근하면 거부한다")
    void should_RejectProtectedApi_When_BearerTokenIsTampered()
            throws Exception {
        String tamperedToken =
                issueAccessToken("1") + "tampered";

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

        @GetMapping("/test/protected")
        String protectedApi(@AuthenticationPrincipal Jwt jwt) {
            return jwt.getSubject();
        }
    }
}
