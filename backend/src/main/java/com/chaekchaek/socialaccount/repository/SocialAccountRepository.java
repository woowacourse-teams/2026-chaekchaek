package com.chaekchaek.socialaccount.repository;

import com.chaekchaek.socialaccount.domain.Provider;
import com.chaekchaek.socialaccount.domain.SocialAccount;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from SocialAccount account where account.provider = :provider "
            + "and account.providerUserId = :providerUserId")
    Optional<SocialAccount> findForLogin(
            @Param("provider") Provider provider,
            @Param("providerUserId") String providerUserId
    );

    Optional<SocialAccount> findByProviderAndProviderUserId(Provider provider, String providerUserId);

    Optional<SocialAccount> findByMemberIdAndProvider(Long memberId, Provider provider);
}
