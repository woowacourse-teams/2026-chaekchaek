package com.chaekchaek.auth.oauth.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chaekchaek.auth.principal.AuthenticatedMember;
import com.chaekchaek.auth.oauth.OAuthGuestContextService;
import com.chaekchaek.auth.service.SocialLoginService;
import jakarta.servlet.http.HttpServletRequest;
import com.chaekchaek.member.domain.Member;
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
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class GoogleOidcUserServiceTest {

    @Mock
    private OidcUserService delegate;

    @Mock
    private SocialLoginService socialLoginService;

    @Mock
    private OAuthGuestContextService guestContextService;

    @Mock
    private HttpServletRequest request;

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
        when(oidcUser.getName()).thenReturn("google-user-123");

        when(guestContextService.findGuestActorId(request)).thenReturn(Optional.empty());
        when(socialLoginService.loginOrSignUp(expectedProfile, null))
                .thenReturn(member);

        // when
        OidcUser result = googleOidcUserService.loadUser(userRequest);

        // then
        assertThat(result).isInstanceOf(AuthenticatedMember.class);

        AuthenticatedMember principal = (AuthenticatedMember) result;

        assertThat(principal.getMemberId()).isEqualTo(1L);
        assertThat(principal.getName()).isEqualTo("google-user-123");

        verify(socialLoginService).loginOrSignUp(expectedProfile, null);
        verify(guestContextService).clear(request);
    }

    @Test
    @DisplayName("OAuth 세션의 게스트 Actor를 소셜 로그인 서비스에 전달한다")
    void should_PassGuestActorFromOAuthContext() {
        when(userRequest.getClientRegistration()).thenReturn(clientRegistration);
        when(clientRegistration.getRegistrationId()).thenReturn("google");
        when(delegate.loadUser(userRequest)).thenReturn(oidcUser);
        when(oidcUser.getSubject()).thenReturn("google-user-123");
        when(oidcUser.getEmail()).thenReturn("member@example.com");
        when(oidcUser.getPicture()).thenReturn("exUrl");
        when(guestContextService.findGuestActorId(request)).thenReturn(Optional.of(7L));

        Member member = mock(Member.class);
        when(member.getId()).thenReturn(1L);
        when(socialLoginService.loginOrSignUp(
                new GoogleProfile("google-user-123", "member@example.com", "exUrl"), 7L))
                .thenReturn(member);

        googleOidcUserService.loadUser(userRequest);

        verify(socialLoginService).loginOrSignUp(
                new GoogleProfile("google-user-123", "member@example.com", "exUrl"), 7L);
        verify(guestContextService).clear(request);
    }
}
