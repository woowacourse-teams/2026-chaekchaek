package com.chaekchaek.auth.token.config;

import com.chaekchaek.auth.token.access.AccessTokenProperties;
import com.chaekchaek.auth.token.cookie.AuthCookieProperties;
import com.chaekchaek.auth.token.refresh.RefreshTokenProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
@EnableConfigurationProperties({
        AccessTokenProperties.class,
        RefreshTokenProperties.class,
        AuthCookieProperties.class
})
public class TokenConfig {

    private static final String JWT_SECRET_KEY_LESS_THAN_32BYTE_ERROR_MESSAGE = "[ERROR] JWT 비밀키는 32바이트 이상이어야 합니다.";

    @Bean
    SecretKey jwtSecretKey(AccessTokenProperties properties) {
        byte[] decodedKey = Base64.getDecoder().decode(properties.secret());

        if (decodedKey.length < 32) {
            throw new IllegalArgumentException(JWT_SECRET_KEY_LESS_THAN_32BYTE_ERROR_MESSAGE);
        }

        return new SecretKeySpec(decodedKey, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey secretKey) {
        return new NimbusJwtEncoder(
                new ImmutableSecret<>(secretKey)
        );
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey secretKey, AccessTokenProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(properties.issuer());

        decoder.setJwtValidator(validator);

        return decoder;
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    SecureRandom secureRandom() {
        return new SecureRandom();
    }
}
