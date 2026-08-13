package com.chaekchaek.book.dto;

import java.util.List;

public record BookItem(
        Long bookId,
        String title,
        String coverImageUrl,
        List<String> authors,
        List<String> translators,
        String publishedDate,
        String isbn13,
        String category,
        String publisher,
        Integer commentCount
) {
}
