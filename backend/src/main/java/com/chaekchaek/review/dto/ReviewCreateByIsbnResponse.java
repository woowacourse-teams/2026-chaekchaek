package com.chaekchaek.review.dto;

public record ReviewCreateByIsbnResponse(
        long bookId,
        ReviewResponse review
) {
}
