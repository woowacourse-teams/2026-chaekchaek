package com.chaekchaek.library.dto;

import java.util.List;

public record LibraryBookDetailResponse(
        long bookId,
        String title,
        String coverImageUrl,
        String spineImageUrl,
        String isbn13,
        List<String> authors
) {
    public LibraryBookDetailResponse {
        authors = List.copyOf(authors);
    }

    public LibraryBookDetailResponse withAuthors(List<String> authors) {
        return new LibraryBookDetailResponse(
                bookId,
                title,
                coverImageUrl,
                spineImageUrl,
                isbn13,
                authors
        );
    }
}
