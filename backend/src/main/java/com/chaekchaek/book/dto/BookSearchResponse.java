package com.chaekchaek.book.dto;

import java.util.List;

public record BookSearchResponse(
        int totalCount,
        Integer nextPage,
        List<BookItem> items
) {
}
