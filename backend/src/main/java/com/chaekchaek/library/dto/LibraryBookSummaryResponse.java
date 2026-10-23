package com.chaekchaek.library.dto;

public record LibraryBookSummaryResponse(
        long bookId,
        String title,
        String spineImageUrl,
        String isbn13
) {
}
