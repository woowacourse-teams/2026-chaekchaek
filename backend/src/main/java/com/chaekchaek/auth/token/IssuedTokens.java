package com.chaekchaek.auth.token;

public record IssuedTokens(
        String accessToken,
        IssuedRefreshToken refreshToken
) {
}
