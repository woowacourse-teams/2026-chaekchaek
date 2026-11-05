package com.chaekchaek.pmfsurvey.service;

import com.chaekchaek.auth.token.access.AccessTokenProperties;
import java.time.Clock;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

@Component
public class PmfSurveyEligibilityTokenProvider {

    private static final String TOKEN_USE_CLAIM = "token_use";
    private static final String TOKEN_USE = "pmf_survey_eligibility";

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final AccessTokenProperties accessTokenProperties;
    private final Clock clock;

    public PmfSurveyEligibilityTokenProvider(
            JwtEncoder jwtEncoder,
            JwtDecoder jwtDecoder,
            AccessTokenProperties accessTokenProperties,
            Clock clock
    ) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.accessTokenProperties = accessTokenProperties;
        this.clock = clock;
    }

    public String issue(long actorId) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(accessTokenProperties.issuer())
                .subject(Long.toString(actorId))
                .issuedAt(clock.instant())
                .claim(TOKEN_USE_CLAIM, TOKEN_USE)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims
        )).getTokenValue();
    }

    public boolean isValidFor(String token, long actorId) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return Long.toString(actorId).equals(jwt.getSubject())
                    && TOKEN_USE.equals(jwt.getClaimAsString(TOKEN_USE_CLAIM));
        } catch (JwtException exception) {
            return false;
        }
    }
}
