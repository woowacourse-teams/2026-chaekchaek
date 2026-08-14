package com.chaekchaek.auth.service;

import com.chaekchaek.auth.dto.MobileTokenResponse;
import com.chaekchaek.auth.token.access.AccessTokenProperties;
import com.chaekchaek.auth.token.dto.IssuedTokens;
import com.chaekchaek.auth.token.refresh.RefreshTokenProperties;
import org.springframework.stereotype.Service;

@Service
public class MobileAuthTokenService {

    private final AuthTokenService authTokenService;
    private final AccessTokenProperties accessTokenProperties;
    private final RefreshTokenProperties refreshTokenProperties;

    public MobileAuthTokenService(
            AuthTokenService authTokenService,
            AccessTokenProperties accessTokenProperties,
            RefreshTokenProperties refreshTokenProperties
    ) {
        this.authTokenService = authTokenService;
        this.accessTokenProperties = accessTokenProperties;
        this.refreshTokenProperties = refreshTokenProperties;
    }

    public MobileTokenResponse reissue(String refreshToken) {
        IssuedTokens tokens = authTokenService.reissue(refreshToken);

        return MobileTokenResponse.from(
                tokens,
                accessTokenProperties,
                refreshTokenProperties
        );
    }

    public void logout(String refreshToken) {
        authTokenService.logout(refreshToken);
    }
}
