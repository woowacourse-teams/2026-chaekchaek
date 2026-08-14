package com.chaekchaek.book.dto;

import java.math.BigDecimal;
import java.util.List;

public record BookDetailResponse(
        Long bookId,
        String isbn13,
        String title,
        String coverImageUrl,
        List<String> authors,
        List<String> translators,
        String publisher,
        String category,
        String publishedDate,
        Integer totalPages,
        int commentCount,
        BigDecimal averageRating,
        int ratingCount,
        BookMyRecordResponse myRecord
) {
}
