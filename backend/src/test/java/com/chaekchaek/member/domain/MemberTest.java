package com.chaekchaek.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberTest {

    @Test
    @DisplayName("신규 회원은 랜덤 닉네임을 사용하는 익명 상태로 생성된다")
    void should_CreateAnonymousMember_When_NewMemberIsCreated() {
        // given
        String anonymousNickname = "우아한 달빛 참새";

        // when
        Member member = Member.create(anonymousNickname, "exUrl", LocalDateTime.now());

        // then
        assertAll(
                () -> assertThat(member.getNickname()).isNull(),
                () -> assertThat(member.getAnonymousNickname()).isEqualTo(anonymousNickname),
                () -> assertThat(member.getDisplayName()).isEqualTo(anonymousNickname),
                () -> assertThat(member.isDisplayAnonymous()).isTrue(),
                () -> assertThat(member.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE)
        );
    }

    @Test
    @DisplayName("익명 닉네임이 없으면 회원을 생성할 수 없다")
    void should_ThrowException_When_AnonymousNicknameIsBlank() {
        // when & then
        assertThatThrownBy(() -> Member.create(" ", null, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("공개 닉네임을 설정해도 익명 상태는 유지된다")
    void should_RemainAnonymous_When_NicknameIsUpdated() {
        // given
        Member member = Member.create("우아한 달빛 참새", null, LocalDateTime.now());

        // when
        member.updateNickname("책책이");

        // then
        assertAll(
                () -> assertThat(member.getNickname()).isEqualTo("책책이"),
                () -> assertThat(member.isDisplayAnonymous()).isTrue(),
                () -> assertThat(member.getDisplayName()).isEqualTo("우아한 달빛 참새")
        );
    }

    @Test
    @DisplayName("공개 닉네임을 설정한 회원은 익명 상태를 해제할 수 있다")
    void should_DisplayNickname_When_AnonymousDisplayIsDisabled() {
        // given
        Member member = Member.create("우아한 달빛 참새", null, LocalDateTime.now());
        member.updateNickname("책책이");

        // when
        member.disableAnonymousDisplay();

        // then
        assertAll(
                () -> assertThat(member.isDisplayAnonymous()).isFalse(),
                () -> assertThat(member.getDisplayName()).isEqualTo("책책이")
        );
    }

    @Test
    @DisplayName("공개 닉네임을 설정하지 않으면 익명 상태를 해제할 수 없다")
    void should_RejectDisableAnonymous_When_NicknameIsNotSet() {
        // given
        Member member = Member.create("우아한 달빛 참새", null, LocalDateTime.now());

        // when & then
        assertThatThrownBy(member::disableAnonymousDisplay)
                .isInstanceOf(IllegalStateException.class);
        assertThat(member.isDisplayAnonymous()).isTrue();
    }

    @Test
    @DisplayName("회원 탈퇴 시 개인정보를 제거하고 익명 닉네임은 유지한다")
    void should_AnonymizeMember_When_Withdrawn() {
        // given
        Member member = Member.create("우아한 달빛 참새", "profile", LocalDateTime.now());
        member.updateNickname("책책이");
        member.disableAnonymousDisplay();
        LocalDateTime withdrawnAt = LocalDateTime.of(2026, 8, 19, 15, 0);

        // when
        member.withdraw(withdrawnAt);

        // then
        assertAll(
                () -> assertThat(member.getNickname()).isNull(),
                () -> assertThat(member.getProfileImageUrl()).isNull(),
                () -> assertThat(member.getAnonymousNickname()).isEqualTo("우아한 달빛 참새"),
                () -> assertThat(member.isDisplayAnonymous()).isTrue(),
                () -> assertThat(member.getAccountStatus()).isEqualTo(AccountStatus.WITHDRAWN),
                () -> assertThat(member.getWithdrawnAt()).isEqualTo(withdrawnAt)
        );
    }
}
