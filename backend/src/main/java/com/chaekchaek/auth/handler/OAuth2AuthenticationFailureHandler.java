package com.chaekchaek.auth.handler;

import com.chaekchaek.auth.oauth.OAuthFrontendRedirectResolver;
import com.chaekchaek.auth.oauth.OAuthGuestContextService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);

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
        log.warn(
                "OAuth2 login failed: exception={}, message={}",
                authenticationException.getClass().getSimpleName(),
                authenticationException.getMessage()
        );
        guestContextService.clear(request);
        response.sendRedirect(redirectResolver.resolveFailureUrl(request));
    }
}
