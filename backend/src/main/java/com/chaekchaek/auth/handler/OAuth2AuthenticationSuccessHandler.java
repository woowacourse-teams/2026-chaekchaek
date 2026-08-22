package com.chaekchaek.auth.handler;

import com.chaekchaek.auth.principal.AuthenticatedMember;
import com.chaekchaek.auth.oauth.OAuthFrontendRedirectResolver;
import com.chaekchaek.auth.service.AuthTokenService;
import com.chaekchaek.auth.token.cookie.AuthCookieProvider;
import com.chaekchaek.auth.token.dto.IssuedTokens;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final String AUTHENTICATED_MEMBER_MUST_EXIST_ERROR_MESSAGE =
            "[ERROR] 인증된 회원 정보가 존재해야 합니다";

    private final AuthTokenService authTokenService;
    private final AuthCookieProvider authCookieProvider;
    private final OAuthFrontendRedirectResolver redirectResolver;

    public OAuth2AuthenticationSuccessHandler(
            AuthTokenService authTokenService,
            AuthCookieProvider authCookieProvider,
            OAuthFrontendRedirectResolver redirectResolver
    ) {
        this.authTokenService = authTokenService;
        this.authCookieProvider = authCookieProvider;
        this.redirectResolver = redirectResolver;
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

        response.sendRedirect(redirectResolver.resolveSuccessUrl(request));
    }

    private AuthenticatedMember requireAuthenticatedMember(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof AuthenticatedMember principal)) {
            throw new IllegalStateException(AUTHENTICATED_MEMBER_MUST_EXIST_ERROR_MESSAGE);
        }

        return principal;
    }

}
