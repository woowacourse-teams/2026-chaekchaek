package com.chaekchaek.auth.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    private static final String LOGIN_FAILURE_CODE = "OAUTH_LOGIN_FAILED";

    private final String failureRedirectedUrl;

    public OAuth2AuthenticationFailureHandler(
            @Value("${app.frontend.base-url}") String frontendBaseUrl,
            @Value("${app.frontend.oauth-failure-path}") String failurePath
    ) {
        this.failureRedirectedUrl =
                        frontendBaseUrl
                        + failurePath
                        + "?error="
                        + LOGIN_FAILURE_CODE;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException
    ) throws IOException, ServletException {
        response.sendRedirect(failureRedirectedUrl);
    }
}
