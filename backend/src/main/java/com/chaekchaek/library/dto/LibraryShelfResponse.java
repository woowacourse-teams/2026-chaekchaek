package com.chaekchaek.library.dto;

import java.util.List;

public record LibraryShelfResponse(
        long libraryId,
        String name,
        boolean isDefault,
        long totalCount,
        List<LibraryBookSummaryResponse> items
) {
    public LibraryShelfResponse {
        items = List.copyOf(items);
    }
}
