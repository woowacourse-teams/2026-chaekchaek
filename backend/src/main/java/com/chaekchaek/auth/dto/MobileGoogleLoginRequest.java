package com.chaekchaek.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record MobileGoogleLoginRequest(

        @NotBlank
        String idToken
) {
}
