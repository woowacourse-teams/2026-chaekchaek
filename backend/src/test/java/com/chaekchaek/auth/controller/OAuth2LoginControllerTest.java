package com.chaekchaek.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.chaekchaek.auth.oauth.OAuthFrontendClient;
import com.chaekchaek.auth.oauth.OAuthFrontendRedirectResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class OAuth2LoginControllerTest {

    private final OAuthFrontendRedirectResolver redirectResolver =
            org.mockito.Mockito.mock(OAuthFrontendRedirectResolver.class);
    private final OAuth2LoginController controller =
            new OAuth2LoginController(redirectResolver);

    @Test
    @DisplayName("허용된 프론트 클라이언트를 저장하고 Google 로그인을 시작한다")
    void should_RememberClientAndRedirect_When_ClientIsAllowed() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<Void> response = controller.googleLogin("local", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation())
                .hasToString("/oauth2/authorization/google");
        verify(redirectResolver).rememberClient(request, OAuthFrontendClient.LOCAL);
    }

    @Test
    @DisplayName("허용되지 않은 프론트 클라이언트는 거부한다")
    void should_RejectLogin_When_ClientIsNotAllowed() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<Void> response = controller.googleLogin("attacker", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
