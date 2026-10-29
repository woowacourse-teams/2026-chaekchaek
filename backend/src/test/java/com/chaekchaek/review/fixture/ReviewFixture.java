package com.chaekchaek.review.fixture;

import com.chaekchaek.review.domain.Review;
import java.time.Instant;
import org.springframework.test.util.ReflectionTestUtils;

public final class ReviewFixture {

    private ReviewFixture() {
    }

    public static Review savedReview(long id, Review review) {
        ReflectionTestUtils.setField(review, "id", id);
        return review;
    }

    public static Review savedReview(long id, Review review, Instant createdAt) {
        savedReview(id, review);
        ReflectionTestUtils.setField(review, "createdAt", createdAt);
        return review;
    }
}
