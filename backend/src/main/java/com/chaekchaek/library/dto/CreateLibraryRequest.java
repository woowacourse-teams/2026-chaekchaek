package com.chaekchaek.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLibraryRequest(
        @NotBlank @Size(max = 100) String name
) {

    public CreateLibraryRequest {
        if (name != null) {
            name = name.strip();
        }
    }
}
