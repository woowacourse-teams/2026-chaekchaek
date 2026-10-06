package com.chaekchaek.book.client;

public record Yes24ImageUrls(
        String spineImageUrl,
        String backImageUrl
) {
    public static Yes24ImageUrls empty() {
        return new Yes24ImageUrls(null, null);
    }
}
