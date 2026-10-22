package com.chaekchaek.library.dto;

import com.chaekchaek.library.domain.Library;

public record LibraryResponse(
        long libraryId,
        String name
) {

    public static LibraryResponse from(Library library) {
        return new LibraryResponse(
                library.getId(),
                library.getName()
        );
    }
}
