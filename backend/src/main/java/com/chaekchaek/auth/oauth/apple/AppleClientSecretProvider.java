package com.chaekchaek.auth.oauth.apple;

import com.chaekchaek.auth.exception.AppleAuthServerException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

public class AppleClientSecretProvider {

    private final AppleAuthProperties properties;
    private final Clock clock;

    AppleClientSecretProvider(AppleAuthProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public String create() {
        try {
            Instant issuedAt = clock.instant();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(properties.teamId())
                    .subject(properties.clientId())
                    .audience(properties.issuer())
                    .issueTime(Date.from(issuedAt))
                    .expirationTime(Date.from(issuedAt.plus(5, ChronoUnit.MINUTES)))
                    .build();
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(properties.keyId()).build(),
                    claims
            );
            jwt.sign(new ECDSASigner(readPrivateKey()));
            return jwt.serialize();
        } catch (Exception exception) {
            throw new AppleAuthServerException(exception);
        }
    }

    private ECPrivateKey readPrivateKey() throws Exception {
        String normalized = properties.privateKey()
                .replace("\\n", "\n")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] encoded = Base64.getDecoder().decode(normalized);
        return (ECPrivateKey) KeyFactory.getInstance("EC")
                .generatePrivate(new PKCS8EncodedKeySpec(encoded));
    }
}
