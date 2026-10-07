package com.chaekchaek.feed.dto;

import java.util.List;

public record FeedReviewListResponse(
        long totalCount,
        Integer nextPage,
        List<FeedReviewResponse> reviews
) {
}
