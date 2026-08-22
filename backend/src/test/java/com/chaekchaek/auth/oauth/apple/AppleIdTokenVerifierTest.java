package com.chaekchaek.auth.oauth.apple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.chaekchaek.auth.exception.InvalidAppleAuthorizationException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@ExtendWith(MockitoExtension.class)
class AppleIdTokenVerifierTest {

    @Mock
    private JwtDecoder jwtDecoder;

    @Test
    void should_VerifySubjectAndNonce() throws Exception {
        String rawNonce = "raw-nonce";
        Jwt jwt = jwt("apple-user-id", sha256(rawNonce));
        when(jwtDecoder.decode("identity-token")).thenReturn(jwt);

        AppleProfile profile = new AppleIdTokenVerifier(jwtDecoder)
                .verify("identity-token", rawNonce);

        assertThat(profile.providerUserId()).isEqualTo("apple-user-id");
    }

    @Test
    void should_RejectMismatchedNonce() {
        when(jwtDecoder.decode("identity-token"))
                .thenReturn(jwt("apple-user-id", "different-nonce"));

        assertThatThrownBy(() -> new AppleIdTokenVerifier(jwtDecoder)
                .verify("identity-token", "raw-nonce"))
                .isInstanceOf(InvalidAppleAuthorizationException.class);
    }

    private Jwt jwt(String subject, String nonce) {
        Instant now = Instant.now();
        return new Jwt(
                "token", now, now.plusSeconds(300),
                Map.of("alg", "RS256"),
                Map.of("sub", subject, "nonce", nonce)
        );
    }

    private String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
