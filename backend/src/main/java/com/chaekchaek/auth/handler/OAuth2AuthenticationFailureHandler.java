package com.chaekchaek.auth.handler;

import com.chaekchaek.auth.oauth.OAuthFrontendRedirectResolver;
import com.chaekchaek.auth.oauth.OAuthGuestContextService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final OAuthFrontendRedirectResolver redirectResolver;
    private final OAuthGuestContextService guestContextService;

    public OAuth2AuthenticationFailureHandler(
            OAuthFrontendRedirectResolver redirectResolver,
            OAuthGuestContextService guestContextService
    ) {
        this.redirectResolver = redirectResolver;
        this.guestContextService = guestContextService;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException
    ) throws IOException, ServletException {
        guestContextService.clear(request);
        response.sendRedirect(redirectResolver.resolveFailureUrl(request));
    }
}
