package com.chaekchaek.book.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AladinBookItem(
        String title,
        String cover,
        String author,
        String description,
        String pubDate,
        String isbn13,
        String categoryName,
        String publisher,
        @JsonProperty("subInfo") AladinBookSubInfo subInfo
) {
    public Integer totalPages() {
        return subInfo == null ? null : subInfo.itemPage();
    }
}
