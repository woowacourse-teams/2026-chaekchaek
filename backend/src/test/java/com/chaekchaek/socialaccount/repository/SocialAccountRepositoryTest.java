package com.chaekchaek.socialaccount.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import com.chaekchaek.socialaccount.domain.Provider;
import com.chaekchaek.socialaccount.domain.SocialAccount;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
public class SocialAccountRepositoryTest {

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("소셜 계정을 회원과 연결하여 저장하고 조회한다")
    void should_ReturnMyInfo_When_AccessTokenIsValid() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 8, 11, 12, 0);

        Member member = Member.create(
                "우아한 참새",
                "exUrl",
                now
        );
        Member savedMember = memberRepository.save(member);

        SocialAccount socialAccount = SocialAccount.connect(
                savedMember,
                Provider.GOOGLE,
                "google-member-123",
                now
        );
        socialAccountRepository.save(socialAccount);

        entityManager.flush();
        entityManager.clear();

        // when
        SocialAccount foundAccount = socialAccountRepository
                .findByProviderAndProviderUserId(
                        Provider.GOOGLE,
                        "google-member-123"
                )
                .orElseThrow();

        // then
        assertAll(
                () -> assertThat(foundAccount.getId()).isNotNull(),
                () -> assertThat(foundAccount.getProvider()).isEqualTo(Provider.GOOGLE),
                () -> assertThat(foundAccount.getProviderUserId()).isEqualTo("google-member-123"),
                () -> assertThat(foundAccount.getConnectedAt()).isEqualTo(now),
                () -> assertThat(foundAccount.getMember().getId()).isEqualTo(savedMember.getId())
        );
    }

    @Test
    @DisplayName("동일 제공자의 동일 사용자의 중복 연결은 거부된다")
    void should_Reject_When_ProviderAccountDuplicated() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 8, 11, 12, 0);

        Member firstMember = memberRepository.save(
                Member.create(
                        "참새1",
                        null,
                        now
                )
        );

        Member secondMember = memberRepository.save(
                Member.create(
                        "참새2",
                        null,
                        now
                )
        );

        socialAccountRepository.saveAndFlush(
                SocialAccount.connect(
                        firstMember,
                        Provider.GOOGLE,
                        "same-google-member",
                        now
                )
        );

        // when & then
        assertThatThrownBy(() -> socialAccountRepository.saveAndFlush(
                SocialAccount.connect(
                        secondMember,
                        Provider.GOOGLE,
                        "same-google-member",
                        now
                )
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("제공자가 다르면 동일한 사용자 ID 연결을 성공한다")
    void should_Allow_When_SameUserIdFromDifferentProviders() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 8, 11, 12, 0);

        Member member = memberRepository.save(
                Member.create(
                        "우아한 참새",
                        null,
                        now
                )
        );

        socialAccountRepository.save(
                SocialAccount.connect(
                        member,
                        Provider.GOOGLE,
                        "same-member-id",
                        now
                )
        );

        socialAccountRepository.save(
                SocialAccount.connect(
                        member,
                        Provider.KAKAO,
                        "same-member-id",
                        now
                )
        );

        // when
        socialAccountRepository.flush();

        // then
        assertThat(socialAccountRepository.count()).isEqualTo(2);
    }
}
