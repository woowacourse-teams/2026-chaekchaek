package com.chaekchaek.pmfsurvey.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaekchaek.auth.token.access.AccessTokenProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PmfSurveyEligibilityTokenProviderTest {

    private static final long ACTOR_ID = 42L;
    private static final Instant NOW = Instant.parse("2026-11-05T03:00:00Z");

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private AccessTokenProperties accessTokenProperties;

    @Test
    @DisplayName("대상 토큰은 발급한 actor만 검증할 수 있고 시간 만료되지 않는다")
    void should_BindEligibilityTokenToActorWithoutWindowExpiry() {
        PmfSurveyEligibilityTokenProvider tokenProvider = new PmfSurveyEligibilityTokenProvider(
                jwtEncoder,
                jwtDecoder,
                accessTokenProperties,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        String token = tokenProvider.issue(ACTOR_ID);

        assertThat(tokenProvider.isValidFor(token, ACTOR_ID)).isTrue();
        assertThat(tokenProvider.isValidFor(token, ACTOR_ID + 1)).isFalse();
        assertThat(tokenProvider.isValidFor("tampered-token", ACTOR_ID)).isFalse();
    }
}
