package com.chaekchaek.library.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PublicLibraryItemResponse(
        long bookId,
        String isbn13,
        String title,
        String coverImageUrl,
        String spineImageUrl,
        String backImageUrl,
        List<String> authors,
        List<String> translators,
        String publisher,
        String category,
        LocalDate publishedDate,
        long commentCount,
        BigDecimal rating
) {
    public static PublicLibraryItemResponse from(LibraryItemResponse item) {
        return new PublicLibraryItemResponse(
                item.bookId(),
                item.isbn13(),
                item.title(),
                item.coverImageUrl(),
                item.spineImageUrl(),
                item.backImageUrl(),
                item.authors(),
                item.translators(),
                item.publisher(),
                item.category(),
                item.publishedDate(),
                item.commentCount(),
                item.rating()
        );
    }
}
