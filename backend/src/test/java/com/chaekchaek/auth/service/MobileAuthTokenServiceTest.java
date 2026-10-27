package com.chaekchaek.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chaekchaek.auth.dto.MobileTokenResponse;
import com.chaekchaek.auth.token.access.AccessTokenProperties;
import com.chaekchaek.auth.token.dto.IssuedTokens;
import com.chaekchaek.auth.token.refresh.IssuedRefreshToken;
import com.chaekchaek.auth.token.refresh.RefreshTokenProperties;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MobileAuthTokenServiceTest {

    @Mock
    private AuthTokenService authTokenService;

    private MobileAuthTokenService mobileAuthTokenService;

    @BeforeEach
    void setUp() {
        AccessTokenProperties accessProperties =
                new AccessTokenProperties(
                        "chaekchaek",
                        "test-secret",
                        Duration.ofMinutes(30)
                );

        RefreshTokenProperties refreshProperties =
                new RefreshTokenProperties(
                        Duration.ofDays(14)
                );

        mobileAuthTokenService =
                new MobileAuthTokenService(
                        authTokenService,
                        accessProperties,
                        refreshProperties
                );
    }

    @Test
    @DisplayName("Refresh Token으로 모바일 토큰을 재발급한다")
    void should_ReissueTokens_When_RefreshTokenIsValid() {
        // given
        IssuedRefreshToken refreshToken =
                new IssuedRefreshToken(
                        "new-refresh-token",
                        LocalDateTime.of(
                                2026,
                                8,
                                28,
                                0,
                                0
                        )
                );

        when(authTokenService.reissue(
                "old-refresh-token"
        )).thenReturn(new IssuedTokens(
                "new-access-token",
                refreshToken
        ));

        // when
        MobileTokenResponse response =
                mobileAuthTokenService.reissue(
                        "old-refresh-token"
                );

        // then
        MobileTokenResponse expected = new MobileTokenResponse(
                "new-access-token",
                "new-refresh-token",
                "Bearer",
                1_800,
                1_209_600
        );
        assertThat(response).isEqualTo(expected);

        verify(authTokenService)
                .reissue("old-refresh-token");
    }

    @Test
    @DisplayName("모바일 로그아웃 시 Refresh Token을 폐기한다")
    void should_Logout_When_RefreshTokenIsProvided() {
        // when
        mobileAuthTokenService.logout("refresh-token");

        // then
        verify(authTokenService).logout("refresh-token");
    }
}