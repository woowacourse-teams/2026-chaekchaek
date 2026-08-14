package com.chaekchaek.library.dto;

import com.chaekchaek.library.domain.ReadingStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddLibraryItemRequest(
        @NotBlank String isbn13,
        @NotNull ReadingStatus status,
        @Positive Integer totalPages
) {
}
