package com.chaekchaek.feed.dto;

import com.chaekchaek.review.dto.AuthorResponse;
import java.time.Instant;

public record FeedReviewResponse(
        long reviewId,
        String content,
        boolean isSpoiler,
        Instant createdAt,
        AuthorResponse author,
        long replyCount,
        long bookId,
        String isbn13,
        String bookTitle,
        String bookCoverImageUrl
) {
}
