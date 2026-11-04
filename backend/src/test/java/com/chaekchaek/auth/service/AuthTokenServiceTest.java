package com.chaekchaek.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.chaekchaek.auth.exception.InvalidRefreshTokenException;
import com.chaekchaek.auth.token.access.AccessTokenProvider;
import com.chaekchaek.auth.token.dto.IssuedTokens;
import com.chaekchaek.auth.token.refresh.IssuedRefreshToken;
import com.chaekchaek.auth.token.refresh.RefreshToken;
import com.chaekchaek.auth.token.refresh.RefreshTokenHasher;
import com.chaekchaek.auth.token.refresh.RefreshTokenProvider;
import com.chaekchaek.auth.token.refresh.RefreshTokenRepository;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.fixture.MemberFixture;
import com.chaekchaek.member.repository.MemberRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
public class AuthTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-13T00:00:00Z");
    private static final LocalDateTime NOW_DATE_TIME = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);

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

    private AuthTokenService authTokenService;

    @BeforeEach
    void setUp() {
        authTokenService = new AuthTokenService(
                memberRepository,
                accessTokenProvider,
                refreshTokenProvider,
                refreshTokenRepository,
                refreshTokenHasher,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("탈퇴 회원의 새 토큰 발급을 거부한다")
    void should_RejectTokenIssue_When_MemberIsWithdrawn() {
        // given
        Member member = MemberFixture.member();
        member.withdraw(NOW_DATE_TIME);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> authTokenService.issue(1L))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(accessTokenProvider, refreshTokenProvider);
    }

    @Test
    @DisplayName("탈퇴 회원은 남아 있는 유효 리프레시 토큰으로도 재발급할 수 없다")
    void should_RejectTokenReissue_When_MemberIsWithdrawn() {
        // given
        Member member = MemberFixture.member();
        member.withdraw(NOW_DATE_TIME);
        RefreshToken token = token(member, "hash");
        when(refreshTokenHasher.hash("token")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));

        // when & then
        assertThatThrownBy(() -> authTokenService.reissue("token"))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(accessTokenProvider, refreshTokenProvider);
    }

    @Test
    @DisplayName("회원에게 AccessToken과 RefreshToken을 발급한다")
    void should_IssueTokens_When_MemberExists() {
        // given
        Member member = MemberFixture.savedMember(1L);

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
    void should_ReissueTokens_When_RefreshTokenIsUsable() {
        // given
        String oldTokenValue = "old-refresh-token";
        String oldTokenHash = "old-refresh-token-hash";

        Member member = MemberFixture.savedMember(1L);
        RefreshToken savedToken = token(member, oldTokenHash);

        when(refreshTokenHasher.hash(oldTokenValue))
                .thenReturn(oldTokenHash);
        when(refreshTokenRepository.findByTokenHash(oldTokenHash))
                .thenReturn(Optional.of(savedToken));

        when(accessTokenProvider.issue(member))
                .thenReturn("new-access-token");

        IssuedRefreshToken newRefreshToken =
                new IssuedRefreshToken(
                        "new-refresh-token",
                        NOW_DATE_TIME.plusDays(14)
                );

        when(refreshTokenProvider.issue(member))
                .thenReturn(newRefreshToken);

        // when
        IssuedTokens result =
                authTokenService.reissue(oldTokenValue);

        // then
        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isSameAs(newRefreshToken);

        assertThat(savedToken.getRevokedAt()).isEqualTo(NOW_DATE_TIME);
        verify(accessTokenProvider).issue(member);
        verify(refreshTokenProvider).issue(member);
    }

    @Test
    @DisplayName("존재하지 않는 Refresh Token 재발급은 거부한다")
    void should_ThrowException_When_RefreshTokenDoesNotExist() {
        // given
        when(refreshTokenHasher.hash("invalid-token"))
                .thenReturn("invalid-hash");
        when(refreshTokenRepository.findByTokenHash("invalid-hash"))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(
                () -> authTokenService.reissue("invalid-token")
        ).isInstanceOf(InvalidRefreshTokenException.class);

        verifyNoInteractions(
                accessTokenProvider,
                refreshTokenProvider
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("폐기되거나 만료된 Refresh Token 재발급은 거부한다")
    void should_ThrowException_When_RefreshTokenIsUnusable(boolean expired) {
        // given
        RefreshToken savedToken;
        if (expired) {
            savedToken = RefreshToken.issue(
                    MemberFixture.member(),
                    "unusable-hash",
                    NOW_DATE_TIME.minusDays(14),
                    NOW_DATE_TIME
            );
        } else {
            savedToken = token(MemberFixture.member(), "unusable-hash");
            savedToken.revoke(NOW_DATE_TIME.minusSeconds(1));
        }

        when(refreshTokenHasher.hash("unusable-token"))
                .thenReturn("unusable-hash");
        when(refreshTokenRepository.findByTokenHash("unusable-hash"))
                .thenReturn(Optional.of(savedToken));

        // when & then
        assertThatThrownBy(
                () -> authTokenService.reissue("unusable-token")
        ).isInstanceOf(InvalidRefreshTokenException.class);

        verifyNoInteractions(
                accessTokenProvider,
                refreshTokenProvider
        );
    }

    @Test
    @DisplayName("로그아웃하면 Refresh Token을 폐기한다")
    void should_RevokeRefreshToken_When_Logout() {
        // given
        RefreshToken savedToken = token(MemberFixture.member());

        when(refreshTokenHasher.hash("refresh-token"))
                .thenReturn("refresh-token-hash");
        when(refreshTokenRepository.findByTokenHash("refresh-token-hash"))
                .thenReturn(Optional.of(savedToken));

        // when
        authTokenService.logout("refresh-token");

        // then
        assertThat(savedToken.getRevokedAt()).isEqualTo(NOW_DATE_TIME);
    }

    @Test
    @DisplayName("로그아웃하면 회원 ID와 함께 로그아웃 이벤트를 기록한다")
    void should_LogLogoutEvent_When_Logout() {
        // given
        Member member = MemberFixture.savedMember(7L);
        RefreshToken savedToken = token(member);
        when(refreshTokenHasher.hash("refresh-token"))
                .thenReturn("refresh-token-hash");
        when(refreshTokenRepository.findByTokenHash("refresh-token-hash"))
                .thenReturn(Optional.of(savedToken));

        Logger logger = (Logger) LoggerFactory.getLogger(AuthTokenService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        // when
        authTokenService.logout("refresh-token");
        logger.detachAppender(appender);

        // then
        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.getFirst().getKeyValuePairs())
                .extracting(pair -> pair.key, pair -> pair.value)
                .containsExactly(
                        tuple("type", "event"),
                        tuple("event", "logout"),
                        tuple("memberId", 7L)
                );
    }

    @Test
    @DisplayName("Refresh Token 없이 로그아웃해도 정상 처리한다")
    void should_CompleteLogout_When_RefreshTokenDoesNotExist() {
        // when
        authTokenService.logout(null);

        // then
        verifyNoInteractions(
                refreshTokenHasher,
                refreshTokenRepository
        );
    }

    @Test
    @DisplayName("이미 폐기된 Refresh Token으로 로그아웃해도 다시 폐기하지 않는다")
    void should_NotRevokeAgain_When_RefreshTokenIsAlreadyRevoked() {
        // given
        RefreshToken savedToken = token(MemberFixture.member());
        LocalDateTime revokedAt = NOW_DATE_TIME.minusSeconds(1);
        savedToken.revoke(revokedAt);

        when(refreshTokenHasher.hash("refresh-token"))
                .thenReturn("refresh-token-hash");
        when(refreshTokenRepository.findByTokenHash("refresh-token-hash"))
                .thenReturn(Optional.of(savedToken));

        // when
        authTokenService.logout("refresh-token");

        // then
        assertThat(savedToken.getRevokedAt()).isEqualTo(revokedAt);
    }

    private RefreshToken token(Member member) {
        return token(member, "refresh-token-hash");
    }

    private RefreshToken token(Member member, String hash) {
        return RefreshToken.issue(member, hash, NOW_DATE_TIME.minusDays(1), NOW_DATE_TIME.plusDays(14));
    }
}
