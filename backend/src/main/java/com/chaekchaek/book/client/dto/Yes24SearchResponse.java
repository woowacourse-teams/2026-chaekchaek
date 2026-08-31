package com.chaekchaek.book.client.dto;

public record Yes24SearchResponse(
        boolean success,
        String errorCode,
        Yes24SearchData data
) {
}
