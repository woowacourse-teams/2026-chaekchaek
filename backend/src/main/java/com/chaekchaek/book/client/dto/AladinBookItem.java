package com.chaekchaek.book.client.dto;

public record AladinBookItem(
        String title,
        String cover,
        String author,
        String pubDate,
        String isbn13,
        String categoryName,
        String publisher
) {
}
