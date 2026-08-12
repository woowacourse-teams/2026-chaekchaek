package com.chaekchaek.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

public class AuthCookieProviderTest {

    @Test
    @DisplayName("AccessToken 쿠키를 생성한다")
    void should_CreateAccessTokenCookie() {
        // given
        AuthCookieProvider provider = new AuthCookieProvider(
                new AccessTokenProperties(
                        "chaekchaek",
                        "unused",
                        Duration.ofMinutes(30)
                ),
                new RefreshTokenProperties(
                        Duration.ofDays(14)
                ),
                new AuthCookieProperties(false, "Lax")
        );

        // when
        ResponseCookie cookie = provider.createAccessTokenCookie("access-token");

        // then
        assertAll(
                () -> assertThat(cookie.getName()).isEqualTo("access_token"),
                () -> assertThat(cookie.getValue()).isEqualTo("access-token"),
                () -> assertThat(cookie.isHttpOnly()).isTrue(),
                () -> assertThat(cookie.isSecure()).isFalse(),
                () -> assertThat(cookie.getPath()).isEqualTo("/"),
                () -> assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofMinutes(30)),
                () -> assertThat(cookie.toString()).contains("SameSite=Lax")
        );
    }

    @Test
    @DisplayName("Access Token 삭제 쿠키를 생성한다")
    void deleteAccessTokenCookie() {
        // given
        AuthCookieProvider provider = new AuthCookieProvider(
                new AccessTokenProperties(
                        "chaekchaek",
                        "unused",
                        Duration.ofMinutes(30)
                ),
                new RefreshTokenProperties(
                        Duration.ofDays(14)
                ),
                new AuthCookieProperties(false, "Lax")
        );

        // when
        ResponseCookie cookie = provider.deleteAccessTokenCookie();

        // then
        assertAll(
                () -> assertThat(cookie.getName()).isEqualTo("access_token"),
                () -> assertThat(cookie.getValue()).isEmpty(),
                () -> assertThat(cookie.getPath()).isEqualTo("/"),
                () -> assertThat(cookie.getMaxAge()).isZero(),
                () -> assertThat(cookie.isHttpOnly()).isTrue()
        );
    }

    @Test
    @DisplayName("Refresh Token 삭제 쿠키를 생성한다")
    void should_Create_DeleteRefreshTokenCookie() {
        // given
        AuthCookieProvider provider = new AuthCookieProvider(
                new AccessTokenProperties(
                        "chaekchaek",
                        "unused",
                        Duration.ofMinutes(30)
                ),
                new RefreshTokenProperties(
                        Duration.ofDays(14)
                ),
                new AuthCookieProperties(false, "Lax")
        );

        // when
        ResponseCookie cookie = provider.deleteRefreshTokenCookie();

        // then
        assertAll(
                () -> assertThat(cookie.getName()).isEqualTo("refresh_token"),
                () -> assertThat(cookie.getValue()).isEmpty(),
                () -> assertThat(cookie.getPath()).isEqualTo("/api/v1/auth"),
                () -> assertThat(cookie.getMaxAge()).isZero(),
                () -> assertThat(cookie.isHttpOnly()).isTrue()
        );
    }
}
