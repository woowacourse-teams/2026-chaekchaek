package com.chaekchaek.auth.token.refresh;

import java.time.LocalDateTime;

public record IssuedRefreshToken(
        String value,
        LocalDateTime expiresAt
) {
}
