package com.chaekchaek.auth.token.access;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.access-token")
public record AccessTokenProperties(
        String issuer,
        String secret,
        Duration expiration
) {
}
