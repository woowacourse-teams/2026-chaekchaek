package com.chaekchaek.auth.service;

import com.chaekchaek.auth.dto.MobileAppleLoginRequest;
import com.chaekchaek.auth.dto.MobileTokenResponse;
import com.chaekchaek.auth.exception.InvalidAppleAuthorizationException;
import com.chaekchaek.auth.oauth.apple.AppleIdTokenVerifier;
import com.chaekchaek.auth.oauth.apple.AppleProfile;
import com.chaekchaek.auth.oauth.apple.AppleTokenClient;
import com.chaekchaek.auth.oauth.apple.AppleTokenResponse;
import com.chaekchaek.auth.token.access.AccessTokenProperties;
import com.chaekchaek.auth.token.dto.IssuedTokens;
import com.chaekchaek.auth.token.refresh.RefreshTokenProperties;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.socialaccount.domain.Provider;
import org.springframework.stereotype.Service;

@Service
public class MobileAppleLoginService {

    private final AppleIdTokenVerifier idTokenVerifier;
    private final AppleTokenClient tokenClient;
    private final SocialLoginService socialLoginService;
    private final AuthTokenService authTokenService;
    private final AccessTokenProperties accessTokenProperties;
    private final RefreshTokenProperties refreshTokenProperties;

    public MobileAppleLoginService(
            AppleIdTokenVerifier idTokenVerifier,
            AppleTokenClient tokenClient,
            SocialLoginService socialLoginService,
            AuthTokenService authTokenService,
            AccessTokenProperties accessTokenProperties,
            RefreshTokenProperties refreshTokenProperties
    ) {
        this.idTokenVerifier = idTokenVerifier;
        this.tokenClient = tokenClient;
        this.socialLoginService = socialLoginService;
        this.authTokenService = authTokenService;
        this.accessTokenProperties = accessTokenProperties;
        this.refreshTokenProperties = refreshTokenProperties;
    }

    public MobileTokenResponse login(MobileAppleLoginRequest request) {
        AppleProfile profile = idTokenVerifier.verify(request.identityToken(), request.nonce());
        AppleTokenResponse appleTokens = tokenClient.exchange(request.authorizationCode());
        AppleProfile exchangedProfile = idTokenVerifier.verify(appleTokens.idToken(), request.nonce());
        if (!profile.providerUserId().equals(exchangedProfile.providerUserId())) {
            throw new InvalidAppleAuthorizationException();
        }
        Member member = socialLoginService.loginOrSignUp(profile);
        socialLoginService.updateProviderRefreshToken(
                Provider.APPLE,
                profile.providerUserId(),
                appleTokens.refreshToken()
        );
        IssuedTokens tokens = authTokenService.issue(member.getId());
        return MobileTokenResponse.from(tokens, accessTokenProperties, refreshTokenProperties);
    }
}
