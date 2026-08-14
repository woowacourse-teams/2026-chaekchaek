package com.chaekchaek.review.dto;

public record AuthorResponse(String displayName, String profileImageUrl, boolean anonymous, boolean mine) {
}
