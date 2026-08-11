package com.chaekchaek.auth.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final String successRedirectUrl;

    public OAuth2AuthenticationSuccessHandler(
            @Value("${app.frontend.base-url}") String frontendBaseUrl,
            @Value("${app.frontend.oauth-success-path}") String successPath
    ) {
        this.successRedirectUrl = frontendBaseUrl + successPath;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        response.sendRedirect(successRedirectUrl);
    }

}
