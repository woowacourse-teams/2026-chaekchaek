package com.chaekchaek.auth.token.refresh;

import com.chaekchaek.member.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "refresh_token")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    private static final String EXPIRATION_MUST_BE_AFTER_ISSUED_AT_ERROR_MESSAGE =
            "[ERROR] Refresh Token 만료 시각은 발급 시각 이후여야 합니다";
    private static final String REFRESH_TOKEN_MUST_NOT_BE_REVOKED_ERROR_MESSAGE =
            "[ERROR] Refresh Token은 사용 가능한 상태여야 합니다. 다시 로그인해 주세요";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_token_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(
            name = "token_hash",
            nullable = false,
            unique = true,
            length = 64
    )
    private String tokenHash;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    private RefreshToken(
            Member member,
            String tokenHash,
            LocalDateTime issuedAt,
            LocalDateTime expiresAt
    ) {
        this.member = member;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public static RefreshToken issue(
            Member member,
            String tokenHash,
            LocalDateTime issuedAt,
            LocalDateTime expiresAt
    ) {
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException(EXPIRATION_MUST_BE_AFTER_ISSUED_AT_ERROR_MESSAGE);
        }

        return new RefreshToken(
                member,
                tokenHash,
                issuedAt,
                expiresAt
        );
    }

    public void revoke(LocalDateTime revokedAt) {
        if (this.revokedAt != null) {
            throw new IllegalStateException(REFRESH_TOKEN_MUST_NOT_BE_REVOKED_ERROR_MESSAGE);
        }

        this.revokedAt = revokedAt;
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isUsable(LocalDateTime now) {
        return !isRevoked() && !isExpired(now);
    }
}
