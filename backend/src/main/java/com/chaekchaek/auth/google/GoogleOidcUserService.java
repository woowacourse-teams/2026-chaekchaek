package com.chaekchaek.auth.google;

import com.chaekchaek.auth.principal.AuthenticatedMember;
import com.chaekchaek.auth.service.SocialLoginService;
import com.chaekchaek.member.domain.Member;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
public class GoogleOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private static final String UNSUPPORTED_REGISTRATION_ID_ERROR_MESSAGE =
            "[ERROR] 지원하지 않는 OIDC 제공자입니다. 올바른 제공자를 사용해주세요: ";
    private static final String GOOGLE_REGISTRATION_ID = "google";

    private final OidcUserService delegate;
    private final SocialLoginService socialLoginService;

    public GoogleOidcUserService(OidcUserService delegate, SocialLoginService socialLoginService) {
        this.delegate = delegate;
        this.socialLoginService = socialLoginService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        validateGoogleRegistration(userRequest);

        OidcUser oidcUser = delegate.loadUser(userRequest);
        GoogleProfile googleProfile = GoogleProfile.from(oidcUser);

        Member member = socialLoginService.loginOrSignUp(googleProfile);

        return AuthenticatedMember.of(member, oidcUser);
    }

    private void validateGoogleRegistration(OidcUserRequest userRequest) {
        String registrationId = userRequest
                .getClientRegistration()
                .getRegistrationId();

        if (!GOOGLE_REGISTRATION_ID.equals(registrationId)) {
            throw new OAuth2AuthenticationException(UNSUPPORTED_REGISTRATION_ID_ERROR_MESSAGE + registrationId);
        }
    }
}
