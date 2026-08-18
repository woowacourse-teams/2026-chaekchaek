package com.chaekchaek.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class MemberTest {

    @Test
    @DisplayName("신규 회원을 기본 상태로 생성한다")
    void should_CreateActiveMember_When_NewMemberIsCreated() {
        // given
        String nickname = "우아한 참새";
        String profileImageUrl = "exUrl";
        String anonymousHandle = "참새-a1b2c3d4";
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 11, 12, 0);

        // when
        Member member = Member.create(
                nickname,
                profileImageUrl,
                anonymousHandle,
                createdAt
        );

        // then
        assertAll(
                () -> assertThat(member.getId()).isNull(),
                () -> assertThat(member.getNickname()).isEqualTo(nickname),
                () -> assertThat(member.getProfileImageUrl()).isEqualTo(profileImageUrl),
                () -> assertThat(member.getAnonymousHandle()).isEqualTo(anonymousHandle),
                () -> assertThat(member.isDisplayAnonymous()).isFalse(),
                () -> assertThat(member.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE),
                () -> assertThat(member.getCreatedAt()).isEqualTo(createdAt),
                () -> assertThat(member.getWithdrawnAt()).isNull()
        );
    }

    @Test
    @DisplayName("닉네임이 없으면 회원을 생성할 수 없다")
    void should_ThrowException_When_NicknameIsNull() {
        assertThatThrownBy(() -> Member.create(

                null,
                null,
                "참새-a1b2c3d4",
                LocalDateTime.now()
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("닉네임이 공백이면 회원을 생성할 수 없다")
    void should_ThrowException_When_NicknameIsBlank() {
        assertThatThrownBy(() -> Member.create(
                " ",
                null,
                "참새-a1b2c3d4",
                LocalDateTime.now()
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("익명 핸들이 없으면 회원을 생성할 수 없다")
    void should_ThrowException_When_AnonymousHandleIsBlank() {
        assertThatThrownBy(() -> Member.create(
                "우아한 참새",
                null,
                " ",
                LocalDateTime.now()
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
