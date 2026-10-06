package com.chaekchaek.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chaekchaek.actor.domain.Actor;
import com.chaekchaek.actor.repository.ActorRepository;
import com.chaekchaek.auth.oauth.google.GoogleProfile;
import com.chaekchaek.common.auth.CurrentActor;
import com.chaekchaek.common.auth.CurrentActorProvider;
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
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
public class SocialLoginServiceTest {

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = Provider.class, names = {"GOOGLE", "APPLE"})
    @DisplayName("Google과 Apple 모두 반복 탈퇴 후 재가입 횟수를 제한하지 않는다")
    void allowsRepeatedRejoinForEachProvider(Provider provider) {
        Member member = Member.create("기존 익명", null, LocalDateTime.now());
        SocialAccount account = SocialAccount.connect(member, provider, "repeat-user", LocalDateTime.now());
        when(socialAccountRepository.findForLogin(provider, "repeat-user"))
                .thenReturn(Optional.of(account));
        when(nicknameGenerator.generate()).thenReturn("새 익명");
        for (int i = 0; i < 3; i++) {
            Member previous = account.getMember();
            previous.withdraw(LocalDateTime.now());
            Member result = provider == Provider.GOOGLE
                    ? socialLoginService.loginOrSignUp(new GoogleProfile("repeat-user", "email", null), 7L)
                    : socialLoginService.loginOrSignUp(new com.chaekchaek.auth.oauth.apple.AppleProfile("repeat-user"));
            assertThat(result).isNotSameAs(previous);
            assertThat(result.getAccountStatus()).isEqualTo(com.chaekchaek.member.domain.AccountStatus.ACTIVE);
            assertThat(account.getMember()).isSameAs(result);
            if (provider == Provider.GOOGLE) {
                verify(guestActorMigrationService).migrate(7L, result);
            }
        }
        verify(actorRepository, org.mockito.Mockito.times(3)).save(any(Actor.class));
    }

    @Test
    @DisplayName("탈퇴 후 동일 소셜 계정 재가입은 새 회원과 Actor를 만들고 기존 회원을 보존한다")
    void createsNewIdentityAfterWithdrawal() {
        GoogleProfile profile = new GoogleProfile("returning-user", "member@example.com", "new-image");
        Member oldMember = Member.create("기존 익명 이름", "old-image", LocalDateTime.now());
        oldMember.updateNickname("기존 공개 이름");
        oldMember.disableAnonymousDisplay();
        oldMember.withdraw(LocalDateTime.now());
        Actor oldActor = Actor.member(oldMember, LocalDateTime.now());
        SocialAccount account = SocialAccount.connect(oldMember, Provider.GOOGLE,
                profile.providerUserId(), LocalDateTime.now());
        account.updateProviderRefreshToken("old-provider-token");
        when(socialAccountRepository.findForLogin(Provider.GOOGLE,
                profile.providerUserId())).thenReturn(Optional.of(account));
        when(nicknameGenerator.generate()).thenReturn("새 익명 이름");

        Member rejoined = socialLoginService.loginOrSignUp(profile);

        assertThat(rejoined).isNotSameAs(oldMember);
        assertThat(account.getMember()).isSameAs(rejoined);
        assertThat(account.getProviderRefreshToken()).isNull();
        assertThat(oldActor.getMember()).isSameAs(oldMember);
        assertThat(oldMember.getAccountStatus()).isEqualTo(com.chaekchaek.member.domain.AccountStatus.WITHDRAWN);
        assertThat(oldMember.getAnonymousNickname()).isEqualTo("기존 익명 이름");
        assertThat(rejoined.getAccountStatus()).isEqualTo(com.chaekchaek.member.domain.AccountStatus.ACTIVE);
        assertThat(rejoined.getAnonymousNickname()).isEqualTo("새 익명 이름");
        assertThat(rejoined.getNickname()).isNull();
        assertThat(rejoined.isDisplayAnonymous()).isTrue();
        ArgumentCaptor<Actor> actorCaptor = ArgumentCaptor.forClass(Actor.class);
        verify(actorRepository).save(actorCaptor.capture());
        assertThat(actorCaptor.getValue()).isNotSameAs(oldActor);
        assertThat(actorCaptor.getValue().getMember()).isSameAs(rejoined);
        rejoined.updateNickname("새 공개 이름");
        rejoined.disableAnonymousDisplay();
        assertThat(rejoined.getDisplayName()).isEqualTo("새 공개 이름");
        assertThat(socialLoginService.loginOrSignUp(profile)).isSameAs(rejoined);
        verify(socialAccountRepository, never()).save(any(SocialAccount.class));
    }

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SocialAccountRepository socialAccountRepository;

    @Mock
    private NicknameGenerator nicknameGenerator;

    @Mock
    private ActorRepository actorRepository;

    @Mock
    private CurrentActorProvider currentActorProvider;

    @Mock
    private GuestActorMigrationService guestActorMigrationService;

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

        when(socialAccountRepository.findForLogin(
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

        when(socialAccountRepository.findForLogin(
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

    @Test
    @DisplayName("게스트가 최초 소셜 로그인하면 기존 Actor와 닉네임을 회원에게 계승한다")
    void should_InheritGuestActorAndNickname_When_GuestSignsUp() {
        GoogleProfile googleProfile = new GoogleProfile(
                "google-user-123",
                "member@example.com",
                "exUrl"
        );
        LocalDateTime now = LocalDateTime.now();
        Actor guestActor = Actor.guest("a".repeat(64), "게스트 참새", now.minusDays(1), now.plusDays(29));

        when(socialAccountRepository.findForLogin(
                Provider.GOOGLE,
                googleProfile.providerUserId()
        )).thenReturn(Optional.empty());
        when(currentActorProvider.findCurrentActor()).thenReturn(Optional.of(CurrentActor.guest(7L)));
        when(actorRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(guestActor));
        doAnswer(invocation -> {
            guestActor.convertToMember(invocation.getArgument(1));
            return null;
        }).when(guestActorMigrationService).migrate(eq(7L), any(Member.class));

        Member result = socialLoginService.loginOrSignUp(googleProfile);

        assertAll(
                () -> assertThat(result.getAnonymousNickname()).isEqualTo("게스트 참새"),
                () -> assertThat(guestActor.getType()).isEqualTo(com.chaekchaek.common.auth.ActorType.MEMBER),
                () -> assertThat(guestActor.getMember()).isSameAs(result),
                () -> assertThat(guestActor.getGuestTokenHash()).isNull(),
                () -> assertThat(guestActor.getGuestNickname()).isNull()
        );
        verify(nicknameGenerator, never()).generate();
        verify(actorRepository, never()).save(any(Actor.class));
        verify(memberRepository).save(result);
        verify(socialAccountRepository).save(any(SocialAccount.class));
    }

    @Test
    @DisplayName("기존 회원이 소셜 로그인하면 제공자와 함께 로그인 로그를 남긴다")
    void should_LogLogin_When_SocialAccountExists(CapturedOutput output) {
        // given
        GoogleProfile googleProfile = new GoogleProfile("google-user-123", "member@example.com", "exUrl");
        Member existingMember = Member.create("책책-1234", "exUrl", LocalDateTime.of(2026, 8, 12, 12, 0));
        SocialAccount existingAccount = SocialAccount.connect(
                existingMember, Provider.GOOGLE, googleProfile.providerUserId(), LocalDateTime.of(2026, 8, 12, 12, 0));
        when(socialAccountRepository.findByProviderAndProviderUserId(Provider.GOOGLE, googleProfile.providerUserId()))
                .thenReturn(Optional.of(existingAccount));

        // when
        socialLoginService.loginOrSignUp(googleProfile);

        // then
        assertAll(
                () -> assertThat(output).contains("Social login: provider=GOOGLE"),
                () -> assertThat(output).doesNotContain("google-user-123"),
                () -> assertThat(output).doesNotContain("member@example.com")
        );
    }

    @Test
    @DisplayName("최초 소셜 로그인이면 가입 로그를 남긴다")
    void should_LogSignUp_When_FirstSocialLogin(CapturedOutput output) {
        // given
        GoogleProfile googleProfile = new GoogleProfile("google-user-123", "member@example.com", "exUrl");
        when(socialAccountRepository.findByProviderAndProviderUserId(Provider.GOOGLE, googleProfile.providerUserId()))
                .thenReturn(Optional.empty());
        when(nicknameGenerator.generate()).thenReturn("우아한 달빛 참새");

        // when
        socialLoginService.loginOrSignUp(googleProfile);

        // then
        assertThat(output).contains("Social sign up: provider=GOOGLE", "fromGuest=false");
    }
}
