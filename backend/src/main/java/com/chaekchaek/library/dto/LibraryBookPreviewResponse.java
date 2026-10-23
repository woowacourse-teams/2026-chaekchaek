package com.chaekchaek.library.dto;

import jakarta.annotation.Nullable;

public record LibraryBookPreviewResponse(
        long bookId,
        String title,
        @Nullable
        String spineImageUrl,
        String isbn13
) {
}
