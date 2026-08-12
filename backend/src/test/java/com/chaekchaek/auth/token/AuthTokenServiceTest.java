package com.chaekchaek.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AuthTokenServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AccessTokenProvider accessTokenProvider;

    @Mock
    private RefreshTokenProvider refreshTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RefreshTokenHasher refreshTokenHasher;

    @Mock
    private Clock clock;

    @InjectMocks
    private AuthTokenService authTokenService;

    @Test
    @DisplayName("회원에게 AccessToken과 RefreshToken을 발급한다")
    void should_IssueAccessTokenAndRefreshToken_When_Member() {
        // given
        Member member = mock(Member.class);

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));
        when(accessTokenProvider.issue(member))
                .thenReturn("access-token");

        IssuedRefreshToken refreshToken =
                new IssuedRefreshToken(
                        "refresh-token",
                        LocalDateTime.of(2026, 8, 26, 12, 0)
                );

        when(refreshTokenProvider.issue(member))
                .thenReturn(refreshToken);

        // when
        IssuedTokens result = authTokenService.issue(1L);

        // then
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isSameAs(refreshToken);

        verify(accessTokenProvider).issue(member);
        verify(refreshTokenProvider).issue(member);
    }

    @Test
    @DisplayName("유효한 Refresh Token으로 토큰을 재발급한다")
    void should_SuccessReissue() {
        // given
        String oldTokenValue = "old-refresh-token";
        String oldTokenHash = "old-refresh-token-hash";

        Member member = mock(Member.class);
        RefreshToken savedToken = mock(RefreshToken.class);

        Instant now = Instant.parse("2026-08-13T00:00:00Z");
        LocalDateTime nowDateTime =
                LocalDateTime.ofInstant(now, ZoneOffset.UTC);

        when(clock.instant()).thenReturn(now);
        when(refreshTokenHasher.hash(oldTokenValue))
                .thenReturn(oldTokenHash);
        when(refreshTokenRepository.findByTokenHash(oldTokenHash))
                .thenReturn(Optional.of(savedToken));
        when(savedToken.isUsable(nowDateTime)).thenReturn(true);
        when(savedToken.getMember()).thenReturn(member);

        when(accessTokenProvider.issue(member))
                .thenReturn("new-access-token");

        IssuedRefreshToken newRefreshToken =
                new IssuedRefreshToken(
                        "new-refresh-token",
                        nowDateTime.plusDays(14)
                );

        when(refreshTokenProvider.issue(member))
                .thenReturn(newRefreshToken);

        // when
        IssuedTokens result =
                authTokenService.reissue(oldTokenValue);

        // then
        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isSameAs(newRefreshToken);

        verify(savedToken).revoke(nowDateTime);
        verify(accessTokenProvider).issue(member);
        verify(refreshTokenProvider).issue(member);
    }

    @Test
    @DisplayName("존재하지 않는 Refresh Token 재발급은 거부한다")
    void should_RejectReissue_When_RefreshTokenNotFound() {
        when(refreshTokenHasher.hash("invalid-token"))
                .thenReturn("invalid-hash");
        when(refreshTokenRepository.findByTokenHash("invalid-hash"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> authTokenService.reissue("invalid-token")
        ).isInstanceOf(InvalidRefreshTokenException.class);

        verifyNoInteractions(
                accessTokenProvider,
                refreshTokenProvider
        );
    }

    @Test
    @DisplayName("폐기되거나 만료된 Refresh Token 재발급은 거부한다")
    void should_RejectReissue_When_UnusableToken() {
        RefreshToken savedToken = mock(RefreshToken.class);
        Instant now = Instant.parse("2026-08-13T00:00:00Z");
        LocalDateTime nowDateTime =
                LocalDateTime.ofInstant(now, ZoneOffset.UTC);

        when(refreshTokenHasher.hash("expired-token"))
                .thenReturn("expired-hash");
        when(refreshTokenRepository.findByTokenHash("expired-hash"))
                .thenReturn(Optional.of(savedToken));
        when(clock.instant()).thenReturn(now);
        when(savedToken.isUsable(nowDateTime)).thenReturn(false);

        assertThatThrownBy(
                () -> authTokenService.reissue("expired-token")
        ).isInstanceOf(InvalidRefreshTokenException.class);

        verifyNoInteractions(
                accessTokenProvider,
                refreshTokenProvider
        );
    }
}
