package com.chaekchaek.incident.alert.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Set;

public record IncidentReference(
        String environment,
        String fingerprint,
        Instant startsAt
) {
    public IncidentReference {
        if (environment == null || !Set.of("dev", "prod").contains(environment)
                || fingerprint == null || !fingerprint.matches("[A-Za-z0-9_-]{1,128}")) {
            throw new IllegalArgumentException("유효한 장애 환경과 식별자가 필요합니다");
        }
        Objects.requireNonNull(startsAt);
    }

    public String key(long orgId) {
        String value = orgId + "|" + environment + "|" + fingerprint + "|" + startsAt;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
