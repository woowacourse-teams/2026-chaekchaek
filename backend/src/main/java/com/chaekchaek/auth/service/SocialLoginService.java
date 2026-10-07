package com.chaekchaek.auth.service;

import com.chaekchaek.actor.domain.Actor;
import com.chaekchaek.actor.repository.ActorRepository;
import com.chaekchaek.auth.oauth.apple.AppleProfile;
import com.chaekchaek.auth.oauth.google.GoogleProfile;
import com.chaekchaek.common.auth.CurrentActor;
import com.chaekchaek.common.auth.CurrentActorProvider;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.member.domain.AccountStatus;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import com.chaekchaek.member.service.NicknameGenerator;
import com.chaekchaek.socialaccount.domain.Provider;
import com.chaekchaek.socialaccount.domain.SocialAccount;
import com.chaekchaek.socialaccount.repository.SocialAccountRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SocialLoginService {

    private static final Logger log = LoggerFactory.getLogger(SocialLoginService.class);

    private final MemberRepository memberRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final NicknameGenerator nicknameGenerator;
    private final ActorRepository actorRepository;
    private final CurrentActorProvider currentActorProvider;
    private final GuestActorMigrationService guestActorMigrationService;

    public SocialLoginService(
            MemberRepository memberRepository,
            SocialAccountRepository socialAccountRepository,
            NicknameGenerator nicknameGenerator,
            ActorRepository actorRepository,
            CurrentActorProvider currentActorProvider,
            GuestActorMigrationService guestActorMigrationService
    ) {
        this.memberRepository = memberRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.nicknameGenerator = nicknameGenerator;
        this.actorRepository = actorRepository;
        this.currentActorProvider = currentActorProvider;
        this.guestActorMigrationService = guestActorMigrationService;
    }

    @Transactional
    public Member loginOrSignUp(GoogleProfile memberInfo) {
        return loginOrSignUpFromCurrentActor(
                Provider.GOOGLE,
                memberInfo.providerUserId(),
                memberInfo.profileImageUrl()
        );
    }

    @Transactional
    public Member loginOrSignUp(GoogleProfile memberInfo, Long guestActorId) {
        return loginOrSignUp(
                Provider.GOOGLE,
                memberInfo.providerUserId(),
                memberInfo.profileImageUrl(),
                guestActorId
        );
    }

    private Member loginOrSignUp(
            Provider provider,
            String providerUserId,
            String profileImageUrl,
            Long guestActorId
    ) {
        return socialAccountRepository
                .findForLogin(provider, providerUserId)
                .map(account -> loginExisting(account, profileImageUrl, guestActorId))
                .orElseGet(() -> signUp(provider, providerUserId, profileImageUrl, guestActorId));
    }

    @Transactional
    public Member loginOrSignUp(AppleProfile memberInfo) {
        return loginOrSignUpFromCurrentActor(
                Provider.APPLE,
                memberInfo.providerUserId(),
                null
        );
    }

    private Member loginOrSignUpFromCurrentActor(Provider provider, String providerUserId, String profileImageUrl) {
        return socialAccountRepository
                .findForLogin(provider, providerUserId)
                .map(account -> loginExisting(
                        account,
                        profileImageUrl,
                        account.getMember().getAccountStatus() == AccountStatus.WITHDRAWN
                                ? currentGuestActorId() : null
                ))
                .orElseGet(() -> signUp(
                        provider,
                        providerUserId,
                        profileImageUrl,
                        currentGuestActorId()
                ));
    }

    private Member loginExisting(SocialAccount account, String profileImageUrl, Long guestActorId) {
        Member member = account.getMember();
        validateLoginStatus(member);
        if (member.getAccountStatus() == AccountStatus.WITHDRAWN) {
            return rejoin(account, profileImageUrl, guestActorId);
        }
        if (guestActorId != null) {
            guestActorMigrationService.migrate(guestActorId, member);
        }
        return logLogin(account.getProvider(), member);
    }

    private void validateLoginStatus(Member member) {
        AccountStatus status = member.getAccountStatus();
        if (status != AccountStatus.ACTIVE && status != AccountStatus.WITHDRAWN) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    private Member rejoin(SocialAccount account, String profileImageUrl, Long guestActorId) {
        LocalDateTime now = LocalDateTime.now();
        Member newMember = Member.create(nicknameGenerator.generate(), profileImageUrl, now);
        memberRepository.save(newMember);
        actorRepository.save(Actor.member(newMember, now));
        if (guestActorId != null) {
            guestActorMigrationService.migrate(guestActorId, newMember);
        }
        account.reconnect(newMember, now);
        return newMember;
    }

    private Member logLogin(Provider provider, Member member) {
        log.info("Social login: provider={}, memberId={}", provider, member.getId());
        return member;
    }

    private Long currentGuestActorId() {
        return currentActorProvider.findCurrentActor()
                .filter(CurrentActor::isGuest)
                .map(CurrentActor::actorId)
                .orElse(null);
    }

    private Member signUp(
            Provider provider,
            String providerUserId,
            String profileImageUrl,
            Long guestActorId
    ) {
        LocalDateTime now = LocalDateTime.now();
        Actor guestActor = guestActorId == null
                ? null
                : actorRepository.findByIdForUpdate(guestActorId)
                        .filter(actor -> actor.isUsableGuestAt(now))
                        .orElseThrow(() -> new BusinessException(ErrorCode.UNUSABLE_GUEST_TOKEN));
        String anonymousNickname = guestActor == null
                ? nicknameGenerator.generate()
                : guestActor.getGuestNickname();

        Member member = Member.create(anonymousNickname, profileImageUrl, now);
        memberRepository.save(member);
        if (guestActor == null) {
            actorRepository.save(Actor.member(member, now));
        } else {
            guestActorMigrationService.migrate(guestActorId, member);
        }

        SocialAccount socialAccount = SocialAccount.connect(
                member,
                provider,
                providerUserId,
                now
        );
        socialAccountRepository.save(socialAccount);

        log.info(
                "Social sign up: provider={}, memberId={}, fromGuest={}",
                provider,
                member.getId(),
                guestActor != null
        );
        return member;
    }

    @Transactional
    public void updateProviderRefreshToken(Provider provider, String providerUserId, String refreshToken) {
        socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .orElseThrow(IllegalStateException::new)
                .updateProviderRefreshToken(refreshToken);
    }
}
