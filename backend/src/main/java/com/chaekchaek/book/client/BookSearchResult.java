package com.chaekchaek.book.client;

import java.util.List;

public record BookSearchResult(
        int totalCount,
        Integer nextPage,
        List<BookSearchItem> items
) {

    public BookSearchResult {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
