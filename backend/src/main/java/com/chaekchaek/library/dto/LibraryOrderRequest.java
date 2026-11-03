package com.chaekchaek.library.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import tools.jackson.databind.annotation.JsonDeserialize;

public record LibraryOrderRequest(
        @NotNull @JsonDeserialize(contentUsing = OrderIdDeserializer.class)
        List<@NotNull @Positive Long> libraryIds
) {
}
