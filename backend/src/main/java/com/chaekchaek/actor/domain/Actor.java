package com.chaekchaek.actor.domain;

import com.chaekchaek.common.auth.ActorType;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "actor", uniqueConstraints = {
        @UniqueConstraint(name = "uk_actor_member", columnNames = "member_id"),
        @UniqueConstraint(name = "uk_actor_guest_token_hash", columnNames = "guest_token_hash")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Actor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "actor_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 20)
    private ActorType type;

    @Column(name = "guest_token_hash", length = 64)
    private String guestTokenHash;

    @Column(name = "guest_nickname", length = 100)
    private String guestNickname;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    private Actor(Member member, ActorType type, String guestTokenHash, String guestNickname,
                  LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.member = member;
        this.type = type;
        this.guestTokenHash = guestTokenHash;
        this.guestNickname = guestNickname;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public static Actor member(Member member, LocalDateTime createdAt) {
        if (member == null || createdAt == null) {
            throw new IllegalArgumentException("Member actor requires a member and creation time");
        }
        return new Actor(member, ActorType.MEMBER, null, null, createdAt, null);
    }

    public static Actor guest(String tokenHash, String nickname, LocalDateTime createdAt, LocalDateTime expiresAt) {
        if (tokenHash == null || tokenHash.isBlank() || tokenHash.length() > 64) {
            throw new IllegalArgumentException("Guest actor requires a valid token hash");
        }
        if (nickname == null || nickname.isBlank() || nickname.length() > 100) {
            throw new IllegalArgumentException("Guest actor requires a valid nickname");
        }
        if (createdAt == null || expiresAt == null || !expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("Guest actor requires a valid expiration time");
        }
        return new Actor(null, ActorType.GUEST, tokenHash, nickname, createdAt, expiresAt);
    }

    public boolean isUsableGuestAt(LocalDateTime now) {
        return type == ActorType.GUEST
                && revokedAt == null
                && expiresAt != null
                && expiresAt.isAfter(now);
    }

    public void convertToMember(Member member) {
        if (type != ActorType.GUEST || member == null) {
            throw new IllegalStateException("Only a guest actor can be converted to a member actor");
        }

        this.member = member;
        this.type = ActorType.MEMBER;
        this.guestTokenHash = null;
        this.guestNickname = null;
        this.expiresAt = null;
        this.revokedAt = null;
    }
}
