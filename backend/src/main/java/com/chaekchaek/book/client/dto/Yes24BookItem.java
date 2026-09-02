package com.chaekchaek.book.client.dto;

import com.chaekchaek.book.domain.Isbn13;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record Yes24BookItem(
        String title,
        String author,
        String goodsSortNm,
        String publisher,
        String isbn13,
        String publishDate,
        String cover,
        Integer pages,
        Yes24ContentDetail contentDetail
) {

    public LocalDate publishedDate() {
        if (publishDate == null || publishDate.isBlank()) {
            return null;
        }
        if (publishDate.length() == 8) {
            return LocalDate.parse(publishDate, DateTimeFormatter.BASIC_ISO_DATE);
        }
        return LocalDate.parse(publishDate, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public boolean matchesIsbn13(Isbn13 isbn13) {
        return isbn13.value().equals(this.isbn13);
    }

    public String description() {
        return contentDetail == null ? null : contentDetail.bookIntroduction();
    }
}
