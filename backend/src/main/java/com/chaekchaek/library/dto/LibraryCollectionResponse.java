package com.chaekchaek.library.dto;

import java.util.List;

public record LibraryCollectionResponse(
        long memberId,
        List<LibraryShelfResponse> libraries
) {
    public LibraryCollectionResponse {
        libraries = List.copyOf(libraries);
    }
}
