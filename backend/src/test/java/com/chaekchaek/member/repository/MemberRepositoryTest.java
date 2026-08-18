package com.chaekchaek.member.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
public class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("회원을 저장하고 ID로 조회한다")
    void should_FindMember_When_MemberIsSaved() {
        // given
        Member member = Member.create(
                MemberType.MEMBER,
                "우아한 참새",
                "exUrl",
                "참새-a1b2c3d4",
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
                () -> assertThat(foundMember.getAnonymousHandle()).isEqualTo("참새-a1b2c3d4"),
                () -> assertThat(foundMember.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE)
        );
    }

    @Test
    @DisplayName("익명 핸들은 회원마다 유일해야 한다")
    void should_RejectMember_When_AnonymousHandleIsDuplicated() {
        // given
        Member firstMember = Member.create(
                MemberType.MEMBER,
                "첫 번째 회원",
                null,
                "참새-duplicate",
                LocalDateTime.of(2026, 8, 11, 12, 0)
        );
        Member secondMember = Member.create(
                MemberType.MEMBER,
                "두 번째 회원",
                null,
                "참새-duplicate",
                LocalDateTime.of(2026, 8, 11, 12, 0)
        );
        memberRepository.saveAndFlush(firstMember);

        // when & then
        assertThatThrownBy(() -> memberRepository.saveAndFlush(secondMember))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
