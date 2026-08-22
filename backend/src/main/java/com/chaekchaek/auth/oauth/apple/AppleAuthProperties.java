package com.chaekchaek.auth.oauth.apple;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.apple")
public record AppleAuthProperties(
        String issuer,
        String jwkSetUri,
        String clientId,
        String teamId,
        String keyId,
        String privateKey,
        String tokenUri,
        String revokeUri
) {
}
