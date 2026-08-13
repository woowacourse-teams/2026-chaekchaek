package com.chaekchaek.book.dto;

import com.chaekchaek.book.dto.validation.ValidIsbn13;
import jakarta.validation.constraints.NotBlank;

public record BookResolveRequest(
        @NotBlank @ValidIsbn13 String isbn13
) {
}
