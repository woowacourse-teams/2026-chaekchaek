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
@Table(name = "reply_reaction")
@IdClass(ReplyReaction.ReplyReactionId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReplyReaction {

    @Id
    @Column(name = "reply_id")
    private long replyId;

    @Id
    @Column(name = "actor_id")
    private long actorId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ReplyReaction(long replyId, long actorId) {
        this.replyId = replyId;
        this.actorId = actorId;
    }

    @PrePersist
    void initializeCreatedAt() {
        createdAt = Instant.now();
    }

    @EqualsAndHashCode
    public static class ReplyReactionId implements Serializable {
        private long replyId;
        private long actorId;

        public ReplyReactionId() {
        }

        public ReplyReactionId(long replyId, long actorId) {
            this.replyId = replyId;
            this.actorId = actorId;
        }
    }
}
