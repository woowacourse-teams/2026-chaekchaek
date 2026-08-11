package com.chaekchaek.member.repository;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.chaekchaek.member.domain.AccountStatus;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.domain.MemberType;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
public class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("회원을 저장하고 ID로 조회한다")
    void should_SaveAndFindById() {
        // given
        Member member = Member.create(
                MemberType.MEMBER,
                "우아한 참새",
                "exUrl",
                LocalDateTime.of(2026, 8, 11, 12, 0)
        );

        // when
        Member savedMember = memberRepository.save(member);

        entityManager.flush();
        entityManager.clear();

        Member foundMember = memberRepository.findById(savedMember.getId())
                .orElseThrow();

        // then
        assertAll(
                () -> assertThat(foundMember.getId()).isEqualTo(savedMember.getId()),
                () -> assertThat(foundMember.getType()).isEqualTo(MemberType.MEMBER),
                () -> assertThat(foundMember.getNickname()).isEqualTo("우아한 참새"),
                () -> assertThat(foundMember.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE)
        );
    }
}
