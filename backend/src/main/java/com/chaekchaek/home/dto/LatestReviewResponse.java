package com.chaekchaek.home.dto;

import com.chaekchaek.review.dto.AuthorResponse;
import java.time.Instant;

public record LatestReviewResponse(String content, Instant createdAt, AuthorResponse author, long replyCount,
                                   long bookId, String isbn13, String bookTitle, String bookCoverImageUrl) {
}
