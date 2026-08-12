package com.chaekchaek.auth.token;

import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthTokenService {

    private static final String CANNOT_FIND_MEMBER_ERROR_MESSAGE = "토큰을 발급할 회원을 찾을 수 없습니다.";

    private final MemberRepository memberRepository;
    private final AccessTokenProvider accessTokenProvider;
    private final RefreshTokenProvider refreshTokenProvider;

    public AuthTokenService(
            MemberRepository memberRepository,
            AccessTokenProvider accessTokenProvider,
            RefreshTokenProvider refreshTokenProvider
    ) {
        this.memberRepository = memberRepository;
        this.accessTokenProvider = accessTokenProvider;
        this.refreshTokenProvider = refreshTokenProvider;
    }

    @Transactional
    public IssuedTokens issue(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException(CANNOT_FIND_MEMBER_ERROR_MESSAGE));

        String accessToken = accessTokenProvider.issue(member);
        IssuedRefreshToken refreshToken = refreshTokenProvider.issue(member);

        return new IssuedTokens(accessToken, refreshToken);
    }
}
