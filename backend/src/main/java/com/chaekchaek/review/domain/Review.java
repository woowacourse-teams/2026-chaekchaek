package com.chaekchaek.review.domain;

import com.chaekchaek.review.exception.ReviewErrorCode;
import com.chaekchaek.review.exception.ReviewException;
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
@Table(name = "review", indexes = {
        @Index(name = "idx_review_book_created", columnList = "book_id,created_at"),
        @Index(name = "idx_review_book_page", columnList = "book_id,current_page"),
        @Index(name = "idx_review_member_book", columnList = "member_id,book_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @Column(name = "book_id", nullable = false)
    private long bookId;

    @Column(name = "member_id", nullable = false)
    private long memberId;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(length = 500)
    private String quote;

    @Column(length = 255)
    private String chapter;

    @Column(name = "current_page")
    private Integer currentPage;

    @Column(name = "is_spoiler", nullable = false)
    private boolean spoiler;

    @Column(nullable = false)
    private boolean anonymous;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private Review(long bookId, long memberId, String content, String quote, String chapter,
                   Integer currentPage, boolean spoiler, boolean anonymous) {
        this.bookId = bookId;
        this.memberId = memberId;
        this.content = content;
        this.quote = quote;
        this.chapter = chapter;
        this.currentPage = currentPage;
        this.spoiler = spoiler;
        this.anonymous = anonymous;
    }

    public static Review create(long bookId, long memberId, String content, String quote, String chapter,
                                Integer currentPage, boolean spoiler, boolean anonymous) {
        return new Review(bookId, memberId, content, quote, chapter, currentPage, spoiler, anonymous);
    }

    public void update(String content, String quote, String chapter, Integer currentPage, boolean spoiler) {
        this.content = content;
        this.quote = quote;
        this.chapter = chapter;
        this.currentPage = currentPage;
        this.spoiler = spoiler;
    }

    public void deleteBy(long memberId) {
        assertModifiableBy(memberId);
        deletedAt = Instant.now();
    }

    public void assertModifiableBy(long memberId) {
        if (deletedAt != null) {
            throw new ReviewException(ReviewErrorCode.DELETED_RESOURCE);
        }
        if (this.memberId != memberId) {
            throw new ReviewException(ReviewErrorCode.FORBIDDEN);
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
