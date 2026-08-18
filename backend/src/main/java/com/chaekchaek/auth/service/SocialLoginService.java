package com.chaekchaek.auth.service;

import com.chaekchaek.auth.oauth.google.GoogleProfile;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import com.chaekchaek.member.service.AnonymousHandleGenerator;
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
    private final AnonymousHandleGenerator anonymousHandleGenerator;

    public SocialLoginService(
            MemberRepository memberRepository,
            SocialAccountRepository socialAccountRepository,
            NicknameGenerator nicknameGenerator,
            AnonymousHandleGenerator anonymousHandleGenerator
    ) {
        this.memberRepository = memberRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.nicknameGenerator = nicknameGenerator;
        this.anonymousHandleGenerator = anonymousHandleGenerator;
    }

    @Transactional
    public Member loginOrSignUp(GoogleProfile memberInfo) {
        return socialAccountRepository
                .findByProviderAndProviderUserId(
                        Provider.GOOGLE,
                        memberInfo.providerUserId()
                )
                .map(SocialAccount::getMember)
                .orElseGet(() -> signUp(memberInfo));
    }

    private Member signUp(GoogleProfile memberInfo) {
        LocalDateTime now = LocalDateTime.now();
        String nickname = nicknameGenerator.generate();
        String anonymousHandle = anonymousHandleGenerator.generate();

        Member member = Member.create(
                nickname,
                memberInfo.profileImageUrl(),
                anonymousHandle,
                now
        );
        memberRepository.save(member);

        SocialAccount socialAccount = SocialAccount.connect(
                member,
                Provider.GOOGLE,
                memberInfo.providerUserId(),
                now
        );
        socialAccountRepository.save(socialAccount);

        return member;
    }

}
