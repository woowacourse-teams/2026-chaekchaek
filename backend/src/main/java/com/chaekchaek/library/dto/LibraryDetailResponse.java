package com.chaekchaek.library.dto;

import java.util.List;

public record LibraryDetailResponse(
        long libraryId,
        long memberId,
        String name,
        boolean isDefault,
        long totalCount,
        List<LibraryBookDetailResponse> items
) {
    public LibraryDetailResponse {
        items = List.copyOf(items);
    }
}
