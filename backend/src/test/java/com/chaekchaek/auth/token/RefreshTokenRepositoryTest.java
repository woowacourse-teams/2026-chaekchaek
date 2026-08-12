package com.chaekchaek.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.domain.MemberType;
import com.chaekchaek.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Refresh Token을 저장하고 해시로 조회한다")
    void should_SaveAndFindByTokenHash() {
        // given
        LocalDateTime issuedAt =
                LocalDateTime.of(2026, 8, 12, 12, 0);

        Member member = memberRepository.save(
                Member.create(
                        MemberType.MEMBER,
                        "책책-refresh-1",
                        null,
                        issuedAt
                )
        );

        RefreshToken refreshToken = RefreshToken.issue(
                member,
                "a".repeat(64),
                issuedAt,
                issuedAt.plusDays(14)
        );

        refreshTokenRepository.save(refreshToken);

        entityManager.flush();
        entityManager.clear();

        // when
        RefreshToken foundToken = refreshTokenRepository
                .findByTokenHash("a".repeat(64))
                .orElseThrow();

        // then
        assertAll(
                () -> assertThat(foundToken.getId()).isNotNull(),
                () -> assertThat(foundToken.getTokenHash()).isEqualTo("a".repeat(64)),
                () -> assertThat(foundToken.getMember().getId()).isEqualTo(member.getId()),
                () -> assertThat(foundToken.getIssuedAt()).isEqualTo(issuedAt),
                () -> assertThat(foundToken.getExpiresAt()).isEqualTo(issuedAt.plusDays(14)),
                () -> assertThat(foundToken.getRevokedAt()).isNull()
        );
    }

    @Test
    @DisplayName("동일한 Refresh Token 해시 중복 저장은 거부된다")
    void should_Reject_DuplicateTokenHash() {
        // given
        LocalDateTime issuedAt =
                LocalDateTime.of(2026, 8, 12, 12, 0);

        Member member = memberRepository.save(
                Member.create(
                        MemberType.MEMBER,
                        "책책-refresh-2",
                        null,
                        issuedAt
                )
        );

        String duplicatedHash = "b".repeat(64);

        refreshTokenRepository.saveAndFlush(
                RefreshToken.issue(
                        member,
                        duplicatedHash,
                        issuedAt,
                        issuedAt.plusDays(14)
                )
        );

        RefreshToken duplicatedToken = RefreshToken.issue(
                member,
                duplicatedHash,
                issuedAt.plusMinutes(1),
                issuedAt.plusDays(14)
        );

        // when & then
        assertThatThrownBy(() ->
                refreshTokenRepository.saveAndFlush(duplicatedToken)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("폐기 시각 변경을 저장한다")
    void should_SaveRevocation() {
        // given
        LocalDateTime issuedAt =
                LocalDateTime.of(2026, 8, 12, 12, 0);

        Member member = memberRepository.save(
                Member.create(
                        MemberType.MEMBER,
                        "책책-refresh-3",
                        null,
                        issuedAt
                )
        );

        RefreshToken refreshToken = refreshTokenRepository.save(
                RefreshToken.issue(
                        member,
                        "c".repeat(64),
                        issuedAt,
                        issuedAt.plusDays(14)
                )
        );

        LocalDateTime revokedAt = issuedAt.plusDays(1);
        refreshToken.revoke(revokedAt);

        entityManager.flush();
        entityManager.clear();

        // when
        RefreshToken foundToken = refreshTokenRepository
                .findById(refreshToken.getId())
                .orElseThrow();

        // then
        assertThat(foundToken.getRevokedAt()).isEqualTo(revokedAt);
        assertThat(foundToken.isRevoked()).isTrue();
    }
}
