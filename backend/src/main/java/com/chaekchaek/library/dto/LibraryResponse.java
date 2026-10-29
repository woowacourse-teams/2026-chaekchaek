package com.chaekchaek.library.dto;

import com.chaekchaek.library.domain.Library;
import java.util.List;

public record LibraryResponse(
        long libraryId,
        String name,
        boolean isDefault,
        int totalCount,
        List<LibraryBookPreviewResponse> items
) {

    public static LibraryResponse from(Library library) {
        return new LibraryResponse(
                library.getId(),
                library.getName(),
                library.isDefault(),
                0,
                List.of()
        );
    }
}
