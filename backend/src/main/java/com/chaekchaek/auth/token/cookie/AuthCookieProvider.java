package com.chaekchaek.auth.token.cookie;

import com.chaekchaek.auth.token.access.AccessTokenProperties;
import com.chaekchaek.auth.token.refresh.RefreshTokenProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieProvider {

    public static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    private final AccessTokenProperties accessTokenProperties;
    private final RefreshTokenProperties refreshTokenProperties;
    private final AuthCookieProperties authCookieProperties;

    public AuthCookieProvider(
            AccessTokenProperties accessTokenProperties,
            RefreshTokenProperties refreshTokenProperties,
            AuthCookieProperties authCookieProperties
    ) {
        this.accessTokenProperties = accessTokenProperties;
        this.refreshTokenProperties = refreshTokenProperties;
        this.authCookieProperties = authCookieProperties;
    }

    public ResponseCookie createAccessTokenCookie(String accessToken) {
        return ResponseCookie
                .from(ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .httpOnly(true)
                .secure(authCookieProperties.secure())
                .sameSite(authCookieProperties.sameSite())
                .path("/")
                .maxAge(accessTokenProperties.expiration())
                .build();
    }

    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie
                .from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(authCookieProperties.secure())
                .sameSite(authCookieProperties.sameSite())
                .path("/api/v1/auth")
                .maxAge(refreshTokenProperties.expiration())
                .build();
    }

    public ResponseCookie deleteAccessTokenCookie() {
        return ResponseCookie
                .from(ACCESS_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(authCookieProperties.secure())
                .sameSite(authCookieProperties.sameSite())
                .path("/")
                .maxAge(0)
                .build();
    }

    public ResponseCookie deleteRefreshTokenCookie() {
        return ResponseCookie
                .from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(authCookieProperties.secure())
                .sameSite(authCookieProperties.sameSite())
                .path("/api/v1/auth")
                .maxAge(0)
                .build();
    }
}
