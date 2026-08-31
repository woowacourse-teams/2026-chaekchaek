package com.chaekchaek.book.dto;

import java.util.List;

public record BookSearchResponse(
        int totalCount,
        Integer nextPage,
        List<BookItem> items
) {

    public BookSearchResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
