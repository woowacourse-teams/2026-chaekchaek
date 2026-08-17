package com.chaekchaek.auth.service;

import com.chaekchaek.auth.dto.MobileTokenResponse;
import com.chaekchaek.auth.oauth.google.GoogleIdTokenVerifier;
import com.chaekchaek.auth.oauth.google.GoogleProfile;
import com.chaekchaek.auth.token.access.AccessTokenProperties;
import com.chaekchaek.auth.token.dto.IssuedTokens;
import com.chaekchaek.auth.token.refresh.RefreshTokenProperties;
import com.chaekchaek.member.domain.Member;
import org.springframework.stereotype.Service;

@Service
public class MobileGoogleLoginService {

    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final SocialLoginService socialLoginService;
    private final AuthTokenService authTokenService;
    private final AccessTokenProperties accessTokenProperties;
    private final RefreshTokenProperties refreshTokenProperties;

    public MobileGoogleLoginService(
            GoogleIdTokenVerifier googleIdTokenVerifier,
            SocialLoginService socialLoginService,
            AuthTokenService authTokenService,
            AccessTokenProperties accessTokenProperties,
            RefreshTokenProperties refreshTokenProperties
    ) {
        this.googleIdTokenVerifier = googleIdTokenVerifier;
        this.socialLoginService = socialLoginService;
        this.authTokenService = authTokenService;
        this.accessTokenProperties = accessTokenProperties;
        this.refreshTokenProperties = refreshTokenProperties;
    }

    public MobileTokenResponse login(String idToken) {
        GoogleProfile googleProfile = googleIdTokenVerifier.verify(idToken);

        Member member = socialLoginService.loginOrSignUp(googleProfile);

        IssuedTokens tokens = authTokenService.issue(member.getId());

        return MobileTokenResponse.from(
                tokens,
                accessTokenProperties,
                refreshTokenProperties
        );
    }
}
