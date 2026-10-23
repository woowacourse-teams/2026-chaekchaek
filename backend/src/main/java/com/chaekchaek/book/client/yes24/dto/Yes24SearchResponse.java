package com.chaekchaek.book.client.yes24.dto;

public record Yes24SearchResponse(
        boolean success,
        String errorCode,
        Yes24SearchData data
) {
}
