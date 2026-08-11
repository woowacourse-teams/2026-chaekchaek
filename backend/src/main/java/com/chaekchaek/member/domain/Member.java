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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_type", nullable = false, length = 20)
    private MemberType type;

    @Column(name = "nickname", unique = true)
    private String nickname;

    @Column(name = "profile_image_url", length = 500)
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
        this.profileImageUrl = profileImageUrl;
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

    //TODO: updateProfile(), withdraw(), changeAnonymousDisplay()
}
