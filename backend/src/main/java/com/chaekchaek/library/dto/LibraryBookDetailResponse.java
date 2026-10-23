package com.chaekchaek.library.dto;

import com.chaekchaek.library.domain.ReadingStatus;
import java.time.Instant;

public record LibraryBookDetailResponse(
        long bookId,
        String title,
        String spineImageUrl,
        String isbn13,
        Integer totalPages,
        ReadingStatus status,
        int currentPage,
        Instant readingUpdatedAt
) {
}
