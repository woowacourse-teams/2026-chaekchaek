package com.chaekchaek.auth.token;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.cookie")
public record AuthCookieProperties(
        boolean secure,
        String sameSite
) {
}
