package com.chaekchaek.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameLibraryRequest(
        @NotBlank
        @Size(min = 1, max = 10)
        String name
) {
}
