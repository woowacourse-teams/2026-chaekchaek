package com.chaekchaek.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chaekchaek.actor.domain.Actor;
import com.chaekchaek.actor.repository.ActorRepository;
import com.chaekchaek.auth.oauth.google.GoogleProfile;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import com.chaekchaek.member.service.NicknameGenerator;
import com.chaekchaek.socialaccount.domain.Provider;
import com.chaekchaek.socialaccount.domain.SocialAccount;
import com.chaekchaek.socialaccount.repository.SocialAccountRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SocialLoginServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SocialAccountRepository socialAccountRepository;

    @Mock
    private NicknameGenerator nicknameGenerator;

    @Mock
    private ActorRepository actorRepository;

    @InjectMocks
    private SocialLoginService socialLoginService;

    @Test
    @DisplayName("기존 소셜 계정이 있으면 연결된 회원으로 로그인한다")
    void should_LoginMember_When_SocialAccountExists() {
        // given
        GoogleProfile googleProfile = new GoogleProfile(
                "google-user-123",
                "member@example.com",
                "exUrl"
        );

        Member existingMember = Member.create(
                "책책-1234",
                googleProfile.profileImageUrl(),
                LocalDateTime.of(2026, 8, 12, 12, 0)
        );

        SocialAccount existingAccount = SocialAccount.connect(
                existingMember,
                Provider.GOOGLE,
                googleProfile.providerUserId(),
                LocalDateTime.of(2026, 8, 12, 12, 0)
        );

        when(socialAccountRepository.findByProviderAndProviderUserId(
                Provider.GOOGLE,
                googleProfile.providerUserId()
        )).thenReturn(Optional.of(existingAccount));

        // when
        Member result = socialLoginService.loginOrSignUp(googleProfile);

        // then
        assertThat(result).isSameAs(existingMember);

        verify(memberRepository, never()).save(any(Member.class));
        verify(actorRepository, never()).save(any(Actor.class));
        verify(socialAccountRepository, never()).save(any(SocialAccount.class));
        verify(nicknameGenerator, never()).generate();
    }

    @Test
    @DisplayName("최초 소셜 로그인이면 회원과 소셜 계정을 생성한다")
    void should_CreateMemberAndSocialAccount_When_FirstSocialLogin() {
        // given
        GoogleProfile googleProfile = new GoogleProfile(
                "google-user-123",
                "member@example.com",
                "exUrl"
        );

        when(socialAccountRepository.findByProviderAndProviderUserId(
                Provider.GOOGLE,
                googleProfile.providerUserId()
        )).thenReturn(Optional.empty());

        when(nicknameGenerator.generate())
                .thenReturn("우아한 달빛 참새");

        // when
        Member result = socialLoginService.loginOrSignUp(googleProfile);

        // then
        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);

        ArgumentCaptor<SocialAccount> accountCaptor = ArgumentCaptor.forClass(SocialAccount.class);
        ArgumentCaptor<Actor> actorCaptor = ArgumentCaptor.forClass(Actor.class);

        verify(memberRepository).save(memberCaptor.capture());
        verify(actorRepository).save(actorCaptor.capture());
        verify(socialAccountRepository).save(accountCaptor.capture());

        Member savedMember = memberCaptor.getValue();
        SocialAccount savedAccount = accountCaptor.getValue();
        Actor savedActor = actorCaptor.getValue();

        assertAll(
                () -> assertThat(result).isSameAs(savedMember),
                () -> assertThat(savedMember.getNickname()).isNull(),
                () -> assertThat(savedMember.getProfileImageUrl()).isEqualTo(googleProfile.profileImageUrl()),
                () -> assertThat(savedMember.getAnonymousNickname()).isEqualTo("우아한 달빛 참새"),
                () -> assertThat(savedMember.isDisplayAnonymous()).isTrue(),
                () -> assertThat(savedActor.getMember()).isSameAs(savedMember),
                () -> assertThat(savedActor.getType()).isEqualTo(com.chaekchaek.common.auth.ActorType.MEMBER),

                () -> assertThat(savedAccount.getMember()).isSameAs(savedMember),
                () -> assertThat(savedAccount.getProvider()).isEqualTo(Provider.GOOGLE),
                () -> assertThat(savedAccount.getProviderUserId()).isEqualTo("google-user-123"),
                () -> assertThat(savedAccount.getConnectedAt()).isEqualTo(savedMember.getCreatedAt())
        );

    }
}
