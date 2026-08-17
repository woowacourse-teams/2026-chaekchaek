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

    private static final String NICKNAME_MUST_EXIST_ERROR_MESSAGE =
            "[ERROR] 닉네임이 존재해야 합니다";
    private static final String NICKNAME_LENGTH_MUST_BE_VALID_ERROR_MESSAGE =
            "[ERROR] 닉네임은 100자 이하여야 합니다";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_type", nullable = false, length = 20)
    private MemberType type;

    @Column(name = "nickname", nullable = false, unique = true, length = 100)
    private String nickname;

    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    private String profileImageUrl;

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
            MemberType type,
            String nickname,
            String profileImageUrl,
            boolean displayAnonymous,
            AccountStatus accountStatus,
            LocalDateTime createdAt,
            LocalDateTime withdrawnAt
    ) {
        this.type = type;
        this.nickname = nickname;
        this.profileImageUrl = normalizeProfileImageUrl(profileImageUrl);
        this.displayAnonymous = displayAnonymous;
        this.accountStatus = accountStatus;
        this.createdAt = createdAt;
        this.withdrawnAt = withdrawnAt;
    }

    public static Member create(
            MemberType memberType,
            String nickname,
            String profileImageUrl,
            LocalDateTime createdAt
    ) {
        validateNickname(nickname);

        return new Member(
                memberType,
                nickname,
                profileImageUrl,
                false,
                AccountStatus.ACTIVE,
                createdAt,
                null
        );
    }

    private static void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException(NICKNAME_MUST_EXIST_ERROR_MESSAGE);
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
