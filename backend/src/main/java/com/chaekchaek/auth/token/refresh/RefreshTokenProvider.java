package com.chaekchaek.auth.token.refresh;

import com.chaekchaek.member.domain.Member;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RefreshTokenProvider {

    private static final String MEMBER_MUST_BE_SAVED_ERROR_MESSAGE =
            "[ERROR] Refresh Token을 발급할 회원은 저장된 상태여야 합니다";
    private static final int TOKEN_BYTE_LENGTH = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenProperties properties;
    private final RefreshTokenHasher refreshTokenHasher;
    private final SecureRandom secureRandom;
    private final Clock clock;

    public RefreshTokenProvider(
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenProperties properties,
            RefreshTokenHasher refreshTokenHasher,
            SecureRandom secureRandom,
            Clock clock
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.properties = properties;
        this.refreshTokenHasher = refreshTokenHasher;
        this.secureRandom = secureRandom;
        this.clock = clock;
    }

    @Transactional
    public IssuedRefreshToken issue(Member member) {
        if (member.getId() == null) {
            throw new IllegalArgumentException(MEMBER_MUST_BE_SAVED_ERROR_MESSAGE);
        }

        String tokenValue = generateTokenValue();
        String tokenHash = refreshTokenHasher.hash(tokenValue);

        LocalDateTime issuedAt = LocalDateTime.ofInstant(
                clock.instant(),
                ZoneOffset.UTC
        );
        LocalDateTime expiresAt = issuedAt.plus(properties.expiration());

        RefreshToken refreshToken = RefreshToken.issue(
                member,
                tokenHash,
                issuedAt,
                expiresAt
        );

        refreshTokenRepository.save(refreshToken);

        return new IssuedRefreshToken(
                tokenValue,
                expiresAt
        );
    }

    private String generateTokenValue() {
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }
}