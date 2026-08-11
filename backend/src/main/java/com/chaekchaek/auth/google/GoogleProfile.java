package com.chaekchaek.auth.google;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public record GoogleProfile(
        String providerUserId,
        String email,
        String profileImageUrl
) {
    public static GoogleProfile from(OidcUser oidcUser) {
        return new GoogleProfile(
                oidcUser.getSubject(),
                oidcUser.getEmail(),
                oidcUser.getPicture()
        );
    }
}
