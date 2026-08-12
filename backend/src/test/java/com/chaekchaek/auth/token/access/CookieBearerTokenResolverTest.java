package com.chaekchaek.auth.token.access;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class CookieBearerTokenResolverTest {

    private final CookieBearerTokenResolver resolver = new CookieBearerTokenResolver();

    @Test
    @DisplayName("쿠키가 없으면 토큰을 반환하지 않는다")
    void should_NotReturnToken_When_NoCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(resolver.resolve(request)).isNull();
    }

    @Test
    @DisplayName("AccessToken 쿠키의 값을 반환한다")
    void should_Return_AccessTokenCookie() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie("JSESSIONID", "session"),
                new Cookie("access_token", "access-value"),
                new Cookie("refresh_token", "refresh-value")
        );

        // when & then
        assertThat(resolver.resolve(request)).isEqualTo("access-value");
    }
}
