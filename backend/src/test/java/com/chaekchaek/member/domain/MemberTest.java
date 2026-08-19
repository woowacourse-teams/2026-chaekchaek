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
        String anonymousNickname = "우아한 달빛 참새";
        Member member = Member.create(anonymousNickname, "exUrl", LocalDateTime.now());

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
        assertThatThrownBy(() -> Member.create(" ", null, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("공개 닉네임을 설정해도 익명 상태는 유지된다")
    void should_RemainAnonymous_When_NicknameIsUpdated() {
        Member member = Member.create("우아한 달빛 참새", null, LocalDateTime.now());

        member.updateNickname("책책이");

        assertAll(
                () -> assertThat(member.getNickname()).isEqualTo("책책이"),
                () -> assertThat(member.isDisplayAnonymous()).isTrue(),
                () -> assertThat(member.getDisplayName()).isEqualTo("우아한 달빛 참새")
        );
    }

    @Test
    @DisplayName("공개 닉네임을 설정한 회원은 익명 상태를 해제할 수 있다")
    void should_DisplayNickname_When_AnonymousDisplayIsDisabled() {
        Member member = Member.create("우아한 달빛 참새", null, LocalDateTime.now());
        member.updateNickname("책책이");

        member.disableAnonymousDisplay();

        assertAll(
                () -> assertThat(member.isDisplayAnonymous()).isFalse(),
                () -> assertThat(member.getDisplayName()).isEqualTo("책책이")
        );
    }

    @Test
    @DisplayName("공개 닉네임을 설정하지 않으면 익명 상태를 해제할 수 없다")
    void should_RejectDisableAnonymous_When_NicknameIsNotSet() {
        Member member = Member.create("우아한 달빛 참새", null, LocalDateTime.now());

        assertThatThrownBy(member::disableAnonymousDisplay)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("[ERROR] 닉네임을 설정해야 익명 상태를 해제할 수 있습니다");
    }
}
