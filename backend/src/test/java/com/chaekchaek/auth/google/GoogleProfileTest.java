package com.chaekchaek.auth.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public class GoogleProfileTest {

    @Test
    @DisplayName("OIDC 사용자 정보를 Google 프로필로 변환한다")
    void should_ConvertFromOidcUser() {
        // given
        OidcUser oidcUser = org.mockito.Mockito.mock(OidcUser.class);

        when(oidcUser.getSubject()).thenReturn("google-user-123");
        when(oidcUser.getEmail()).thenReturn("member@example.com");
        when(oidcUser.getPicture()).thenReturn("exUrl");

        // when
        GoogleProfile result = GoogleProfile.from(oidcUser);

        // then
        assertAll(
                () -> assertThat(result.providerUserId()).isEqualTo("google-user-123"),
                () -> assertThat(result.email()).isEqualTo("member@example.com"),
                () -> assertThat(result.profileImageUrl()).isEqualTo("exUrl")
        );
    }
}
