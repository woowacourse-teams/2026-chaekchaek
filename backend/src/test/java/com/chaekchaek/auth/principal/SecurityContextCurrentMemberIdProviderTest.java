package com.chaekchaek.auth.principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class SecurityContextCurrentMemberIdProviderTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final SecurityContextCurrentMemberIdProvider provider =
            new SecurityContextCurrentMemberIdProvider(memberRepository);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("JWT subject에서 현재 회원 ID를 제공한다")
    void should_ProvideCurrentMemberId_When_JwtIsAuthenticated() {
        // given
        authenticate("42");
        when(memberRepository.findById(42L)).thenReturn(Optional.of(
                Member.create("익명 참새", null, LocalDateTime.now())));

        // when & then
        assertThat(provider.getCurrentMemberId()).isEqualTo(42L);
        assertThat(provider.findCurrentMemberId()).hasValue(42L);
    }

    @Test
    @DisplayName("인증 정보가 없으면 현재 회원을 찾지 못한다")
    void should_NotFindCurrentMember_When_AuthenticationIsMissing() {
        // when & then
        assertThat(provider.findCurrentMemberId()).isEmpty();
        assertThatThrownBy(provider::getCurrentMemberId)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("JWT subject가 회원 ID 형식이 아니면 인증되지 않은 것으로 처리한다")
    void should_NotFindCurrentMember_When_JwtSubjectIsInvalid() {
        // given
        authenticate("not-a-member-id");

        // when & then
        assertThat(provider.findCurrentMemberId()).isEmpty();
    }

    private void authenticate(String subject) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    @Test
    void rejectsWithdrawnMemberWithExistingAccessToken() {
        Member member = Member.create("익명 참새", null, LocalDateTime.now());
        member.withdraw(LocalDateTime.now());
        when(memberRepository.findById(42L)).thenReturn(Optional.of(member));
        authenticate("42");
        assertThatThrownBy(provider::getCurrentMemberId)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }
}
