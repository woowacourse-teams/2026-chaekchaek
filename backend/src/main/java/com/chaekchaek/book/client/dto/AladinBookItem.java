package com.chaekchaek.book.client.dto;

import com.chaekchaek.book.domain.Isbn13;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

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
    public boolean matchesIsbn13(Isbn13 isbn13) {
        return isbn13.value().equals(this.isbn13);
    }

    public LocalDate publishedDate() {
        if (pubDate == null || pubDate.isBlank()) {
            return null;
        }
        return LocalDate.parse(pubDate);
    }

    public Integer totalPages() {
        return subInfo == null ? null : subInfo.itemPage();
    }
}
