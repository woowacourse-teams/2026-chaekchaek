package com.chaekchaek.auth.token.refresh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chaekchaek.member.domain.Member;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RefreshTokenProviderTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-08-12T06:00:00Z");

    private RefreshTokenRepository refreshTokenRepository;
    private RefreshTokenHasher refreshTokenHasher;
    private SecureRandom secureRandom;
    private RefreshTokenProvider refreshTokenProvider;

    @BeforeEach
    void setUp() {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        refreshTokenHasher = new RefreshTokenHasher();
        secureRandom = mock(SecureRandom.class);

        RefreshTokenProperties properties =
                new RefreshTokenProperties(
                        Duration.ofDays(14)
                );

        Clock fixedClock = Clock.fixed(
                FIXED_TIME,
                ZoneOffset.UTC
        );

        refreshTokenProvider = new RefreshTokenProvider(
                refreshTokenRepository,
                properties,
                refreshTokenHasher,
                secureRandom,
                fixedClock
        );
    }

    @Test
    @DisplayName("저장된 회원에게 Refresh Token을 발급한다")
    void should_IssueRefreshToken_When_MemberIsSaved() {
        // given
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(1L);

        doAnswer(invocation -> {
            byte[] bytes = invocation.getArgument(0);
            Arrays.fill(bytes, (byte) 1);
            return null;
        }).when(secureRandom).nextBytes(any(byte[].class));

        // when
        IssuedRefreshToken result = refreshTokenProvider.issue(member);

        // then
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken savedToken = captor.getValue();

        LocalDateTime expectedIssuedAt =
                LocalDateTime.ofInstant(
                        FIXED_TIME,
                        ZoneOffset.UTC
                );

        assertAll(
                () -> assertThat(result.value()).isNotBlank(),
                () -> assertThat(result.expiresAt()).isEqualTo(expectedIssuedAt.plusDays(14)),

                () -> assertThat(savedToken.getMember()).isSameAs(member),
                () -> assertThat(savedToken.getIssuedAt()).isEqualTo(expectedIssuedAt),
                () -> assertThat(savedToken.getExpiresAt()).isEqualTo(expectedIssuedAt.plusDays(14)),
                () -> assertThat(savedToken.getRevokedAt()).isNull()
        );
    }

    @Test
    @DisplayName("Refresh Token 원문 대신 해시를 저장한다")
    void should_SaveTokenHash_When_RefreshTokenIsIssued() {
        // given
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(1L);

        doAnswer(invocation -> {
            byte[] bytes = invocation.getArgument(0);
            Arrays.fill(bytes, (byte) 2);
            return null;
        }).when(secureRandom).nextBytes(any(byte[].class));

        // when
        IssuedRefreshToken result = refreshTokenProvider.issue(member);

        // then
        ArgumentCaptor<RefreshToken> captor =
                ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepository).save(captor.capture());

        String storedHash = captor.getValue().getTokenHash();

        assertThat(storedHash).hasSize(64);
        assertThat(storedHash).isNotEqualTo(result.value());
        assertThat(storedHash).isEqualTo(refreshTokenHasher.hash(result.value()));
    }

    @Test
    @DisplayName("저장되지 않은 회원에게 Refresh Token을 발급하지 않는다")
    void should_ThrowException_When_MemberIsUnsaved() {
        // given
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(null);

        // when & then
        assertThatThrownBy(() ->
                refreshTokenProvider.issue(member)
        ).isInstanceOf(IllegalArgumentException.class);

        verify(refreshTokenRepository, never())
                .save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Refresh Token을 발급할 때마다 다른 원문을 생성한다")
    void should_IssueDifferentToken_When_IssuedRepeatedly() {
        // given
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(1L);

        doAnswer(invocation -> {
            byte[] bytes = invocation.getArgument(0);
            Arrays.fill(bytes, (byte) 1);
            return null;
        }).doAnswer(invocation -> {
            byte[] bytes = invocation.getArgument(0);
            Arrays.fill(bytes, (byte) 2);
            return null;
        }).when(secureRandom).nextBytes(any(byte[].class));

        // when
        IssuedRefreshToken first = refreshTokenProvider.issue(member);
        IssuedRefreshToken second = refreshTokenProvider.issue(member);

        // then
        assertThat(first.value()).isNotEqualTo(second.value());
    }
}