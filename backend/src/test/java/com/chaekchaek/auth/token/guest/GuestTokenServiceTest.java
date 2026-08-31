package com.chaekchaek.auth.token.guest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chaekchaek.actor.domain.Actor;
import com.chaekchaek.actor.repository.ActorRepository;
import com.chaekchaek.common.auth.ActorType;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.member.service.NicknameGenerator;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class GuestTokenServiceTest {

    @Test
    void issuesOpaqueTokenValidForNinetyDaysAndStoresOnlyItsHash() {
        ActorRepository actorRepository = mock(ActorRepository.class);
        GuestTokenHasher hasher = new GuestTokenHasher();
        NicknameGenerator nicknameGenerator = mock(NicknameGenerator.class);
        SecureRandom secureRandom = mock(SecureRandom.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-26T09:00:00Z"), ZoneOffset.UTC);
        when(nicknameGenerator.generate()).thenReturn("다정한 파란 참새");
        fillRandomBytes(secureRandom, (byte) 1);
        GuestTokenService service = new GuestTokenService(actorRepository, hasher,
                properties(), nicknameGenerator, secureRandom, clock);

        IssuedGuestToken issued = service.issue();

        ArgumentCaptor<Actor> actorCaptor = ArgumentCaptor.forClass(Actor.class);
        verify(actorRepository).save(actorCaptor.capture());
        Actor actor = actorCaptor.getValue();
        assertThat(actor.getType()).isEqualTo(ActorType.GUEST);
        assertThat(actor.getGuestNickname()).isEqualTo("다정한 파란 참새");
        assertThat(actor.getGuestTokenHash()).isEqualTo(hasher.hash(issued.value()));
        assertThat(actor.getGuestTokenHash()).isNotEqualTo(issued.value());
        assertThat(actor.getGuestTokenIssuedAt()).isEqualTo("2026-08-26T09:00:00");
        assertThat(issued.expiresAt()).isEqualTo("2026-11-24T09:00:00");
    }

    @Test
    void refreshesTokenForSameActorWithinRefreshWindow() {
        TestFixture fixture = new TestFixture();
        Actor actor = fixture.actorExpiringAt(fixture.now.plusDays(10));

        IssuedGuestToken refreshed = fixture.service.refresh("current-token");

        assertThat(refreshed.value()).isNotEqualTo("current-token");
        assertThat(refreshed.nickname()).isEqualTo("다정한 파란 참새");
        assertThat(refreshed.expiresAt()).isEqualTo(fixture.now.plusDays(90));
        assertThat(actor.getGuestTokenHash()).isEqualTo(fixture.hasher.hash(refreshed.value()));
        assertThat(actor.getGuestTokenIssuedAt()).isEqualTo(fixture.now);
        assertThat(actor.getExpiresAt()).isEqualTo(fixture.now.plusDays(90));
        verify(fixture.actorRepository).findByIdForUpdate(7L);
    }

    @Test
    void rejectsRefreshBeforeRefreshWindow() {
        TestFixture fixture = new TestFixture();
        Actor actor = fixture.actorExpiringAt(fixture.now.plusDays(15));

        assertThatThrownBy(() -> fixture.service.refresh("current-token"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.GUEST_TOKEN_REFRESH_NOT_ALLOWED));
        assertThat(actor.getGuestTokenHash()).isEqualTo(fixture.hasher.hash("current-token"));
    }

    @Test
    void rejectsExpiredGuestTokenRefresh() {
        TestFixture fixture = new TestFixture();
        fixture.actorExpiringAt(fixture.now.minusSeconds(1));

        assertThatThrownBy(() -> fixture.service.refresh("current-token"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.UNUSABLE_GUEST_TOKEN));
    }

    @Test
    void rejectsUnknownGuestTokenRefresh() {
        TestFixture fixture = new TestFixture();
        when(fixture.actorRepository.findByGuestTokenHash(fixture.hasher.hash("unknown")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> fixture.service.refresh("unknown"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_GUEST_TOKEN));
    }

    private static GuestTokenProperties properties() {
        return new GuestTokenProperties(Duration.ofDays(90), Duration.ofDays(14));
    }

    private static void fillRandomBytes(SecureRandom secureRandom, byte value) {
        doAnswer(invocation -> {
            byte[] bytes = invocation.getArgument(0);
            java.util.Arrays.fill(bytes, value);
            return null;
        }).when(secureRandom).nextBytes(any(byte[].class));
    }

    private static class TestFixture {
        private final ActorRepository actorRepository = mock(ActorRepository.class);
        private final GuestTokenHasher hasher = new GuestTokenHasher();
        private final NicknameGenerator nicknameGenerator = mock(NicknameGenerator.class);
        private final SecureRandom secureRandom = mock(SecureRandom.class);
        private final LocalDateTime now = LocalDateTime.of(2026, 8, 26, 9, 0);
        private final GuestTokenService service;

        private TestFixture() {
            Clock clock = Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
            fillRandomBytes(secureRandom, (byte) 2);
            service = new GuestTokenService(actorRepository, hasher, properties(),
                    nicknameGenerator, secureRandom, clock);
        }

        private Actor actorExpiringAt(LocalDateTime expiresAt) {
            Actor actor = Actor.guest(hasher.hash("current-token"), "다정한 파란 참새",
                    now.minusDays(80), expiresAt);
            ReflectionTestUtils.setField(actor, "id", 7L);
            when(actorRepository.findByGuestTokenHash(hasher.hash("current-token")))
                    .thenReturn(Optional.of(actor));
            when(actorRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(actor));
            return actor;
        }
    }
}
