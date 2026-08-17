package com.chaekchaek.auth.token.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.chaekchaek.auth.token.cookie.AuthCookieProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

public class HeaderOrCookieBearerTokenResolverTest {

    private final HeaderOrCookieBearerTokenResolver resolver = new HeaderOrCookieBearerTokenResolver();

    @Test
    @DisplayName("쿠키가 없으면 토큰을 반환하지 않는다")
    void should_ReturnNull_When_CookieDoesNotExist() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(resolver.resolve(request)).isNull();
    }

    @Test
    @DisplayName("AccessToken 쿠키의 값을 반환한다")
    void should_ReturnAccessToken_When_AccessTokenCookieExists() {
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

    @Test
    @DisplayName("Authorization Header에서 Bearer Token을 반환한다")
    void should_ReturnToken_When_BearerHeaderExists() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer header-access-token"
        );

        // when
        String token = resolver.resolve(request);

        // then
        assertThat(token).isEqualTo("header-access-token");
    }

    @Test
    @DisplayName("Authorization Header가 없으면 쿠키의 Access Token을 반환한다")
    void should_ReturnCookieToken_When_BearerHeaderDoesNotExist() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setCookies(
                new Cookie(
                        AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME,
                        "cookie-access-token"
                )
        );

        // when
        String token = resolver.resolve(request);

        // then
        assertThat(token).isEqualTo("cookie-access-token");
    }

    @Test
    @DisplayName("헤더와 쿠키에 토큰이 있으면 Bearer Header를 우선한다")
    void should_ReturnHeaderToken_When_HeaderAndCookieExist() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer header-access-token"
        );

        request.setCookies(
                new Cookie(
                        AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME,
                        "cookie-access-token"
                )
        );

        // when
        String token = resolver.resolve(request);

        // then
        assertThat(token).isEqualTo("header-access-token");
    }

    @Test
    @DisplayName("Authorization Header와 쿠키가 없으면 토큰을 반환하지 않는다")
    void should_ReturnNull_When_HeaderAndCookieDoNotExist() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(resolver.resolve(request)).isNull();
    }

    @Test
    @DisplayName("Bearer 형식이 아닌 Authorization Header는 인증에 사용하지 않는다")
    void should_ReturnCookieToken_When_AuthorizationSchemeIsNotBearer() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Basic credentials"
        );

        request.setCookies(
                new Cookie(
                        AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME,
                        "cookie-access-token"
                )
        );

        // when
        String token = resolver.resolve(request);

        // then
        assertThat(token).isEqualTo("cookie-access-token");
    }

    @Test
    @DisplayName("형식이 잘못된 Bearer Header는 거부한다")
    void should_ThrowException_When_BearerHeaderIsMalformed() {
        // given
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer"
        );

        request.setCookies(
                new Cookie(
                        AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME,
                        "cookie-access-token"
                )
        );

        // when & then
        assertThatThrownBy(() -> resolver.resolve(request))
                .isInstanceOf(
                        OAuth2AuthenticationException.class
                );
    }
}
