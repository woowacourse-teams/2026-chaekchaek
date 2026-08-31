package com.chaekchaek.book.client;

import java.time.LocalDate;
import java.util.List;

public record BookSearchItem(
        String title,
        String coverImageUrl,
        List<String> authors,
        List<String> translators,
        LocalDate publishedDate,
        String isbn13,
        String category,
        String publisher
) {

    public BookSearchItem {
        authors = authors == null ? List.of() : List.copyOf(authors);
        translators = translators == null ? List.of() : List.copyOf(translators);
    }
}
