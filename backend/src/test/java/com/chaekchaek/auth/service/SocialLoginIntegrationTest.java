package com.chaekchaek.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.chaekchaek.auth.oauth.google.GoogleProfile;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import com.chaekchaek.socialaccount.domain.Provider;
import com.chaekchaek.socialaccount.domain.SocialAccount;
import com.chaekchaek.socialaccount.repository.SocialAccountRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-client-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-client-secret",
        "app.frontend.base-url=http://localhost:3000"
})
public class SocialLoginIntegrationTest {

    @Autowired
    private SocialLoginService socialLoginService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("최초 Google 로그인 시 회원과 소셜 계정을 생성한다")
    void should_CreateMemberAndSocialAccount_When_FirstGoogleLogin() {
        // given
        GoogleProfile profile = new GoogleProfile(
                "google-user-123",
                "member@example.com",
                "exUrl"
        );

        // when
        Member member = socialLoginService.loginOrSignUp(profile);

        // then
        SocialAccount socialAccount = socialAccountRepository
                .findByProviderAndProviderUserId(
                        Provider.GOOGLE,
                        profile.providerUserId()
                )
                .orElseThrow();

        assertAll(
                () -> assertThat(member.getId()).isNotNull(),
                () -> assertThat(memberRepository.count()).isEqualTo(1),
                () -> assertThat(socialAccountRepository.count()).isEqualTo(1),
                () -> assertThat(socialAccount.getMember().getId()).isEqualTo(member.getId()),
                () -> assertThat(socialAccount.getProvider()).isEqualTo(Provider.GOOGLE),
                () -> assertThat(socialAccount.getProviderUserId()).isEqualTo(profile.providerUserId())
        );
    }

    @Test
    @DisplayName("같은 Google 계정으로 다시 로그인해도 회원을 중복 생성하지 않는다")
    void should_NotCreateDuplicateMember_When_GoogleLoginIsRepeated() {
        // given
        GoogleProfile profile = new GoogleProfile(
                "google-user-123",
                "member@example.com",
                "exUrl"
        );

        // when
        Member firstLoginMember = socialLoginService.loginOrSignUp(profile);
        Long firstMemberId = firstLoginMember.getId();
        entityManager.flush();
        entityManager.clear();
        Member secondLoginMember = socialLoginService.loginOrSignUp(profile);

        // then
        assertAll(
                () -> assertThat(secondLoginMember.getId()).isEqualTo(firstLoginMember.getId()),
                () -> assertThat(memberRepository.count()).isEqualTo(1),
                () -> assertThat(socialAccountRepository.count()).isEqualTo(1),
                () -> assertThat(secondLoginMember.getId()).isEqualTo(firstMemberId)
        );
    }
}
