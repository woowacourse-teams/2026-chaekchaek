package com.chaekchaek.review.dto;

import java.time.Instant;

public record ReplyResponse(long replyId, String content, boolean deleted, Instant createdAt,
                            AuthorResponse author, long likeCount, boolean likedByMe) {
}
