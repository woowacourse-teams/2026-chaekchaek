package com.chaekchaek.book.client.dto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record Yes24BookItem(
        String title,
        String author,
        String goodsSortNm,
        String publisher,
        String isbn13,
        String publishDate,
        String cover
) {

    public LocalDate publishedDate() {
        if (publishDate == null || publishDate.isBlank()) {
            return null;
        }
        return LocalDate.parse(publishDate, DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
}
