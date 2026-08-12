package com.chaekchaek.auth.handler;

import com.chaekchaek.auth.principal.AuthenticatedMember;
import com.chaekchaek.auth.token.cookie.AuthCookieProvider;
import com.chaekchaek.auth.service.AuthTokenService;
import com.chaekchaek.auth.token.dto.IssuedTokens;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final String CANNOT_FIND_AUTHENTICATED_MEMBER_ERROR_MESSAGE = "인증된 회원 정보를 찾을 수 없습니다.";

    private final AuthTokenService authTokenService;
    private final AuthCookieProvider authCookieProvider;
    private final String successRedirectUrl;

    public OAuth2AuthenticationSuccessHandler(
            AuthTokenService authTokenService,
            AuthCookieProvider authCookieProvider,
            @Value("${app.frontend.base-url}") String frontendBaseUrl,
            @Value("${app.frontend.oauth-success-path}") String successPath
    ) {
        this.authTokenService = authTokenService;
        this.authCookieProvider = authCookieProvider;
        this.successRedirectUrl = frontendBaseUrl + successPath;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        AuthenticatedMember principal = requireAuthenticatedMember(authentication);
        IssuedTokens tokens = authTokenService.issue(principal.getMemberId());
        ResponseCookie accessCookie = authCookieProvider.createAccessTokenCookie(tokens.accessToken());
        ResponseCookie refreshCookie = authCookieProvider.createRefreshTokenCookie(tokens.refreshToken().value());

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                accessCookie.toString()
        );
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                refreshCookie.toString()
        );

        response.sendRedirect(successRedirectUrl);
    }

    private AuthenticatedMember requireAuthenticatedMember(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof AuthenticatedMember principal)) {
            throw new IllegalStateException(CANNOT_FIND_AUTHENTICATED_MEMBER_ERROR_MESSAGE);
        }

        return principal;
    }

}
