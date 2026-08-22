package com.chaekchaek.auth.oauth;

import java.util.Arrays;
import java.util.Optional;

public enum OAuthFrontendClient {

    LOCAL("local"),
    DEV("dev");

    private final String value;

    OAuthFrontendClient(String value) {
        this.value = value;
    }

    public static Optional<OAuthFrontendClient> from(String value) {
        return Arrays.stream(values())
                .filter(client -> client.value.equalsIgnoreCase(value))
                .findFirst();
    }
}
