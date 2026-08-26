package com.chaekchaek.actor.domain;

import static org.assertj.core.api.Assertions.assertThat;

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
    }
}
