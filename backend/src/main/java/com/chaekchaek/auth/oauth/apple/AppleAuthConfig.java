package com.chaekchaek.auth.oauth.apple;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AppleAuthProperties.class)
public class AppleAuthConfig {

    @Bean
    AppleIdTokenVerifier appleIdTokenVerifier(AppleAuthProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(properties.jwkSetUri())
                .build();
        OAuth2TokenValidator<Jwt> issuerAndTimestamp =
                JwtValidators.createDefaultWithIssuer(properties.issuer());
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                issuerAndTimestamp,
                new AppleAudienceValidator(properties.clientId())
        ));
        return new AppleIdTokenVerifier(decoder);
    }

    @Bean
    AppleClientSecretProvider appleClientSecretProvider(
            AppleAuthProperties properties,
            Clock clock
    ) {
        return new AppleClientSecretProvider(properties, clock);
    }

    @Bean
    AppleTokenClient appleTokenClient(
            RestClient.Builder builder,
            AppleAuthProperties properties,
            AppleClientSecretProvider clientSecretProvider
    ) {
        return new AppleTokenClient(builder.build(), properties, clientSecretProvider);
    }
}
