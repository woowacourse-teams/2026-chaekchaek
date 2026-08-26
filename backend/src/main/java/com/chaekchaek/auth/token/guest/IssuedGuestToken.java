package com.chaekchaek.auth.token.guest;

import java.time.LocalDateTime;

public record IssuedGuestToken(String value, String nickname, LocalDateTime expiresAt) {
}
