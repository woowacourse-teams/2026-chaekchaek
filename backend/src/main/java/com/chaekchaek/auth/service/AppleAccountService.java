package com.chaekchaek.auth.service;

import com.chaekchaek.auth.oauth.apple.AppleTokenClient;
import com.chaekchaek.socialaccount.domain.Provider;
import com.chaekchaek.socialaccount.domain.SocialAccount;
import com.chaekchaek.socialaccount.repository.SocialAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppleAccountService {

    private final SocialAccountRepository socialAccountRepository;
    private final AppleTokenClient appleTokenClient;

    public AppleAccountService(
            SocialAccountRepository socialAccountRepository,
            AppleTokenClient appleTokenClient
    ) {
        this.socialAccountRepository = socialAccountRepository;
        this.appleTokenClient = appleTokenClient;
    }

    @Transactional
    public void revokeIfConnected(Long memberId) {
        socialAccountRepository.findByMemberIdAndProvider(memberId, Provider.APPLE)
                .map(SocialAccount::getProviderRefreshToken)
                .filter(token -> !token.isBlank())
                .ifPresent(appleTokenClient::revoke);
    }
}
