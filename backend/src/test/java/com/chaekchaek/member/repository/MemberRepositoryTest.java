package com.chaekchaek.member.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaekchaek.member.domain.Member;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("익명 닉네임의 중복을 허용한다")
    void should_AllowDuplicatedAnonymousNickname_When_MembersShareAnonymousNickname() {
        // given
        Member firstMember = Member.create("우아한 달빛 참새", null, LocalDateTime.now());
        Member secondMember = Member.create("우아한 달빛 참새", null, LocalDateTime.now());

        memberRepository.saveAndFlush(firstMember);

        // when
        memberRepository.saveAndFlush(secondMember);

        // then
        assertThat(memberRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("사용자가 설정한 공개 닉네임은 회원마다 유일해야 한다")
    void should_RejectNickname_When_NicknameIsDuplicated() {
        // given
        Member firstMember = Member.create("우아한 달빛 참새", null, LocalDateTime.now());
        firstMember.updateNickname("책책이");
        Member secondMember = Member.create("다정한 별빛 참새", null, LocalDateTime.now());
        secondMember.updateNickname("책책이");

        memberRepository.saveAndFlush(firstMember);

        // when & then
        assertThatThrownBy(() -> memberRepository.saveAndFlush(secondMember))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
