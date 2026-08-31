package com.chaekchaek.library.dto;

import java.util.List;

public record PublicLibraryListResponse(
        long totalCount,
        long filteredCount,
        Integer nextPage,
        List<PublicLibraryItemResponse> items
) {
    public static PublicLibraryListResponse from(LibraryListResponse library) {
        return new PublicLibraryListResponse(library.totalCount(), library.filteredCount(), library.nextPage(),
                library.items().stream().map(PublicLibraryItemResponse::from).toList());
    }
}
