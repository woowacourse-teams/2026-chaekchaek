package com.chaekchaek.library.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BulkDeleteLibraryItemsRequest(
        @NotEmpty @Size(max = 10) List<Long> bookIds
) {

    public boolean hasDuplicateBookIds() {
        return bookIds.stream().distinct().count() != bookIds.size();
    }
}
