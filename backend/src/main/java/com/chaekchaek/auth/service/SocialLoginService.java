package com.chaekchaek.auth.service;

import com.chaekchaek.auth.oauth.google.GoogleProfile;
import com.chaekchaek.auth.oauth.apple.AppleProfile;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import com.chaekchaek.member.service.NicknameGenerator;
import com.chaekchaek.socialaccount.domain.Provider;
import com.chaekchaek.socialaccount.domain.SocialAccount;
import com.chaekchaek.socialaccount.repository.SocialAccountRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SocialLoginService {

    private final MemberRepository memberRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final NicknameGenerator nicknameGenerator;

    public SocialLoginService(
            MemberRepository memberRepository,
            SocialAccountRepository socialAccountRepository,
            NicknameGenerator nicknameGenerator
    ) {
        this.memberRepository = memberRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.nicknameGenerator = nicknameGenerator;
    }

    @Transactional
    public Member loginOrSignUp(GoogleProfile memberInfo) {
        return loginOrSignUp(
                Provider.GOOGLE,
                memberInfo.providerUserId(),
                memberInfo.profileImageUrl()
        );
    }

    @Transactional
    public Member loginOrSignUp(AppleProfile memberInfo) {
        return loginOrSignUp(Provider.APPLE, memberInfo.providerUserId(), null);
    }

    private Member loginOrSignUp(Provider provider, String providerUserId, String profileImageUrl) {
        return socialAccountRepository
                .findByProviderAndProviderUserId(
                        provider,
                        providerUserId
                )
                .map(SocialAccount::getMember)
                .orElseGet(() -> signUp(provider, providerUserId, profileImageUrl));
    }

    private Member signUp(Provider provider, String providerUserId, String profileImageUrl) {
        LocalDateTime now = LocalDateTime.now();
        String anonymousNickname = nicknameGenerator.generate();

        Member member = Member.create(
                anonymousNickname,
                profileImageUrl,
                now
        );
        memberRepository.save(member);

        SocialAccount socialAccount = SocialAccount.connect(
                member,
                provider,
                providerUserId,
                now
        );
        socialAccountRepository.save(socialAccount);

        return member;
    }

    @Transactional
    public void updateProviderRefreshToken(
            Provider provider,
            String providerUserId,
            String refreshToken
    ) {
        socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .orElseThrow(IllegalStateException::new)
                .updateProviderRefreshToken(refreshToken);
    }

}
