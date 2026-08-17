package com.chaekchaek.auth.token.refresh;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenHasher {

    private static final String HASH_ALGORITHM_MUST_BE_AVAILABLE_ERROR_MESSAGE =
            "[ERROR] SHA-256 해시 알고리즘을 사용할 수 있는 환경이어야 합니다";

    public String hash(String tokenValue) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hashedBytes = digest.digest(
                    tokenValue.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashedBytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(HASH_ALGORITHM_MUST_BE_AVAILABLE_ERROR_MESSAGE, exception);
        }
    }
}