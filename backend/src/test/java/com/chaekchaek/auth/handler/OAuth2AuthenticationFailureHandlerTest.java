package com.chaekchaek.auth.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

class OAuth2AuthenticationFailureHandlerTest {

    @Test
    @DisplayName("OAuth2 로그인에 실패하면 에러 코드와 함께 로그인 페이지로 이동한다")
    void should_RedirectWithErrorCode_When_OAuth2LoginFails()
            throws Exception {
        // given
        OAuth2AuthenticationFailureHandler handler =
                new OAuth2AuthenticationFailureHandler(
                        "http://localhost:3000",
                        "/login"
                );
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException exception =
                mock(AuthenticationException.class);

        // when
        handler.onAuthenticationFailure(request, response, exception);

        // then
        assertThat(response.getRedirectedUrl())
                .isEqualTo(
                        "http://localhost:3000/login"
                                + "?error=OAUTH_LOGIN_FAILED"
                );
    }
}
