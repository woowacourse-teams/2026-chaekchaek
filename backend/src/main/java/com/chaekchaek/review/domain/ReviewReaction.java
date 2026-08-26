package com.chaekchaek.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "reaction")
@IdClass(ReviewReaction.ReviewReactionId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewReaction {

    @Id
    @Column(name = "review_id")
    private long reviewId;

    @Id
    @Column(name = "actor_id")
    private long actorId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ReviewReaction(long reviewId, long actorId) {
        this.reviewId = reviewId;
        this.actorId = actorId;
    }

    @PrePersist
    void initializeCreatedAt() {
        createdAt = Instant.now();
    }

    @EqualsAndHashCode
    public static class ReviewReactionId implements Serializable {
        private long reviewId;
        private long actorId;

        public ReviewReactionId() {
        }

        public ReviewReactionId(long reviewId, long actorId) {
            this.reviewId = reviewId;
            this.actorId = actorId;
        }
    }
}
