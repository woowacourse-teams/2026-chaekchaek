package com.chaekchaek.auth.service;

import com.chaekchaek.auth.exception.InvalidRefreshTokenException;
import com.chaekchaek.auth.token.access.AccessTokenProvider;
import com.chaekchaek.auth.token.dto.IssuedTokens;
import com.chaekchaek.auth.token.refresh.IssuedRefreshToken;
import com.chaekchaek.auth.token.refresh.RefreshToken;
import com.chaekchaek.auth.token.refresh.RefreshTokenHasher;
import com.chaekchaek.auth.token.refresh.RefreshTokenProvider;
import com.chaekchaek.auth.token.refresh.RefreshTokenRepository;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthTokenService {

    private static final String MEMBER_MUST_EXIST_ERROR_MESSAGE =
            "[ERROR] 토큰을 발급할 회원이 존재해야 합니다";
    private static final String REFRESH_TOKEN_MUST_EXIST_ERROR_MESSAGE =
            "[ERROR] Refresh Token이 존재해야 합니다";
    private static final String REFRESH_TOKEN_MUST_BE_VALID_ERROR_MESSAGE =
            "[ERROR] Refresh Token은 유효해야 합니다. 다시 로그인해 주세요";
    private static final String REFRESH_TOKEN_MUST_BE_USABLE_ERROR_MESSAGE =
            "[ERROR] Refresh Token은 만료되거나 폐기되지 않은 상태여야 합니다. 다시 로그인해 주세요";

    private final MemberRepository memberRepository;
    private final AccessTokenProvider accessTokenProvider;
    private final RefreshTokenProvider refreshTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenHasher refreshTokenHasher;
    private final Clock clock;

    public AuthTokenService(
            MemberRepository memberRepository,
            AccessTokenProvider accessTokenProvider,
            RefreshTokenProvider refreshTokenProvider,
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenHasher refreshTokenHasher,
            Clock clock
    ) {
        this.memberRepository = memberRepository;
        this.accessTokenProvider = accessTokenProvider;
        this.refreshTokenProvider = refreshTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenHasher = refreshTokenHasher;
        this.clock = clock;
    }

    @Transactional
    public IssuedTokens issue(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException(MEMBER_MUST_EXIST_ERROR_MESSAGE));

        String accessToken = accessTokenProvider.issue(member);
        IssuedRefreshToken refreshToken = refreshTokenProvider.issue(member);

        return new IssuedTokens(accessToken, refreshToken);
    }

    @Transactional
    public IssuedTokens reissue(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new InvalidRefreshTokenException(REFRESH_TOKEN_MUST_EXIST_ERROR_MESSAGE);
        }

        String tokenHash = refreshTokenHasher.hash(refreshTokenValue);

        RefreshToken savedRefreshToken =
                refreshTokenRepository.findByTokenHash(tokenHash)
                        .orElseThrow(() -> new InvalidRefreshTokenException(REFRESH_TOKEN_MUST_BE_VALID_ERROR_MESSAGE));

        if (!savedRefreshToken.isUsable(now())) {
            throw new InvalidRefreshTokenException(REFRESH_TOKEN_MUST_BE_USABLE_ERROR_MESSAGE);
        }

        savedRefreshToken.revoke(now());

        Member member = savedRefreshToken.getMember();

        String newAccessToken = accessTokenProvider.issue(member);
        IssuedRefreshToken newRefreshToken =
                refreshTokenProvider.issue(member);

        return new IssuedTokens(
                newAccessToken,
                newRefreshToken
        );
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            return;
        }

        String tokenHash = refreshTokenHasher.hash(refreshTokenValue);

        refreshTokenRepository.findByTokenHash(tokenHash)
                .filter(refreshToken -> !refreshToken.isRevoked())
                .ifPresent(refreshToken -> refreshToken.revoke(now()));
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(
                clock.instant(),
                ZoneOffset.UTC
        );
    }
}
