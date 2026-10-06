package com.chaekchaek.feed.dto;

import com.chaekchaek.review.dto.AuthorResponse;
import java.time.Instant;
import java.util.List;

public record FeedReviewResponse(
        long reviewId,
        String content,
        String quote,
        Integer currentPage,
        boolean isSpoiler,
        Instant createdAt,
        AuthorResponse author,
        long likeCount,
        boolean likedByMe,
        long replyCount,
        long bookId,
        String isbn13,
        String bookTitle,
        List<String> bookAuthors,
        String bookCoverImageUrl
) {
}
