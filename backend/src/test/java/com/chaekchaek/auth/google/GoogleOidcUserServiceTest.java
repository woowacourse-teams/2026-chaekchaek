package com.chaekchaek.auth.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chaekchaek.auth.service.SocialLoginService;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.domain.MemberType;
import java.time.LocalDateTime;
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
    @DisplayName("Google_OIDC_사용자를_회원으로_조회하거나_가입시킨다")
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

        Member member = Member.create(
                MemberType.MEMBER,
                "책책-1234",
                "exUrl",
                LocalDateTime.of(2026, 8, 12, 12, 0)
        );

        GoogleProfile expectedProfile = new GoogleProfile(
                "google-user-123",
                "member@example.com",
                "exUrl"
        );

        when(socialLoginService.loginOrSignUp(expectedProfile))
                .thenReturn(member);

        // when
        OidcUser result = googleOidcUserService.loadUser(userRequest);

        // then
        assertThat(result).isSameAs(oidcUser);
        verify(socialLoginService).loginOrSignUp(expectedProfile);
    }
}
