package com.chaekchaek.auth.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenHasher {

    private static final String UNUSABLE_ALGORITHM_ERROR_MESSAGE = "SHA-256 알고리즘을 사용할 수 없습니다. 다른 알고리즘을 사용해 주세요.";

    public String hash(String tokenValue) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hashedBytes = digest.digest(
                    tokenValue.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashedBytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(UNUSABLE_ALGORITHM_ERROR_MESSAGE, exception);
        }
    }
}