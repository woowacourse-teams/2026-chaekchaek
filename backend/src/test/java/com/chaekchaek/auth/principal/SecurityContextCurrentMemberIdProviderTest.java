package com.chaekchaek.auth.principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.member.domain.AccountStatus;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
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
                .isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED)
        );
    }

    @Test
    @DisplayName("JWT subject가 회원 ID 형식이 아니면 인증되지 않은 것으로 처리한다")
    void should_NotFindCurrentMember_When_JwtSubjectIsInvalid() {
        // given
        authenticate("not-a-member-id");

        // when & then
        assertThat(provider.findCurrentMemberId()).isEmpty();
    }

    @Test
    @DisplayName("활성 회원의 쓰기 인증은 회원 ID를 반환한다")
    void should_ProvideWriteMemberId_When_MemberIsActive() {
        // given
        authenticate("42");
        when(memberRepository.findById(42L)).thenReturn(Optional.of(
                Member.create("익명 참새", null, LocalDateTime.now())
        ));

        // when & then
        assertThat(provider.getCurrentMemberIdForWrite()).isEqualTo(42L);
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = {"SUSPENDED", "WITHDRAWN"})
    @DisplayName("비활성 회원의 쓰기는 권한 오류이며 기존 회원 조회는 인증 오류를 유지한다")
    void should_DistinguishInactiveWrite_When_LegacyLookupRemainsUnauthorized(AccountStatus status) {
        // given
        Member member = Member.create("익명 참새", null, LocalDateTime.now());
        ReflectionTestUtils.setField(member, "accountStatus", status);
        authenticate("42");
        when(memberRepository.findById(42L)).thenReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(provider::getCurrentMemberIdForWrite)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        assertThatThrownBy(provider::getCurrentMemberId)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
        assertThatThrownBy(provider::findCurrentMemberId)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("존재하지 않는 회원의 쓰기 인증을 거절한다")
    void should_RejectWriteAuthentication_When_MemberDoesNotExist() {
        // given
        authenticate("42");
        when(memberRepository.findById(42L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(provider::getCurrentMemberIdForWrite)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-subject", "9223372036854775808"})
    @DisplayName("숫자 회원 ID로 해석할 수 없는 JWT의 쓰기를 거절한다")
    void should_RejectWriteAuthentication_When_SubjectIsInvalid(String subject) {
        // given
        authenticate(subject);

        // when & then
        assertThatThrownBy(provider::getCurrentMemberIdForWrite)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
        assertThat(provider.findCurrentMemberId()).isEmpty();
    }

    @Test
    @DisplayName("인증이 없거나 JWT 인증이 아니면 쓰기를 거절한다")
    void should_RejectWriteAuthentication_When_JwtAuthenticationIsMissing() {
        // when & then
        assertThatThrownBy(provider::getCurrentMemberIdForWrite)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));

        // given
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("member", "password", List.of())
        );

        // when & then
        assertThatThrownBy(provider::getCurrentMemberIdForWrite)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
        assertThat(provider.findCurrentMemberId()).isEmpty();
    }

    @Test
    @DisplayName("검증되지 않은 JWT 인증의 쓰기를 거절한다")
    void should_RejectWriteAuthentication_When_JwtIsNotAuthenticated() {
        // given
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("42").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        // when & then
        assertThatThrownBy(provider::getCurrentMemberIdForWrite)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    private void authenticate(String subject) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    @Test
    void should_RejectExistingAccessToken_When_MemberIsWithdrawn() {
        // given
        Member member = Member.create("익명 참새", null, LocalDateTime.now());
        member.withdraw(LocalDateTime.now());
        when(memberRepository.findById(42L)).thenReturn(Optional.of(member));
        authenticate("42");

        // when & then
        assertThatThrownBy(provider::getCurrentMemberId)
                .isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED)
        );
    }
}
