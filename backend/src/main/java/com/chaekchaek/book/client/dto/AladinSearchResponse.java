package com.chaekchaek.book.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.util.List;

public record AladinSearchResponse(
        Integer errorCode,
        String errorMessage,
        @JsonSetter(nulls = Nulls.AS_EMPTY) int totalResults,
        @JsonSetter(nulls = Nulls.AS_EMPTY) int startIndex,
        @JsonSetter(nulls = Nulls.AS_EMPTY) int itemsPerPage,
        @JsonProperty("item") List<AladinBookItem> items
) {

    public boolean hasError() {
        return errorCode != null;
    }

    public boolean hasNextPage() {
        return (long) startIndex * itemsPerPage < totalResults;
    }
}
