package com.chaekchaek.socialaccount.domain;

import com.chaekchaek.member.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "social_account",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_social_account_provider_user_id",
                        columnNames = {"provider", "provider_user_id"}
                )
        })
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_account_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    private Provider provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "connected_at", nullable = false)
    private LocalDateTime connectedAt;

    @Column(name = "provider_refresh_token", length = 2048)
    private String providerRefreshToken;

    private SocialAccount(
            Member member,
            Provider provider,
            String providerUserId,
            LocalDateTime connectedAt
    ) {
        this.member = member;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.connectedAt = connectedAt;
    }

    public static SocialAccount connect(
            Member member,
            Provider provider,
            String providerUserId,
            LocalDateTime connectedAt
    ) {
        return new SocialAccount(
                member,
                provider,
                providerUserId,
                connectedAt
        );
    }

    public void updateProviderRefreshToken(String providerRefreshToken) {
        this.providerRefreshToken = providerRefreshToken;
    }
}
