package com.chaekchaek.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.chaekchaek.auth.token.refresh.RefreshToken;
import com.chaekchaek.auth.token.refresh.RefreshTokenRepository;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.common.exception.MemberNotFoundException;
import com.chaekchaek.common.exception.NicknameAlreadyExistsException;
import com.chaekchaek.common.exception.NicknameRequiredException;
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

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RefreshToken refreshToken;

    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("회원 ID로 내 정보를 조회한다")
    void should_GetMyInfo_When_MemberExists() {
        // given
        Member member = Member.create(
                "덜 우아한 참새",
                "exUrl",
                LocalDateTime.of(2026, 8, 13, 12, 0)
        );

        given(memberRepository.findById(1L))
                .willReturn(Optional.of(member));

        // when
        MemberResponse response = memberService.getMyInfo(1L);

        // then
        assertAll(
                () -> assertThat(response.nickname()).isNull(),
                () -> assertThat(response.profileImageUrl()).isEqualTo("exUrl"),
                () -> assertThat(response.anonymousNickname()).isEqualTo("덜 우아한 참새"),
                () -> assertThat(response.displayAnonymous()).isTrue(),
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

    @Test
    @DisplayName("공개 닉네임을 설정한다")
    void should_UpdateNickname() {
        Member member = Member.create("우아한 달빛 참새", null, LocalDateTime.now());
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(memberRepository.existsByNicknameAndIdNot("책책이", 1L)).willReturn(false);

        MemberResponse response = memberService.updateNickname(1L, "책책이");

        assertAll(
                () -> assertThat(response.nickname()).isEqualTo("책책이"),
                () -> assertThat(response.displayAnonymous()).isTrue()
        );
    }

    @Test
    @DisplayName("이미 사용 중인 공개 닉네임은 설정할 수 없다")
    void should_RejectDuplicatedNickname() {
        Member member = Member.create("우아한 달빛 참새", null, LocalDateTime.now());
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(memberRepository.existsByNicknameAndIdNot("책책이", 1L)).willReturn(true);

        assertThatThrownBy(() -> memberService.updateNickname(1L, "책책이"))
                .isInstanceOf(NicknameAlreadyExistsException.class);
    }

    @Test
    @DisplayName("공개 닉네임 없이 익명 상태를 해제할 수 없다")
    void should_RejectDisableAnonymityWithoutNickname() {
        Member member = Member.create("우아한 달빛 참새", null, LocalDateTime.now());
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.updateAnonymity(1L, false))
                .isInstanceOf(NicknameRequiredException.class);
    }

    @Test
    @DisplayName("공개 닉네임 설정 후 익명 상태를 해제한다")
    void should_DisableAnonymityAfterNicknameIsSet() {
        Member member = Member.create("우아한 달빛 참새", null, LocalDateTime.now());
        member.updateNickname("책책이");
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        MemberResponse response = memberService.updateAnonymity(1L, false);

        assertThat(response.displayAnonymous()).isFalse();
    }

    @Test
    @DisplayName("회원을 탈퇴 처리한다")
    void should_WithdrawMember() {
        Member member = Member.create("우아한 달빛 참새", "profile", LocalDateTime.now());
        member.updateNickname("책책이");
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(refreshTokenRepository.findAllByMemberIdAndRevokedAtIsNull(1L))
                .willReturn(java.util.List.of(refreshToken));

        memberService.withdraw(1L);

        assertAll(
                () -> assertThat(member.getAccountStatus().name()).isEqualTo("WITHDRAWN"),
                () -> assertThat(member.getWithdrawnAt()).isNotNull(),
                () -> assertThat(member.getNickname()).isNull(),
                () -> assertThat(member.getProfileImageUrl()).isNull()
        );
        verify(refreshToken).revoke(any(LocalDateTime.class));
    }
}
