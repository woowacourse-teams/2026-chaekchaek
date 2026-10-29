package com.chaekchaek.library.dto;

import java.util.List;

public record LibraryBookSummaryResponse(
        long bookId,
        String title,
        String coverImageUrl,
        String spineImageUrl,
        String isbn13,
        List<String> authors
) {
    public LibraryBookSummaryResponse {
        authors = List.copyOf(authors);
    }

    public LibraryBookSummaryResponse withAuthors(List<String> authors) {
        return new LibraryBookSummaryResponse(
                bookId,
                title,
                coverImageUrl,
                spineImageUrl,
                isbn13,
                authors
        );
    }
}
