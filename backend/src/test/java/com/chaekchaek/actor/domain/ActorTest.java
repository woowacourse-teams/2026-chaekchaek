package com.chaekchaek.actor.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaekchaek.common.auth.ActorType;
import com.chaekchaek.member.domain.Member;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ActorTest {

    @Test
    void createsMemberActorWithoutGuestCredentials() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 26, 12, 0);
        Member member = Member.create("다정한 참새", null, now);

        Actor actor = Actor.member(member, now);

        assertThat(actor.getType()).isEqualTo(ActorType.MEMBER);
        assertThat(actor.getMember()).isSameAs(member);
        assertThat(actor.getGuestTokenHash()).isNull();
        assertThat(actor.getGuestNickname()).isNull();
    }

    @Test
    void createsGuestActorWithoutMember() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 26, 12, 0);

        Actor actor = Actor.guest("a".repeat(64), "다정한 참새", now, now.plusDays(30));

        assertThat(actor.getType()).isEqualTo(ActorType.GUEST);
        assertThat(actor.getMember()).isNull();
        assertThat(actor.getGuestNickname()).isEqualTo("다정한 참새");
        assertThat(actor.getGuestTokenIssuedAt()).isEqualTo(now);
    }

    @Test
    void convertsGuestActorToMemberAndRemovesGuestCredentials() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 26, 12, 0);
        Actor actor = Actor.guest("a".repeat(64), "다정한 참새", now, now.plusDays(30));
        Member member = Member.create(actor.getGuestNickname(), null, now.plusHours(1));

        actor.convertToMember(member);

        assertThat(actor.getType()).isEqualTo(ActorType.MEMBER);
        assertThat(actor.getMember()).isSameAs(member);
        assertThat(actor.getGuestTokenHash()).isNull();
        assertThat(actor.getGuestNickname()).isNull();
        assertThat(actor.getExpiresAt()).isNull();
        assertThat(actor.getGuestTokenIssuedAt()).isNull();
    }

    @Test
    void refreshesGuestTokenWithinRefreshWindow() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 26, 12, 0);
        Actor actor = Actor.guest("a".repeat(64), "다정한 참새", now.minusDays(80), now.plusDays(10));

        assertThat(actor.isRefreshableGuestAt(now, java.time.Duration.ofDays(14))).isTrue();

        actor.refreshGuestToken("b".repeat(64), now, now.plusDays(90));

        assertThat(actor.getGuestTokenHash()).isEqualTo("b".repeat(64));
        assertThat(actor.getGuestTokenIssuedAt()).isEqualTo(now);
        assertThat(actor.getExpiresAt()).isEqualTo(now.plusDays(90));
    }

    @Test
    void doesNotAllowRefreshBeforeWindowOrAfterExpiration() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 26, 12, 0);
        Actor early = Actor.guest("a".repeat(64), "다정한 참새", now, now.plusDays(15));
        Actor expired = Actor.guest("b".repeat(64), "다정한 참새", now.minusDays(91), now.minusDays(1));

        assertThat(early.isRefreshableGuestAt(now, java.time.Duration.ofDays(14))).isFalse();
        assertThat(expired.isRefreshableGuestAt(now, java.time.Duration.ofDays(14))).isFalse();
    }

    @Test
    void grantsAdminPermissionToMemberActor() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 26, 12, 0);
        Member member = Member.create("다정한 참새", null, now);
        Actor actor = Actor.member(member, now);

        actor.grantAdmin();

        assertThat(actor.getType()).isEqualTo(ActorType.ADMIN);
        assertThat(actor.isAdmin()).isTrue();
        assertThat(actor.getMember()).isSameAs(member);
    }

    @Test
    void rejectsAdminPermissionForGuestActor() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 26, 12, 0);
        Actor actor = Actor.guest("a".repeat(64), "다정한 참새", now, now.plusDays(30));

        assertThatThrownBy(actor::grantAdmin).isInstanceOf(IllegalStateException.class);
        assertThat(actor.getType()).isEqualTo(ActorType.GUEST);
    }
}
