package com.chaekchaek.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.BDDMockito.given;

import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.common.exception.MemberNotFoundException;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.dto.MemberResponse;
import com.chaekchaek.member.repository.MemberRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("회원 ID로 내 정보를 조회한다")
    void should_GetMyInfo_When_MemberExists() {
        // given
        Member member = Member.create(
                "덜 우아한 참새",
                "exUrl",
                "참새-service",
                LocalDateTime.of(2026, 8, 13, 12, 0)
        );

        given(memberRepository.findById(1L))
                .willReturn(Optional.of(member));

        // when
        MemberResponse response = memberService.getMyInfo(1L);

        // then
        assertAll(
                () -> assertThat(response.nickname()).isEqualTo("덜 우아한 참새"),
                () -> assertThat(response.profileImageUrl()).isEqualTo("exUrl"),
                () -> assertThat(response.displayAnonymous()).isFalse(),
                () -> assertThat(response.accountStatus()).isEqualTo("ACTIVE")
        );

    }

    @Test
    @DisplayName("존재하지 않는 회원을 조회하면 예외가 발생한다")
    void should_ThrowException_When_MemberNotFound() {
        // given
        given(memberRepository.findById(999L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.getMyInfo(999L))
                .isInstanceOfSatisfying(
                        MemberNotFoundException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND)
                );
    }
}
