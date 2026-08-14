package com.chaekchaek.review.dto;

import java.time.Instant;
import java.util.List;

public record ReviewResponse(long reviewId, String content, String quote, String chapter, Integer currentPage,
                             boolean isSpoiler, boolean deleted, Instant createdAt, AuthorResponse author,
                             long likeCount, boolean likedByMe, long replyCount,
                             List<ReplyResponse> recentReplies) {
}
