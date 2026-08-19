package com.chaekchaek.home.dto;

import java.time.Instant;

public record LatestReviewResponse(String content, Instant createdAt, long replyCount,
                                   long bookId, String isbn13, String bookTitle, String bookCoverImageUrl) {
}
