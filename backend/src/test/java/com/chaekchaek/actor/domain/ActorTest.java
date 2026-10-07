package com.chaekchaek.actor.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaekchaek.common.auth.ActorType;
import com.chaekchaek.member.domain.Member;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ActorTest {

    @Test
    void should_CreateMemberActorWithoutGuestCredentials_When_MemberIsProvided() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 8, 26, 12, 0);
        Member member = Member.create("다정한 참새", null, now);

        // when
        Actor actor = Actor.member(member, now);

        // then
        assertThat(actor.getType()).isEqualTo(ActorType.MEMBER);
        assertThat(actor.getMember()).isSameAs(member);
        assertThat(actor.getGuestTokenHash()).isNull();
        assertThat(actor.getGuestNickname()).isNull();
    }

    @Test
    void should_CreateGuestActorWithoutMember_When_GuestCredentialsAreProvided() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 8, 26, 12, 0);

        // when
        Actor actor = Actor.guest("a".repeat(64), "다정한 참새", now, now.plusDays(30));

        // then
        assertThat(actor.getType()).isEqualTo(ActorType.GUEST);
        assertThat(actor.getMember()).isNull();
        assertThat(actor.getGuestNickname()).isEqualTo("다정한 참새");
        assertThat(actor.getGuestTokenIssuedAt()).isEqualTo(now);
    }

    @Test
    void should_ConvertGuestActorAndRemoveGuestCredentials_When_MemberIsAssigned() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 8, 26, 12, 0);
        Actor actor = Actor.guest("a".repeat(64), "다정한 참새", now, now.plusDays(30));
        Member member = Member.create(actor.getGuestNickname(), null, now.plusHours(1));

        // when
        actor.convertToMember(member);

        // then
        assertThat(actor.getType()).isEqualTo(ActorType.MEMBER);
        assertThat(actor.getMember()).isSameAs(member);
        assertThat(actor.getGuestTokenHash()).isNull();
        assertThat(actor.getGuestNickname()).isNull();
        assertThat(actor.getExpiresAt()).isNull();
        assertThat(actor.getGuestTokenIssuedAt()).isNull();
    }

    @Test
    void should_RefreshGuestToken_When_TokenIsWithinRefreshWindow() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 8, 26, 12, 0);
        Actor actor = Actor.guest("a".repeat(64), "다정한 참새", now.minusDays(80), now.plusDays(10));

        // when & then
        assertThat(actor.isRefreshableGuestAt(now, Duration.ofDays(14))).isTrue();

        actor.refreshGuestToken("b".repeat(64), now, now.plusDays(90));

        assertThat(actor.getGuestTokenHash()).isEqualTo("b".repeat(64));
        assertThat(actor.getGuestTokenIssuedAt()).isEqualTo(now);
        assertThat(actor.getExpiresAt()).isEqualTo(now.plusDays(90));
    }

    @Test
    void should_RejectGuestTokenRefresh_When_TokenIsOutsideRefreshWindow() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 8, 26, 12, 0);
        Actor early = Actor.guest("a".repeat(64), "다정한 참새", now, now.plusDays(15));
        Actor expired = Actor.guest("b".repeat(64), "다정한 참새", now.minusDays(91), now.minusDays(1));

        // when & then
        assertThat(early.isRefreshableGuestAt(now, Duration.ofDays(14))).isFalse();
        assertThat(expired.isRefreshableGuestAt(now, Duration.ofDays(14))).isFalse();
    }

    @Test
    void should_GrantAdminPermission_When_ActorIsMember() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 8, 26, 12, 0);
        Member member = Member.create("다정한 참새", null, now);
        Actor actor = Actor.member(member, now);

        // when
        actor.grantAdmin();

        // then
        assertThat(actor.getType()).isEqualTo(ActorType.ADMIN);
        assertThat(actor.isAdmin()).isTrue();
        assertThat(actor.getMember()).isSameAs(member);
    }

    @Test
    void should_RejectAdminPermission_When_ActorIsGuest() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 8, 26, 12, 0);
        Actor actor = Actor.guest("a".repeat(64), "다정한 참새", now, now.plusDays(30));

        // when & then
        assertThatThrownBy(actor::grantAdmin).isInstanceOf(IllegalStateException.class);
        assertThat(actor.getType()).isEqualTo(ActorType.GUEST);
    }
}
