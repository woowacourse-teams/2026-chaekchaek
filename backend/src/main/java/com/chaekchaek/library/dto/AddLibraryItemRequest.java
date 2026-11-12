package com.chaekchaek.library.dto;

import jakarta.validation.constraints.NotBlank;

public record AddLibraryItemRequest(
        @NotBlank String isbn13
) {
}
