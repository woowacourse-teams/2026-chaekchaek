package com.chaekchaek.book.client;

import java.time.LocalDate;
import java.util.List;

public record BookDetailItem(
        String title,
        String coverImageUrl,
        String description,
        List<String> authors,
        List<String> translators,
        LocalDate publishedDate,
        String isbn13,
        String category,
        String publisher,
        Integer totalPages
) {

    public BookDetailItem {
        authors = authors == null ? List.of() : List.copyOf(authors);
        translators = translators == null ? List.of() : List.copyOf(translators);
    }
}
