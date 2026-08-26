package com.chaekchaek.auth.token.guest;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.guest-token")
public record GuestTokenProperties(Duration expiration) {

    public GuestTokenProperties {
        if (expiration == null || expiration.isZero() || expiration.isNegative()) {
            throw new IllegalArgumentException("Guest token expiration must be positive");
        }
    }
}
