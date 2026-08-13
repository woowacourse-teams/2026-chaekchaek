package com.chaekchaek.auth.service;

import com.chaekchaek.auth.exception.InvalidRefreshTokenException;
import com.chaekchaek.auth.token.access.AccessTokenProvider;
import com.chaekchaek.auth.token.dto.IssuedTokens;
import com.chaekchaek.auth.token.refresh.IssuedRefreshToken;
import com.chaekchaek.auth.token.refresh.RefreshToken;
import com.chaekchaek.auth.token.refresh.RefreshTokenHasher;
import com.chaekchaek.auth.token.refresh.RefreshTokenProvider;
import com.chaekchaek.auth.token.refresh.RefreshTokenRepository;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.common.exception.MemberNotFoundException;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthTokenService {

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
                .orElseThrow(MemberNotFoundException::new);

        String accessToken = accessTokenProvider.issue(member);
        IssuedRefreshToken refreshToken = refreshTokenProvider.issue(member);

        return new IssuedTokens(accessToken, refreshToken);
    }

    @Transactional
    public IssuedTokens reissue(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new InvalidRefreshTokenException(ErrorCode.REFRESH_TOKEN_REQUIRED);
        }

        String tokenHash = refreshTokenHasher.hash(refreshTokenValue);

        RefreshToken savedRefreshToken =
                refreshTokenRepository.findByTokenHash(tokenHash)
                        .orElseThrow(() -> new InvalidRefreshTokenException(ErrorCode.INVALID_REFRESH_TOKEN));

        LocalDateTime now = now();

        if (!savedRefreshToken.isUsable(now)) {
            throw new InvalidRefreshTokenException(ErrorCode.UNUSABLE_REFRESH_TOKEN);
        }

        savedRefreshToken.revoke(now);

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
