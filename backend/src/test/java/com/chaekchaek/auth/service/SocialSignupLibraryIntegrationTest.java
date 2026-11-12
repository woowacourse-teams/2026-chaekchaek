package com.chaekchaek.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.chaekchaek.actor.domain.Actor;
import com.chaekchaek.actor.repository.ActorRepository;
import com.chaekchaek.auth.oauth.apple.AppleProfile;
import com.chaekchaek.auth.oauth.google.GoogleProfile;
import com.chaekchaek.library.domain.Library;
import com.chaekchaek.library.domain.LibrarySort;
import com.chaekchaek.library.repository.LibraryRepository;
import com.chaekchaek.library.service.LibraryService;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import com.chaekchaek.member.service.MemberService;
import com.chaekchaek.socialaccount.domain.Provider;
import com.chaekchaek.socialaccount.repository.SocialAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:social-signup-library;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@Transactional
@Import(SocialSignupLibraryIntegrationTest.FixedClockConfiguration.class)
class SocialSignupLibraryIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-11-10T03:00:00Z");

    @Autowired
    private SocialLoginService socialLoginService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private LibraryService libraryService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ActorRepository actorRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @MockitoSpyBean
    private LibraryRepository libraryRepository;

    @ParameterizedTest
    @EnumSource(value = Provider.class, names = {"GOOGLE", "APPLE"})
    @DisplayName("공개 닉네임을 입력하지 않아도 소셜 가입 시 기본 책장을 생성하고 조회한다")
    void should_CreateDefaultLibrary_When_SocialMemberSignsUp(Provider provider) {
        // given & when
        Member member = login(provider);

        // then
        Library library = libraryRepository.findByMemberIdAndIsDefaultTrue(member.getId()).orElseThrow();
        assertThat(member.getNickname()).isNull();
        assertThat(member.isDisplayAnonymous()).isTrue();
        assertThat(library.getName()).isEqualTo("기본 책장");
        assertThat(library.getDisplayOrder()).isEqualTo(1L);
        assertThat(libraryRepository.findAllByMemberIdOrderByDisplayOrderAscIdAsc(member.getId()))
                .hasSize(1);
        assertThat(libraryService.getLibrary(member.getId(), 1, LibrarySort.RECENT).items()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = Provider.class, names = {"GOOGLE", "APPLE"})
    @DisplayName("같은 소셜 계정으로 다시 로그인하면 변경한 기본 책장을 유지하고 중복 생성하지 않는다")
    void should_KeepDefaultLibrary_When_SocialLoginIsRepeated(Provider provider) {
        // given
        Member member = login(provider);
        Library library = libraryRepository.findByMemberIdAndIsDefaultTrue(member.getId()).orElseThrow();
        library.rename("내가정한이름", NOW);
        libraryRepository.flush();

        // when
        Member loggedIn = login(provider);

        // then
        assertThat(loggedIn.getId()).isEqualTo(member.getId());
        assertThat(libraryRepository.findAllByMemberIdOrderByDisplayOrderAscIdAsc(member.getId()))
                .singleElement()
                .satisfies(saved -> {
                    assertThat(saved.getId()).isEqualTo(library.getId());
                    assertThat(saved.getName()).isEqualTo("내가정한이름");
                    assertThat(saved.isDefault()).isTrue();
                });
    }

    @ParameterizedTest
    @EnumSource(value = Provider.class, names = {"GOOGLE", "APPLE"})
    @DisplayName("탈퇴 후 소셜 재가입하면 새 회원의 기본 책장을 만들고 이전 책장을 보존한다")
    void should_CreateNewDefaultLibrary_When_WithdrawnMemberRejoins(Provider provider) {
        // given
        Member previous = login(provider);
        Library previousLibrary = libraryRepository.findByMemberIdAndIsDefaultTrue(previous.getId()).orElseThrow();
        previousLibrary.rename("이전책장", NOW);
        previous.withdraw(LocalDateTime.of(2026, 11, 10, 3, 0));
        memberRepository.flush();

        // when
        Member rejoined = login(provider);

        // then
        Library newLibrary = libraryRepository.findByMemberIdAndIsDefaultTrue(rejoined.getId()).orElseThrow();
        assertThat(rejoined.getId()).isNotEqualTo(previous.getId());
        assertThat(rejoined.getNickname()).isNull();
        assertThat(newLibrary.getId()).isNotEqualTo(previousLibrary.getId());
        assertThat(newLibrary.getName()).isEqualTo("기본 책장");
        assertThat(newLibrary.getDisplayOrder()).isEqualTo(1L);
        assertThat(previousLibrary.getName()).isEqualTo("이전책장");
        assertThat(libraryRepository.findById(previousLibrary.getId())).isPresent();
    }

    @Test
    @DisplayName("게스트가 소셜 가입하면 기존 Actor를 계승하고 기본 책장도 생성한다")
    void should_CreateDefaultLibrary_When_GuestBecomesMember() {
        // given
        Actor guest = actorRepository.saveAndFlush(Actor.guest(
                "a".repeat(64),
                "게스트 참새",
                LocalDateTime.ofInstant(NOW, ZoneOffset.UTC).minusDays(1),
                LocalDateTime.ofInstant(NOW, ZoneOffset.UTC).plusDays(29)
        ));

        // when
        Member member = socialLoginService.loginOrSignUp(googleProfile(), guest.getId());

        // then
        assertThat(actorRepository.findByMemberId(member.getId())).get()
                .extracting(Actor::getId).isEqualTo(guest.getId());
        assertThat(member.getAnonymousNickname()).isEqualTo("게스트 참새");
        assertThat(libraryRepository.findByMemberIdAndIsDefaultTrue(member.getId())).isPresent();
    }

    @Test
    @DisplayName("가입 후 첫 공개 닉네임을 저장해도 기본 책장의 이름과 개수를 유지한다")
    void should_KeepDefaultLibrary_When_FirstNicknameIsSavedAfterSignup() {
        // given
        Member member = login(Provider.GOOGLE);
        Library library = libraryRepository.findByMemberIdAndIsDefaultTrue(member.getId()).orElseThrow();

        // when
        memberService.updateNickname(member.getId(), "책읽는참새");

        // then
        assertThat(libraryRepository.findAllByMemberIdOrderByDisplayOrderAscIdAsc(member.getId()))
                .singleElement()
                .satisfies(saved -> {
                    assertThat(saved.getId()).isEqualTo(library.getId());
                    assertThat(saved.getName()).isEqualTo("기본 책장");
                });
    }

    @Test
    @DisplayName("공개 닉네임이 없어도 가입 직후 추가 책장을 생성한다")
    void should_CreateAdditionalLibrary_When_NicknameIsNotSet() {
        // given
        Member member = login(Provider.GOOGLE);

        // when
        var additional = libraryService.create(member.getId(), "올해읽을책");

        // then
        assertThat(additional.isDefault()).isFalse();
        assertThat(libraryRepository.findAllByMemberIdOrderByDisplayOrderAscIdAsc(member.getId()))
                .extracting(Library::getName).containsExactly("기본 책장", "올해읽을책");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("기본 책장 저장에 실패하면 회원과 Actor와 소셜 계정을 함께 롤백한다")
    void should_RollBackSignup_When_DefaultLibraryCreationFails() {
        // given
        doThrow(new IllegalStateException("library storage unavailable"))
                .when(libraryRepository).create(any(Library.class));

        // when & then
        assertThatThrownBy(() -> login(Provider.GOOGLE)).isInstanceOf(IllegalStateException.class);
        assertThat(memberRepository.count()).isZero();
        assertThat(actorRepository.count()).isZero();
        assertThat(socialAccountRepository.count()).isZero();
        assertThat(libraryRepository.count()).isZero();
    }

    private Member login(Provider provider) {
        if (provider == Provider.APPLE) {
            return socialLoginService.loginOrSignUp(new AppleProfile("signup-library-user"));
        }
        return socialLoginService.loginOrSignUp(googleProfile());
    }

    private GoogleProfile googleProfile() {
        return new GoogleProfile(
                "signup-library-user",
                "signup-library@example.com",
                null
        );
    }

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock signupTestClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
