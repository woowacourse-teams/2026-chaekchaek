package com.chaekchaek.auth.token.guest;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.guest-token")
public record GuestTokenProperties(Duration expiration, Duration refreshWindow) {

    public GuestTokenProperties {
        if (expiration == null || expiration.isZero() || expiration.isNegative()) {
            throw new IllegalArgumentException("Guest token expiration must be positive");
        }
        if (refreshWindow == null || refreshWindow.isZero() || refreshWindow.isNegative()
                || refreshWindow.compareTo(expiration) >= 0) {
            throw new IllegalArgumentException("Guest token refresh window must be positive and shorter than expiration");
        }
    }
}
