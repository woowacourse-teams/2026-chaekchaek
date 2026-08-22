package com.chaekchaek.auth.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chaekchaek.auth.principal.AuthenticatedMember;
import com.chaekchaek.auth.oauth.OAuthFrontendRedirectResolver;
import com.chaekchaek.auth.token.cookie.AuthCookieProvider;
import com.chaekchaek.auth.service.AuthTokenService;
import com.chaekchaek.auth.token.refresh.IssuedRefreshToken;
import com.chaekchaek.auth.token.dto.IssuedTokens;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
public class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private AuthTokenService authTokenService;

    @Mock
    private AuthCookieProvider authCookieProvider;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuthFrontendRedirectResolver redirectResolver;

    private OAuth2AuthenticationSuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2AuthenticationSuccessHandler(
                authTokenService,
                authCookieProvider,
                redirectResolver
        );
    }

    @Test
    @DisplayName("로그인 성공 시 토큰 쿠키를 발급하고 리다이렉트한다")
    void should_RedirectAndIssueTokenCookie_When_LoginSuccess()
            throws Exception {
        // given
        AuthenticatedMember principal =
                mock(AuthenticatedMember.class);

        when(principal.getMemberId()).thenReturn(1L);
        when(authentication.getPrincipal())
                .thenReturn(principal);

        IssuedRefreshToken refreshToken =
                new IssuedRefreshToken(
                        "refresh-token",
                        LocalDateTime.now().plusDays(14)
                );

        when(authTokenService.issue(1L))
                .thenReturn(new IssuedTokens(
                        "access-token",
                        refreshToken
                ));

        when(authCookieProvider.createAccessTokenCookie(
                "access-token"
        )).thenReturn(ResponseCookie
                .from("access_token", "access-token")
                .httpOnly(true)
                .build());

        when(authCookieProvider.createRefreshTokenCookie(
                "refresh-token"
        )).thenReturn(ResponseCookie
                .from("refresh_token", "refresh-token")
                .httpOnly(true)
                .build());

        MockHttpServletRequest request =
                new MockHttpServletRequest();
        MockHttpServletResponse response =
                new MockHttpServletResponse();
        when(redirectResolver.resolveSuccessUrl(request))
                .thenReturn("http://localhost:3000/oauth/callback");

        // when
        handler.onAuthenticationSuccess(
                request,
                response,
                authentication
        );

        // then
        assertThat(response.getRedirectedUrl())
                .isEqualTo(
                        "http://localhost:3000/oauth/callback"
                );

        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE))
                .hasSize(2);
    }
}
