package com.chaekchaek.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record MobileAppleLoginRequest(
        @NotBlank String identityToken,
        @NotBlank String authorizationCode,
        @NotBlank String nonce
) {
}
