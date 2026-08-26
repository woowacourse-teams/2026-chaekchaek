package com.chaekchaek.auth.principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chaekchaek.actor.domain.Actor;
import com.chaekchaek.actor.repository.ActorRepository;
import com.chaekchaek.auth.token.guest.GuestTokenHasher;
import com.chaekchaek.common.auth.CurrentActor;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.member.domain.Member;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SecurityContextCurrentActorProviderTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-26T09:00:00Z"), ZoneOffset.UTC);

    @Test
    void resolvesGuestActorFromHeader() {
        Fixtures fixtures = new Fixtures();
        Actor guest = Actor.guest("hash", "게스트", LocalDateTime.now(CLOCK), LocalDateTime.now(CLOCK).plusDays(1));
        ReflectionTestUtils.setField(guest, "id", 7L);
        when(fixtures.request.getHeader(SecurityContextCurrentActorProvider.GUEST_TOKEN_HEADER)).thenReturn("token");
        when(fixtures.hasher.hash("token")).thenReturn("hash");
        when(fixtures.repository.findByGuestTokenHash("hash")).thenReturn(Optional.of(guest));

        Optional<CurrentActor> actor = fixtures.provider().findCurrentActor();

        assertThat(actor).contains(CurrentActor.guest(7L));
    }

    @Test
    void rejectsExpiredGuestToken() {
        Fixtures fixtures = new Fixtures();
        Actor guest = Actor.guest("hash", "게스트", LocalDateTime.now(CLOCK).minusDays(2),
                LocalDateTime.now(CLOCK).minusDays(1));
        when(fixtures.request.getHeader(SecurityContextCurrentActorProvider.GUEST_TOKEN_HEADER)).thenReturn("token");
        when(fixtures.hasher.hash("token")).thenReturn("hash");
        when(fixtures.repository.findByGuestTokenHash("hash")).thenReturn(Optional.of(guest));

        assertThatThrownBy(() -> fixtures.provider().findCurrentActor())
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNUSABLE_GUEST_TOKEN));
    }

    @Test
    void prefersLoggedInMemberOverGuestHeader() {
        Fixtures fixtures = new Fixtures();
        Member member = Member.create("회원", null, LocalDateTime.now(CLOCK));
        ReflectionTestUtils.setField(member, "id", 3L);
        Actor memberActor = Actor.member(member, LocalDateTime.now(CLOCK));
        ReflectionTestUtils.setField(memberActor, "id", 4L);
        when(fixtures.memberProvider.findCurrentMemberId()).thenReturn(OptionalLong.of(3L));
        when(fixtures.repository.findByMemberId(3L)).thenReturn(Optional.of(memberActor));
        when(fixtures.request.getHeader(SecurityContextCurrentActorProvider.GUEST_TOKEN_HEADER)).thenReturn("token");

        assertThat(fixtures.provider().findCurrentActor()).contains(CurrentActor.member(4L, 3L));
        verify(fixtures.repository, never()).findByGuestTokenHash(org.mockito.ArgumentMatchers.anyString());
    }

    private static class Fixtures {
        final SecurityContextCurrentMemberIdProvider memberProvider = mock(SecurityContextCurrentMemberIdProvider.class);
        final ActorRepository repository = mock(ActorRepository.class);
        final GuestTokenHasher hasher = mock(GuestTokenHasher.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);

        Fixtures() {
            when(memberProvider.findCurrentMemberId()).thenReturn(OptionalLong.empty());
        }

        SecurityContextCurrentActorProvider provider() {
            return new SecurityContextCurrentActorProvider(memberProvider, repository, hasher, request, CLOCK);
        }
    }
}
