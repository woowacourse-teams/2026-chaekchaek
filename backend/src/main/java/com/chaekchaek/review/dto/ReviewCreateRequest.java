package com.chaekchaek.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewCreateRequest(
        @NotBlank @Size(max = 1000) String content,
        @Size(max = 500) String quote,
        @Size(max = 255) String chapter,
        Boolean isSpoiler
) {
    public boolean spoiler() {
        return Boolean.TRUE.equals(isSpoiler);
    }
}
