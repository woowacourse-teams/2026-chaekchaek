package com.chaekchaek.auth.oauth.google;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

class GoogleAudienceValidatorTest {

    private static final String WEB_CLIENT_ID = "test-web-client-id";

    private final GoogleAudienceValidator validator =
            new GoogleAudienceValidator(WEB_CLIENT_ID);

    @Test
    @DisplayName("Web Client ID가 audience에 포함되면 검증에 성공한다")
    void should_Succeed_When_AudienceContainsWebClientId() {
        // given
        Jwt jwt = createJwt(List.of(WEB_CLIENT_ID));

        // when
        OAuth2TokenValidatorResult result =
                validator.validate(jwt);

        // then
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    @DisplayName("Web Client ID가 audience에 없으면 검증에 실패한다")
    void should_Fail_When_AudienceDoesNotContainWebClientId() {
        // given
        Jwt jwt = createJwt(
                List.of("different-client-id")
        );

        // when
        OAuth2TokenValidatorResult result =
                validator.validate(jwt);

        // then
        assertThat(result.hasErrors()).isTrue();
    }

    private Jwt createJwt(List<String> audience) {
        Instant issuedAt = Instant.parse(
                "2026-08-14T00:00:00Z"
        );

        return Jwt.withTokenValue("google-id-token")
                .header("alg", "RS256")
                .subject("google-user-id")
                .audience(audience)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(3_600))
                .build();
    }
}