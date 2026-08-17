package com.chaekchaek.auth.oauth.google;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.google-id-token")
public record GoogleIdTokenProperties(
        String issuer,
        String jwkSetUri,
        String audience
) {
}
