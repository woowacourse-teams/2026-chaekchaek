package com.chaekchaek.library.dto;

import com.chaekchaek.library.domain.ReadingStatus;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateLibraryItemRequest(
        ReadingStatus status,
        @PositiveOrZero Integer currentPage,
        @Positive Integer totalPages
) {

    public boolean hasExactlyOneUpdate() {
        return (status == null) != (currentPage == null);
    }
}
