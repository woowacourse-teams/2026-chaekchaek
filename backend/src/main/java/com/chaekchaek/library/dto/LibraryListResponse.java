package com.chaekchaek.library.dto;

import java.util.List;

public record LibraryListResponse(
        long totalCount,
        int maxSize,
        long filteredCount,
        Integer nextPage,
        List<LibraryItemResponse> items
) {
}
