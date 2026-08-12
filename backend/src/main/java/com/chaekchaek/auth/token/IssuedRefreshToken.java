package com.chaekchaek.auth.token;

import java.time.LocalDateTime;

public record IssuedRefreshToken(
        String value,
        LocalDateTime expiresAt
) {
}
