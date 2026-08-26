package com.chaekchaek.auth.service;

import com.chaekchaek.actor.domain.Actor;
import com.chaekchaek.actor.repository.ActorRepository;
import com.chaekchaek.auth.oauth.apple.AppleProfile;
import com.chaekchaek.auth.oauth.google.GoogleProfile;
import com.chaekchaek.common.auth.CurrentActor;
import com.chaekchaek.common.auth.CurrentActorProvider;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import com.chaekchaek.member.service.NicknameGenerator;
import com.chaekchaek.socialaccount.domain.Provider;
import com.chaekchaek.socialaccount.domain.SocialAccount;
import com.chaekchaek.socialaccount.repository.SocialAccountRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SocialLoginService {

    private final MemberRepository memberRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final NicknameGenerator nicknameGenerator;
    private final ActorRepository actorRepository;
    private final CurrentActorProvider currentActorProvider;

    public SocialLoginService(
            MemberRepository memberRepository,
            SocialAccountRepository socialAccountRepository,
            NicknameGenerator nicknameGenerator,
            ActorRepository actorRepository,
            CurrentActorProvider currentActorProvider
    ) {
        this.memberRepository = memberRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.nicknameGenerator = nicknameGenerator;
        this.actorRepository = actorRepository;
        this.currentActorProvider = currentActorProvider;
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
        Actor guestActor = findGuestActorForConversion(now).orElse(null);
        String anonymousNickname = guestActor == null
                ? nicknameGenerator.generate()
                : guestActor.getGuestNickname();

        Member member = Member.create(
                anonymousNickname,
                profileImageUrl,
                now
        );
        memberRepository.save(member);
        if (guestActor == null) {
            actorRepository.save(Actor.member(member, now));
        } else {
            guestActor.convertToMember(member);
        }

        SocialAccount socialAccount = SocialAccount.connect(
                member,
                provider,
                providerUserId,
                now
        );
        socialAccountRepository.save(socialAccount);

        return member;
    }

    private Optional<Actor> findGuestActorForConversion(LocalDateTime now) {
        return currentActorProvider.findCurrentActor()
                .filter(CurrentActor::isGuest)
                .map(currentActor -> actorRepository.findByIdForUpdate(currentActor.actorId())
                        .filter(actor -> actor.isUsableGuestAt(now))
                        .orElseThrow(() -> new BusinessException(ErrorCode.UNUSABLE_GUEST_TOKEN)));
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
