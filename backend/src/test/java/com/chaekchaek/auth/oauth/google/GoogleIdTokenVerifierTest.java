package com.chaekchaek.auth.oauth.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

import com.chaekchaek.auth.exception.InvalidGoogleIdTokenException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

@ExtendWith(MockitoExtension.class)
class GoogleIdTokenVerifierTest {

    @Mock
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("유효한 Google ID Token을 프로필로 변환한다")
    void should_ReturnGoogleProfile_When_IdTokenIsValid() {
        // given
        GoogleIdTokenVerifier verifier =
                new GoogleIdTokenVerifier(jwtDecoder);

        Jwt jwt = createJwt(
                "google-user-id",
                "user@example.com",
                "exUrl"
        );

        when(jwtDecoder.decode("valid-id-token"))
                .thenReturn(jwt);

        // when
        GoogleProfile profile =
                verifier.verify("valid-id-token");

        // then
        assertAll(
                () -> assertThat(profile.providerUserId()).isEqualTo("google-user-id"),
                () -> assertThat(profile.email()).isEqualTo("user@example.com"),
                () -> assertThat(profile.profileImageUrl()).isEqualTo("exUrl")
        );
    }

    @Test
    @DisplayName("Google ID Token 검증에 실패하면 인증 예외가 발생한다")
    void should_ThrowException_When_IdTokenIsInvalid() {
        // given
        GoogleIdTokenVerifier verifier =
                new GoogleIdTokenVerifier(jwtDecoder);

        when(jwtDecoder.decode("invalid-id-token"))
                .thenThrow(new JwtException(
                        "invalid token"
                ));

        // when & then
        assertThatThrownBy(
                () -> verifier.verify("invalid-id-token")
        ).isInstanceOf(
                InvalidGoogleIdTokenException.class
        );
    }

    @Test
    @DisplayName("subject가 없으면 Google ID Token 검증에 실패한다")
    void should_ThrowException_When_SubjectIsMissing() {
        // given
        GoogleIdTokenVerifier verifier =
                new GoogleIdTokenVerifier(jwtDecoder);

        Jwt jwt = createJwt(
                null,
                "user@example.com",
                null
        );

        when(jwtDecoder.decode("id-token-without-subject"))
                .thenReturn(jwt);

        // when & then
        assertThatThrownBy(
                () -> verifier.verify(
                        "id-token-without-subject"
                )
        ).isInstanceOf(
                InvalidGoogleIdTokenException.class
        );
    }

    private Jwt createJwt(
            String subject,
            String email,
            String picture
    ) {
        Instant issuedAt = Instant.parse(
                "2026-08-14T00:00:00Z"
        );

        Jwt.Builder builder = Jwt
                .withTokenValue("google-id-token")
                .header("alg", "RS256")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(3_600))
                .claim("email", email)
                .claim("picture", picture);

        if (subject != null) {
            builder.subject(subject);
        }

        return builder.build();
    }
}