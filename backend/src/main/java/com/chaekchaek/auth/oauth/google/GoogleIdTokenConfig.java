package com.chaekchaek.auth.oauth.google;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
@EnableConfigurationProperties(GoogleIdTokenProperties.class)
public class GoogleIdTokenConfig {

    @Bean
    GoogleIdTokenVerifier googleIdTokenVerifier(
            GoogleIdTokenProperties properties
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(properties.jwkSetUri())
                .build();

        OAuth2TokenValidator<Jwt> issuerAndTimestampValidator =
                JwtValidators.createDefaultWithIssuer(
                        properties.issuer()
                );

        OAuth2TokenValidator<Jwt> audienceValidator =
                new GoogleAudienceValidator(
                        properties.audience()
                );

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        issuerAndTimestampValidator,
                        audienceValidator
                )
        );

        return new GoogleIdTokenVerifier(decoder);
    }
}
