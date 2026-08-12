package com.chaekchaek.auth.controller;

import com.chaekchaek.auth.token.AuthCookieProvider;
import com.chaekchaek.auth.token.AuthTokenService;
import com.chaekchaek.auth.token.IssuedTokens;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthTokenService authTokenService;
    private final AuthCookieProvider authCookieProvider;

    public AuthController(
            AuthTokenService authTokenService,
            AuthCookieProvider authCookieProvider
    ) {
        this.authTokenService = authTokenService;
        this.authCookieProvider = authCookieProvider;
    }

    @PostMapping("/reissue")
    public ResponseEntity<Void> reissue(
            @CookieValue(
                    name = AuthCookieProvider.REFRESH_TOKEN_COOKIE_NAME,
                    required = false
            )
            String refreshToken
    ) {
        IssuedTokens tokens = authTokenService.reissue(refreshToken);

        ResponseCookie accessCookie = authCookieProvider.createAccessTokenCookie(
                tokens.accessToken());

        ResponseCookie refreshCookie = authCookieProvider.createRefreshTokenCookie(
                tokens.refreshToken().value()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.add(
            HttpHeaders.SET_COOKIE,
                accessCookie.toString()
        );
        headers.add(
                HttpHeaders.SET_COOKIE,
                refreshCookie.toString()
        );

        return new ResponseEntity<>(
                headers,
                HttpStatus.NO_CONTENT
        );
    }
}
