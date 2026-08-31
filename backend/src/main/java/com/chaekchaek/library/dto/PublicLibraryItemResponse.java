package com.chaekchaek.library.dto;

import com.chaekchaek.library.domain.ReadingStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PublicLibraryItemResponse(
        long bookId,
        String isbn13,
        String title,
        String coverImageUrl,
        List<String> authors,
        List<String> translators,
        String publisher,
        String category,
        LocalDate publishedDate,
        Integer totalPages,
        long commentCount,
        ReadingStatus status,
        int currentPage,
        BigDecimal rating
) {
    public static PublicLibraryItemResponse from(LibraryItemResponse item) {
        return new PublicLibraryItemResponse(item.bookId(), item.isbn13(), item.title(), item.coverImageUrl(),
                item.authors(), item.translators(), item.publisher(), item.category(), item.publishedDate(),
                item.totalPages(), item.commentCount(), item.status(), item.currentPage(), item.rating());
    }
}
