package com.chaekchaek.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chaekchaek.auth.dto.MobileAppleLoginRequest;
import com.chaekchaek.auth.dto.MobileTokenResponse;
import com.chaekchaek.auth.oauth.apple.AppleIdTokenVerifier;
import com.chaekchaek.auth.oauth.apple.AppleProfile;
import com.chaekchaek.auth.oauth.apple.AppleTokenClient;
import com.chaekchaek.auth.oauth.apple.AppleTokenResponse;
import com.chaekchaek.auth.token.access.AccessTokenProperties;
import com.chaekchaek.auth.token.dto.IssuedTokens;
import com.chaekchaek.auth.token.refresh.IssuedRefreshToken;
import com.chaekchaek.auth.token.refresh.RefreshTokenProperties;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.socialaccount.domain.Provider;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MobileAppleLoginServiceTest {

    @Mock AppleIdTokenVerifier verifier;
    @Mock AppleTokenClient tokenClient;
    @Mock SocialLoginService socialLoginService;
    @Mock AuthTokenService authTokenService;

    private MobileAppleLoginService service;

    @BeforeEach
    void setUp() {
        service = new MobileAppleLoginService(
                verifier, tokenClient, socialLoginService, authTokenService,
                new AccessTokenProperties("issuer", "secret", Duration.ofMinutes(30)),
                new RefreshTokenProperties(Duration.ofDays(14))
        );
    }

    @Test
    void should_LoginAndStoreAppleRefreshToken() {
        MobileAppleLoginRequest request = new MobileAppleLoginRequest("id-token", "code", "nonce");
        AppleProfile profile = new AppleProfile("apple-user");
        Member member = Member.create("우아한 달빛 참새", null, LocalDateTime.now());
        AppleTokenResponse appleTokens = new AppleTokenResponse(
                "apple-access", "apple-refresh", "apple-id", "Bearer", 300L
        );
        IssuedTokens issuedTokens = new IssuedTokens(
                "access", new IssuedRefreshToken("refresh", LocalDateTime.now().plusDays(14))
        );
        when(verifier.verify("id-token", "nonce")).thenReturn(profile);
        when(tokenClient.exchange("code")).thenReturn(appleTokens);
        when(verifier.verify("apple-id", "nonce")).thenReturn(profile);
        when(socialLoginService.loginOrSignUp(profile)).thenReturn(member);
        when(authTokenService.issue(member.getId())).thenReturn(issuedTokens);

        MobileTokenResponse response = service.login(request);

        assertThat(response.accessToken()).isEqualTo("access");
        verify(socialLoginService).updateProviderRefreshToken(
                Provider.APPLE, "apple-user", "apple-refresh"
        );
    }
}
