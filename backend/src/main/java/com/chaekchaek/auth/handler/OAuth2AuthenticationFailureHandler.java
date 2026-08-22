package com.chaekchaek.auth.handler;

import com.chaekchaek.auth.oauth.OAuthFrontendRedirectResolver;
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

    public OAuth2AuthenticationFailureHandler(OAuthFrontendRedirectResolver redirectResolver) {
        this.redirectResolver = redirectResolver;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException
    ) throws IOException, ServletException {
        response.sendRedirect(redirectResolver.resolveFailureUrl(request));
    }
}
