package com.chaekchaek.auth.token.dto;

import com.chaekchaek.auth.token.refresh.IssuedRefreshToken;

public record IssuedTokens(
        String accessToken,
        IssuedRefreshToken refreshToken
) {
}
