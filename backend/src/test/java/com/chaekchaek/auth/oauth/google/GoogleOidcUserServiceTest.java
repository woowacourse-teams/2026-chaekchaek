package com.chaekchaek.auth.oauth.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chaekchaek.auth.principal.AuthenticatedMember;
import com.chaekchaek.auth.service.SocialLoginService;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.domain.MemberType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@ExtendWith(MockitoExtension.class)
public class GoogleOidcUserServiceTest {

    @Mock
    private OidcUserService delegate;

    @Mock
    private SocialLoginService socialLoginService;

    @Mock
    private OidcUserRequest userRequest;

    @Mock
    private ClientRegistration clientRegistration;

    @Mock
    private OidcUser oidcUser;

    @InjectMocks
    private GoogleOidcUserService googleOidcUserService;

    @Test
    @DisplayName("Google OIDC 사용자를 회원으로 조회하거나 가입시킨다")
    void should_FindOrRegister_When_GoogleOidcUser() {
        // given
        when(userRequest.getClientRegistration())
                .thenReturn(clientRegistration);
        when(clientRegistration.getRegistrationId())
                .thenReturn("google");

        when(delegate.loadUser(userRequest)).thenReturn(oidcUser);
        when(oidcUser.getSubject()).thenReturn("google-user-123");
        when(oidcUser.getEmail()).thenReturn("member@example.com");
        when(oidcUser.getPicture()).thenReturn("exUrl");

        GoogleProfile expectedProfile = new GoogleProfile(
                "google-user-123",
                "member@example.com",
                "exUrl"
        );

        Member member = mock(Member.class);
        when(member.getId()).thenReturn(1L);
        when(member.getType()).thenReturn(MemberType.MEMBER);
        when(oidcUser.getName()).thenReturn("google-user-123");

        when(socialLoginService.loginOrSignUp(expectedProfile))
                .thenReturn(member);

        // when
        OidcUser result = googleOidcUserService.loadUser(userRequest);

        // then
        assertThat(result).isInstanceOf(AuthenticatedMember.class);

        AuthenticatedMember principal = (AuthenticatedMember) result;

        assertThat(principal.getMemberId()).isEqualTo(1L);
        assertThat(principal.getMemberType()).isEqualTo(MemberType.MEMBER);
        assertThat(principal.getName()).isEqualTo("google-user-123");

        verify(socialLoginService).loginOrSignUp(expectedProfile);
    }
}
