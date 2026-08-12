package com.chaekchaek.book.dto;

public record BookItem(
        String title,
        String coverImageUrl,
        String author,
        String publishedDate,
        String isbn13,
        String category,
        String publisher
) {
}
