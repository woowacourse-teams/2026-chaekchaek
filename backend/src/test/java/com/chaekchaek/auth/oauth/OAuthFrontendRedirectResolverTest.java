package com.chaekchaek.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class OAuthFrontendRedirectResolverTest {

    private final OAuthFrontendRedirectResolver resolver =
            new OAuthFrontendRedirectResolver(
                    "https://chaekchaek.com",
                    "http://localhost:3000",
                    "http://43.203.240.201",
                    "/oauth/callback",
                    "/login"
            );

    @Test
    @DisplayName("로컬에서 시작한 OAuth 로그인은 로컬 프론트로 이동한다")
    void should_ResolveLocalSuccessUrl_When_ClientIsLocal() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        resolver.rememberClient(request, OAuthFrontendClient.LOCAL);

        String redirectUrl = resolver.resolveSuccessUrl(request);

        assertThat(redirectUrl).isEqualTo("http://localhost:3000/oauth/callback");
    }

    @Test
    @DisplayName("개발 서버에서 시작한 OAuth 로그인은 개발 프론트로 이동한다")
    void should_ResolveDevSuccessUrl_When_ClientIsDev() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        resolver.rememberClient(request, OAuthFrontendClient.DEV);

        String redirectUrl = resolver.resolveSuccessUrl(request);

        assertThat(redirectUrl).isEqualTo("http://43.203.240.201/oauth/callback");
    }

    @Test
    @DisplayName("OAuth 로그인 실패도 시작한 프론트의 로그인 화면으로 이동한다")
    void should_ResolveFailureUrl_When_ClientExists() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        resolver.rememberClient(request, OAuthFrontendClient.DEV);

        String redirectUrl = resolver.resolveFailureUrl(request);

        assertThat(redirectUrl).isEqualTo(
                "http://43.203.240.201/login?error=OAUTH_LOGIN_FAILED"
        );
    }

    @Test
    @DisplayName("저장된 클라이언트가 없으면 기존 기본 프론트 주소를 사용한다")
    void should_ResolveDefaultUrl_When_ClientDoesNotExist() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        String redirectUrl = resolver.resolveSuccessUrl(request);

        assertThat(redirectUrl).isEqualTo("https://chaekchaek.com/oauth/callback");
    }

    @Test
    @DisplayName("리다이렉트 주소를 결정한 후 저장된 클라이언트를 제거한다")
    void should_ClearClient_AfterResolvingUrl() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        resolver.rememberClient(request, OAuthFrontendClient.LOCAL);

        resolver.resolveSuccessUrl(request);
        String secondRedirectUrl = resolver.resolveSuccessUrl(request);

        assertThat(secondRedirectUrl).isEqualTo("https://chaekchaek.com/oauth/callback");
    }
}
