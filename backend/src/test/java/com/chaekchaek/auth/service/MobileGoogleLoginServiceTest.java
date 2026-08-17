package com.chaekchaek.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chaekchaek.auth.dto.MobileTokenResponse;
import com.chaekchaek.auth.oauth.google.GoogleIdTokenVerifier;
import com.chaekchaek.auth.oauth.google.GoogleProfile;
import com.chaekchaek.auth.token.access.AccessTokenProperties;
import com.chaekchaek.auth.token.dto.IssuedTokens;
import com.chaekchaek.auth.token.refresh.IssuedRefreshToken;
import com.chaekchaek.auth.token.refresh.RefreshTokenProperties;
import com.chaekchaek.member.domain.Member;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MobileGoogleLoginServiceTest {

    @Mock
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    @Mock
    private SocialLoginService socialLoginService;

    @Mock
    private AuthTokenService authTokenService;

    @Mock
    private Member member;

    private MobileGoogleLoginService mobileGoogleLoginService;

    @BeforeEach
    void setUp() {
        AccessTokenProperties accessTokenProperties =
                new AccessTokenProperties(
                        "chaekchaek",
                        "test-secret",
                        Duration.ofMinutes(30)
                );

        RefreshTokenProperties refreshTokenProperties =
                new RefreshTokenProperties(
                        Duration.ofDays(14)
                );

        mobileGoogleLoginService =
                new MobileGoogleLoginService(
                        googleIdTokenVerifier,
                        socialLoginService,
                        authTokenService,
                        accessTokenProperties,
                        refreshTokenProperties
                );
    }

    @Test
    @DisplayName("Google ID Token으로 로그인하고 자체 토큰을 발급한다")
    void should_IssueTokens_When_GoogleIdTokenIsValid() {
        // given
        GoogleProfile profile = new GoogleProfile(
                "google-user-id",
                "member@example.com",
                "exUrl"
        );

        IssuedRefreshToken refreshToken =
                new IssuedRefreshToken(
                        "refresh-token",
                        LocalDateTime.of(
                                2026, 8, 28, 0, 0
                        )
                );

        when(googleIdTokenVerifier.verify("google-id-token"))
                .thenReturn(profile);
        when(socialLoginService.loginOrSignUp(profile))
                .thenReturn(member);
        when(member.getId()).thenReturn(1L);
        when(authTokenService.issue(1L))
                .thenReturn(new IssuedTokens(
                        "access-token",
                        refreshToken
                ));

        // when
        MobileTokenResponse response =
                mobileGoogleLoginService.login(
                        "google-id-token"
                );

        // then
        assertAll(
                () -> assertThat(response.accessToken()).isEqualTo("access-token"),
                () -> assertThat(response.refreshToken()).isEqualTo("refresh-token"),
                () -> assertThat(response.tokenType()).isEqualTo("Bearer"),
                () -> assertThat(response.accessTokenExpiresIn()).isEqualTo(1_800),
                () -> assertThat(response.refreshTokenExpiresIn()).isEqualTo(1_209_600)
        );

        verify(googleIdTokenVerifier).verify("google-id-token");
        verify(socialLoginService).loginOrSignUp(profile);
        verify(authTokenService).issue(1L);
    }
}