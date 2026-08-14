package com.chaekchaek.auth.dto;

import com.chaekchaek.auth.token.access.AccessTokenProperties;
import com.chaekchaek.auth.token.dto.IssuedTokens;
import com.chaekchaek.auth.token.refresh.RefreshTokenProperties;

public record MobileTokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn
) {

    private static final String TOKEN_TYPE = "Bearer";

    public static MobileTokenResponse from(
            IssuedTokens tokens,
            AccessTokenProperties accessTokenProperties,
            RefreshTokenProperties refreshTokenProperties
    ) {
        return new MobileTokenResponse(
                tokens.accessToken(),
                tokens.refreshToken().value(),
                TOKEN_TYPE,
                accessTokenProperties.expiration().toSeconds(),
                refreshTokenProperties.expiration().toSeconds()
        );
    }
}
