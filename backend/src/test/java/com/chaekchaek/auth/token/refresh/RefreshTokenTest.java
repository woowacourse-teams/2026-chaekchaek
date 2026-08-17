package com.chaekchaek.auth.token.refresh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

import com.chaekchaek.member.domain.Member;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    private static final LocalDateTime ISSUED_AT =
            LocalDateTime.of(2026, 8, 12, 12, 0);

    private static final LocalDateTime EXPIRES_AT = ISSUED_AT.plusDays(14);

    private static final String TOKEN_HASH = "a".repeat(64);

    @Test
    @DisplayName("Refresh Token을 발급한다")
    void should_IssueRefreshToken_When_ExpirationIsAfterIssuedAt() {
        // given
        Member member = mock(Member.class);

        // when
        RefreshToken refreshToken = RefreshToken.issue(
                member,
                TOKEN_HASH,
                ISSUED_AT,
                EXPIRES_AT
        );

        // then
        assertAll(
                () -> assertThat(refreshToken.getId()).isNull(),
                () -> assertThat(refreshToken.getMember()).isSameAs(member),
                () -> assertThat(refreshToken.getTokenHash()).isEqualTo(TOKEN_HASH),
                () -> assertThat(refreshToken.getIssuedAt()).isEqualTo(ISSUED_AT),
                () -> assertThat(refreshToken.getExpiresAt()).isEqualTo(EXPIRES_AT),
                () -> assertThat(refreshToken.getRevokedAt()).isNull()
        );

    }

    @Test
    @DisplayName("만료 시각이 발급 시각과 같으면 발급이 거부된다")
    void should_ThrowException_When_ExpirationEqualsIssuedAt() {
        Member member = mock(Member.class);

        assertThatThrownBy(() -> RefreshToken.issue(
                member,
                TOKEN_HASH,
                ISSUED_AT,
                ISSUED_AT
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("만료 시각이 발급 시각보다 이전이면 발급할 수 없다")
    void should_ThrowException_When_ExpirationIsBeforeIssuedAt() {
        Member member = mock(Member.class);

        assertThatThrownBy(() -> RefreshToken.issue(
                member,
                TOKEN_HASH,
                ISSUED_AT,
                ISSUED_AT.minusSeconds(1)
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("만료 전이고 폐기되지 않은 Refresh Token은 사용할 수 있다")
    void should_ReturnUsable_When_TokenIsNotExpiredOrRevoked() {
        Member member = mock(Member.class);

        RefreshToken refreshToken = RefreshToken.issue(
                member,
                TOKEN_HASH,
                ISSUED_AT,
                EXPIRES_AT
        );

        assertThat(refreshToken.isUsable(
                EXPIRES_AT.minusSeconds(1)
        )).isTrue();
    }

    @Test
    @DisplayName("만료 시각부터 Refresh Token을 사용할 수 없다")
    void should_ReturnUnusable_When_CurrentTimeEqualsExpiration() {
        Member member = mock(Member.class);

        RefreshToken refreshToken = RefreshToken.issue(
                member,
                TOKEN_HASH,
                ISSUED_AT,
                EXPIRES_AT
        );

        assertThat(refreshToken.isExpired(EXPIRES_AT)).isTrue();
        assertThat(refreshToken.isUsable(EXPIRES_AT)).isFalse();
    }

    @Test
    @DisplayName("폐기한 Refresh Token은 사용할 수 없다")
    void should_ReturnUnusable_When_TokenIsRevoked() {
        Member member = mock(Member.class);

        RefreshToken refreshToken = RefreshToken.issue(
                member,
                TOKEN_HASH,
                ISSUED_AT,
                EXPIRES_AT
        );

        LocalDateTime revokedAt = ISSUED_AT.plusDays(1);

        refreshToken.revoke(revokedAt);

        assertThat(refreshToken.isRevoked()).isTrue();
        assertThat(refreshToken.getRevokedAt()).isEqualTo(revokedAt);
        assertThat(refreshToken.isUsable(revokedAt)).isFalse();
    }

    @Test
    @DisplayName("이미 폐기된 Refresh Token을 다시 폐기할 수 없다")
    void should_ThrowException_When_TokenIsAlreadyRevoked() {
        Member member = mock(Member.class);

        RefreshToken refreshToken = RefreshToken.issue(
                member,
                TOKEN_HASH,
                ISSUED_AT,
                EXPIRES_AT
        );

        refreshToken.revoke(ISSUED_AT.plusDays(1));

        assertThatThrownBy(() ->
                refreshToken.revoke(ISSUED_AT.plusDays(2))
        ).isInstanceOf(IllegalStateException.class);
    }
}
