package com.chaekchaek.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record MobileRefreshTokenRequest(

        @NotBlank
        String refreshToken
) {
}
