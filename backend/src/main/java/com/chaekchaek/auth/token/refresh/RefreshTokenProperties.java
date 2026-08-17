package com.chaekchaek.auth.token.refresh;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.refresh-token")
public record RefreshTokenProperties(
        Duration expiration
) {
}
