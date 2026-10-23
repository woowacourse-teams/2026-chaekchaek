package com.chaekchaek.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateLibraryRequest(
        @NotBlank
        @Size(min = 1, max = 10)
        @Pattern(regexp = "^[가-힣A-Za-z0-9_-]+$")
        String name
) {
}
