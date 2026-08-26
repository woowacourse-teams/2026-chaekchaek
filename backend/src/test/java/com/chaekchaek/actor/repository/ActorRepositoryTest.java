package com.chaekchaek.actor.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaekchaek.actor.domain.Actor;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class ActorRepositoryTest {

    @Autowired ActorRepository actorRepository;
    @Autowired MemberRepository memberRepository;

    @Test
    void findsActorsByMemberAndGuestTokenHash() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 26, 12, 0);
        Member member = memberRepository.save(Member.create("회원 참새", null, now));
        Actor memberActor = actorRepository.save(Actor.member(member, now));
        Actor guestActor = actorRepository.save(Actor.guest("b".repeat(64), "게스트 참새", now, now.plusDays(30)));

        assertThat(actorRepository.findByMemberId(member.getId())).contains(memberActor);
        assertThat(actorRepository.findByGuestTokenHash("b".repeat(64))).contains(guestActor);
    }

    @Test
    void backfillsOnlyMembersWithoutActor() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 26, 12, 0);
        Member existing = memberRepository.save(Member.create("기존 참새", null, now));
        Member missing = memberRepository.save(Member.create("누락 참새", null, now));
        actorRepository.saveAndFlush(Actor.member(existing, now));

        int inserted = actorRepository.backfillMissingMemberActors();

        assertThat(inserted).isEqualTo(1);
        assertThat(actorRepository.findByMemberId(missing.getId())).isPresent();
        assertThat(actorRepository.count()).isEqualTo(2);
    }
}
