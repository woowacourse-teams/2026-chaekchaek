package com.chaekchaek.library.domain;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "library_item",
        check = {
                @CheckConstraint(
                        name = "ck_library_item_rating_state",
                        constraint = "(rating is null and rating_updated_at is null) "
                                + "or (rating is not null and rating_updated_at is not null)"
                )
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_library_item_member_book",
                columnNames = {"member_id", "book_id"}
        )
)
public class LibraryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private long memberId;

    @Column(name = "book_id", nullable = false)
    private long bookId;

    @Column(precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(nullable = false, updatable = false)
    private Instant addedAt;

    private Instant ratingUpdatedAt;

    protected LibraryItem() {
    }

    private LibraryItem(long memberId, long bookId, Instant now) {
        this.memberId = memberId;
        this.bookId = bookId;
        this.addedAt = now;
    }

    public static LibraryItem create(long memberId, long bookId, Instant now) {
        return new LibraryItem(memberId, bookId, now);
    }

    public static LibraryItem createForRating(
            long memberId,
            long bookId,
            BigDecimal rating,
            Instant now
    ) {
        LibraryItem item = new LibraryItem(memberId, bookId, now);
        item.rate(rating, now);
        return item;
    }

    public void rate(BigDecimal rating, Instant now) {
        validateRating(rating);
        this.rating = rating;
        this.ratingUpdatedAt = now;
    }

    private static void validateRating(BigDecimal rating) {
        if (rating == null || rating.compareTo(new BigDecimal("0.1")) < 0
                || rating.compareTo(new BigDecimal("5.0")) > 0 || rating.scale() > 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    public void removeRating() {
        this.rating = null;
        this.ratingUpdatedAt = null;
    }

    public Long getId() { return id; }
    public long getMemberId() { return memberId; }
    public long getBookId() { return bookId; }
    public BigDecimal getRating() { return rating; }
    public Instant getAddedAt() { return addedAt; }
    public Instant getRatingUpdatedAt() { return ratingUpdatedAt; }
}
