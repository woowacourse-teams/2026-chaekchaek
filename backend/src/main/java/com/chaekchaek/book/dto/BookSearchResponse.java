package com.chaekchaek.book.dto;

import java.util.List;

public record BookSearchResponse(
        int totalResults,
        int startIndex,
        int itemsPerPage,
        boolean hasNext,
        List<BookItem> items
) {
}
