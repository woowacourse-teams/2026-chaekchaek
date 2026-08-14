package com.chaekchaek.library.dto;

import com.chaekchaek.library.domain.ReadingStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BulkUpdateLibraryStatusRequest(
        @NotEmpty @Size(max = 10) List<Long> bookIds,
        @NotNull ReadingStatus status
) {

    public boolean hasDuplicateBookIds() {
        return bookIds.stream().distinct().count() != bookIds.size();
    }
}
