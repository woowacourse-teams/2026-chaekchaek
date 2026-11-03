package com.chaekchaek.review.dto;

import java.time.Instant;

public record ReplyResponse(
        long replyId,
        String content,
        Instant createdAt,
        AuthorResponse author,
        long likeCount,
        boolean likedByMe
) {
}
