package com.chaekchaek.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member")
public class Member {

    private static final String NICKNAME_LENGTH_MUST_BE_VALID_ERROR_MESSAGE =
            "[ERROR] 닉네임은 100자 이하여야 합니다";
    private static final String ANONYMOUS_NICKNAME_MUST_EXIST_ERROR_MESSAGE =
            "[ERROR] 익명 닉네임이 존재해야 합니다";
    private static final String ANONYMOUS_NICKNAME_LENGTH_MUST_BE_VALID_ERROR_MESSAGE =
            "[ERROR] 익명 닉네임은 100자 이하여야 합니다";
    private static final String NICKNAME_REQUIRED_TO_DISABLE_ANONYMOUS_ERROR_MESSAGE =
            "[ERROR] 닉네임을 설정해야 익명 상태를 해제할 수 있습니다";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "nickname", unique = true, length = 100)
    private String nickname;

    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    private String profileImageUrl;

    @Column(name = "anonymous_nickname", nullable = false, length = 100)
    private String anonymousNickname;

    @Column(name = "display_anonymous", nullable = false)
    private boolean displayAnonymous;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    private AccountStatus accountStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    private Member(
            String nickname,
            String profileImageUrl,
            String anonymousNickname,
            boolean displayAnonymous,
            AccountStatus accountStatus,
            LocalDateTime createdAt,
            LocalDateTime withdrawnAt
    ) {
        this.nickname = nickname;
        this.profileImageUrl = normalizeProfileImageUrl(profileImageUrl);
        this.anonymousNickname = anonymousNickname;
        this.displayAnonymous = displayAnonymous;
        this.accountStatus = accountStatus;
        this.createdAt = createdAt;
        this.withdrawnAt = withdrawnAt;
    }

    public static Member create(
            String anonymousNickname,
            String profileImageUrl,
            LocalDateTime createdAt
    ) {
        validateAnonymousNickname(anonymousNickname);

        return new Member(
                null,
                profileImageUrl,
                anonymousNickname,
                true,
                AccountStatus.ACTIVE,
                createdAt,
                null
        );
    }

    public void updateNickname(String nickname) {
        validateNickname(nickname);
        this.nickname = nickname;
    }

    public void changeAnonymousDisplay(boolean displayAnonymous) {
        if (!displayAnonymous && nickname == null) {
            throw new IllegalStateException(NICKNAME_REQUIRED_TO_DISABLE_ANONYMOUS_ERROR_MESSAGE);
        }
        this.displayAnonymous = displayAnonymous;
    }

    public void disableAnonymousDisplay() {
        changeAnonymousDisplay(false);
    }

    public String getDisplayName() {
        return displayAnonymous ? anonymousNickname : nickname;
    }

    private static void validateAnonymousNickname(String anonymousNickname) {
        if (anonymousNickname == null || anonymousNickname.isBlank()) {
            throw new IllegalArgumentException(ANONYMOUS_NICKNAME_MUST_EXIST_ERROR_MESSAGE);
        }
        if (anonymousNickname.length() > 100) {
            throw new IllegalArgumentException(ANONYMOUS_NICKNAME_LENGTH_MUST_BE_VALID_ERROR_MESSAGE);
        }
    }

    private static void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("[ERROR] 닉네임이 존재해야 합니다");
        }

        if (nickname.length() > 100) {
            throw new IllegalArgumentException(NICKNAME_LENGTH_MUST_BE_VALID_ERROR_MESSAGE);
        }
    }

    private static String normalizeProfileImageUrl(String profileImageUrl) {
        if (profileImageUrl == null || profileImageUrl.isBlank()) {
            return null;
        }

        return profileImageUrl;
    }

    //TODO: updateProfile(), withdraw(), changeAnonymousDisplay()
}
