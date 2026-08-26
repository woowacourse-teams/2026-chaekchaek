package com.chaekchaek.review.domain;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "reply", indexes = @Index(name = "idx_reply_review_created", columnList = "review_id,created_at"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reply_id")
    private Long id;

    @Column(name = "review_id", nullable = false)
    private long reviewId;

    @Column(name = "actor_id")
    private long actorId;

    @Column(nullable = false, length = 200)
    private String content;

    @Column(nullable = false)
    private boolean anonymous;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private Reply(long reviewId, long actorId, String content, boolean anonymous) {
        this.reviewId = reviewId;
        this.actorId = actorId;
        this.content = content;
        this.anonymous = anonymous;
    }

    public static Reply create(long reviewId, long actorId, String content, boolean anonymous) {
        return new Reply(reviewId, actorId, content, anonymous);
    }

    public void updateBy(long actorId, String content) {
        assertModifiableBy(actorId);
        this.content = content;
    }

    public void deleteBy(long actorId) {
        assertModifiableBy(actorId);
        deletedAt = Instant.now();
    }

    public void assertModifiableBy(long actorId) {
        if (deletedAt != null) {
            throw new BusinessException(ErrorCode.DELETED_RESOURCE);
        }
        if (this.actorId != actorId) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    @PrePersist
    void initializeTimestamps() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }
}
