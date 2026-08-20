package com.chaekchaek.auth.oauth.apple;

import com.chaekchaek.auth.exception.InvalidAppleAuthorizationException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

public class AppleIdTokenVerifier {

    private final JwtDecoder jwtDecoder;

    AppleIdTokenVerifier(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    public AppleProfile verify(String identityToken, String rawNonce) {
        try {
            Jwt jwt = jwtDecoder.decode(identityToken);
            validateSubject(jwt);
            validateNonce(jwt, rawNonce);
            return new AppleProfile(jwt.getSubject());
        } catch (JwtException | IllegalArgumentException exception) {
            throw new InvalidAppleAuthorizationException(exception);
        }
    }

    private void validateSubject(Jwt jwt) {
        if (jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw new InvalidAppleAuthorizationException();
        }
    }

    private void validateNonce(Jwt jwt, String rawNonce) {
        String tokenNonce = jwt.getClaimAsString("nonce");
        String expectedNonce = sha256(rawNonce);
        if (tokenNonce == null || !MessageDigest.isEqual(
                tokenNonce.getBytes(StandardCharsets.US_ASCII),
                expectedNonce.getBytes(StandardCharsets.US_ASCII))) {
            throw new InvalidAppleAuthorizationException();
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
