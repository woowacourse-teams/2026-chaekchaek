package com.chaekchaek.auth.controller;

import com.chaekchaek.auth.oauth.OAuthFrontendClient;
import com.chaekchaek.auth.oauth.OAuthFrontendRedirectResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/oauth2")
public class OAuth2LoginController {

    private static final URI GOOGLE_AUTHORIZATION_URI =
            URI.create("/oauth2/authorization/google");

    private final OAuthFrontendRedirectResolver redirectResolver;

    public OAuth2LoginController(OAuthFrontendRedirectResolver redirectResolver) {
        this.redirectResolver = redirectResolver;
    }

    @GetMapping("/google")
    public ResponseEntity<Void> googleLogin(
            @RequestParam String client,
            HttpServletRequest request
    ) {
        OAuthFrontendClient frontendClient = OAuthFrontendClient.from(client)
                .orElse(null);

        if (frontendClient == null) {
            return ResponseEntity.badRequest().build();
        }

        redirectResolver.rememberClient(request, frontendClient);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(GOOGLE_AUTHORIZATION_URI)
                .build();
    }
}
