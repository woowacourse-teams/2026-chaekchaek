package com.chaekchaek.auth.token.guest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chaekchaek.actor.domain.Actor;
import com.chaekchaek.actor.repository.ActorRepository;
import com.chaekchaek.common.auth.ActorType;
import com.chaekchaek.member.service.NicknameGenerator;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GuestTokenServiceTest {

    @Test
    void issuesOpaqueTokenAndStoresOnlyItsHash() {
        ActorRepository actorRepository = mock(ActorRepository.class);
        GuestTokenHasher hasher = new GuestTokenHasher();
        NicknameGenerator nicknameGenerator = mock(NicknameGenerator.class);
        SecureRandom secureRandom = mock(SecureRandom.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-26T09:00:00Z"), ZoneOffset.UTC);
        when(nicknameGenerator.generate()).thenReturn("다정한 파란 참새");
        doAnswer(invocation -> {
            byte[] bytes = invocation.getArgument(0);
            java.util.Arrays.fill(bytes, (byte) 1);
            return null;
        }).when(secureRandom).nextBytes(any(byte[].class));
        GuestTokenService service = new GuestTokenService(actorRepository, hasher,
                new GuestTokenProperties(Duration.ofDays(30)), nicknameGenerator, secureRandom, clock);

        IssuedGuestToken issued = service.issue();

        ArgumentCaptor<Actor> actorCaptor = ArgumentCaptor.forClass(Actor.class);
        verify(actorRepository).save(actorCaptor.capture());
        Actor actor = actorCaptor.getValue();
        assertThat(actor.getType()).isEqualTo(ActorType.GUEST);
        assertThat(actor.getGuestNickname()).isEqualTo("다정한 파란 참새");
        assertThat(actor.getGuestTokenHash()).isEqualTo(hasher.hash(issued.value()));
        assertThat(actor.getGuestTokenHash()).isNotEqualTo(issued.value());
        assertThat(issued.expiresAt()).isEqualTo("2026-09-25T09:00:00");
    }
}
