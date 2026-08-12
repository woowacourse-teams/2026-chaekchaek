package com.chaekchaek.auth.token.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.domain.MemberType;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class AccessTokenProviderTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-08-12T06:00:00Z");

    private static final String ISSUER = "chaekchaek";

    private static final String BASE64_SECRET =
            Base64.getEncoder().encodeToString(
                    "0123456789abcdef0123456789abcdef"
                            .getBytes()
            );

    private AccessTokenProvider accessTokenProvider;
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        AccessTokenProperties properties =
                new AccessTokenProperties(
                        ISSUER,
                        BASE64_SECRET,
                        Duration.ofMinutes(30)
                );

        SecretKey secretKey = new SecretKeySpec(
                Base64.getDecoder().decode(BASE64_SECRET),
                "HmacSHA256"
        );

        NimbusJwtEncoder jwtEncoder = new NimbusJwtEncoder(
                new ImmutableSecret<>(secretKey)
        );

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        Clock fixedClock = Clock.fixed(
                FIXED_TIME,
                ZoneOffset.UTC
        );

        JwtTimestampValidator timestampValidator = new JwtTimestampValidator();
        timestampValidator.setClock(fixedClock);

        JwtIssuerValidator issuerValidator = new JwtIssuerValidator(ISSUER);

        OAuth2TokenValidator<Jwt> validator =
                new DelegatingOAuth2TokenValidator<>(
                        timestampValidator,
                        issuerValidator
                );

        decoder.setJwtValidator(validator);

        accessTokenProvider = new AccessTokenProvider(
                jwtEncoder,
                properties,
                fixedClock
        );

        jwtDecoder = decoder;
    }

    @Test
    @DisplayName("저장된 회원에게 Access Token을 발급한다")
    void should_IssueAccessToken_When_SavedMember() {
        // given
        Member member = org.mockito.Mockito.mock(Member.class);

        when(member.getId()).thenReturn(1L);
        when(member.getType()).thenReturn(MemberType.MEMBER);

        // when
        String token = accessTokenProvider.issue(member);
        Jwt jwt = jwtDecoder.decode(token);

        // then
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(ISSUER);
        assertThat(jwt.getSubject()).isEqualTo("1");
        assertThat(jwt.getIssuedAt()).isEqualTo(FIXED_TIME);
        assertThat(jwt.getExpiresAt()).isEqualTo(FIXED_TIME.plus(Duration.ofMinutes(30)));
        assertThat(jwt.getClaimAsString("memberType")).isEqualTo("MEMBER");
    }

    @Test
    @DisplayName("저장되지 않은 회원에게는 Access Token을 발급하지 않는다")
    void shouldNot_IssueAccessToken_When_UnsavedMember() {
        // given
        Member member = org.mockito.Mockito.mock(Member.class);

        when(member.getId()).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> accessTokenProvider.issue(member))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("다른 비밀키로 Access Token을 검증하면 거부된다")
    void should_RejectToken_When_SignedWithDifferentKey() {
        // given
        Member member = org.mockito.Mockito.mock(Member.class);

        when(member.getId()).thenReturn(1L);
        when(member.getType()).thenReturn(MemberType.MEMBER);

        String token = accessTokenProvider.issue(member);

        byte[] differentKeyBytes = "abcdef0123456789abcdef0123456789".getBytes();

        SecretKey differentKey = new SecretKeySpec(
                differentKeyBytes,
                "HmacSHA256"
        );

        JwtDecoder differentDecoder = NimbusJwtDecoder
                .withSecretKey(differentKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        // when & then
        assertThatThrownBy(() -> differentDecoder.decode(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("만료된 Access Token은 거부된다")
    void should_Reject_When_TokenExpired() {
        // given
        AccessTokenProperties expiredProperties =
                new AccessTokenProperties(
                        ISSUER,
                        BASE64_SECRET,
                        Duration.ofMinutes(30)
                );

        SecretKey secretKey = new SecretKeySpec(
                Base64.getDecoder().decode(BASE64_SECRET),
                "HmacSHA256"
        );

        NimbusJwtEncoder encoder = new NimbusJwtEncoder(
                new ImmutableSecret<>(secretKey)
        );

        Clock oldClock = Clock.fixed(
                Instant.parse("2020-01-01T00:00:00Z"),
                ZoneOffset.UTC
        );

        AccessTokenProvider expiredTokenProvider =
                new AccessTokenProvider(
                        encoder,
                        expiredProperties,
                        oldClock
                );

        Member member = org.mockito.Mockito.mock(Member.class);

        when(member.getId()).thenReturn(1L);
        when(member.getType()).thenReturn(MemberType.MEMBER);

        String expiredToken = expiredTokenProvider.issue(member);

        // when & then
        assertThatThrownBy(() -> jwtDecoder.decode(expiredToken))
                .isInstanceOf(JwtException.class);
    }
}