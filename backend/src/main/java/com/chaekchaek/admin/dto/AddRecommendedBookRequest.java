package com.chaekchaek.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record AddRecommendedBookRequest(
        @NotBlank String isbn13
) {
}
