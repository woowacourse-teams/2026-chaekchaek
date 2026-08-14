package com.chaekchaek.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "library_items", uniqueConstraints = @UniqueConstraint(
        name = "uk_library_item_member_book", columnNames = {"member_id", "book_id"}
))
public class LibraryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private long memberId;

    @Column(name = "book_id", nullable = false)
    private long bookId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReadingStatus status;

    @Column(nullable = false)
    private int currentPage;

    @Column(precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(nullable = false, updatable = false)
    private Instant addedAt;

    @Column(nullable = false)
    private Instant readingUpdatedAt;

    private Instant ratingUpdatedAt;

    protected LibraryItem() {
    }

    private LibraryItem(long memberId, long bookId, ReadingStatus status, Integer totalPages,
                        Instant now) {
        this.memberId = memberId;
        this.bookId = bookId;
        this.status = status;
        this.currentPage = initialPage(status, totalPages);
        this.addedAt = now;
        this.readingUpdatedAt = now;
    }

    public static LibraryItem create(long memberId, long bookId, ReadingStatus status,
                                     Integer totalPages, Instant now) {
        if (status == ReadingStatus.FINISHED && totalPages == null) {
            throw invalidReadingState();
        }
        return new LibraryItem(memberId, bookId, status, totalPages, now);
    }

    public void changeStatus(ReadingStatus status, Integer totalPages, Instant now) {
        if (status == ReadingStatus.FINISHED && totalPages == null) {
            throw invalidReadingState();
        }
        int changedPage = status == ReadingStatus.WANT_TO_READ
                ? 0
                : status == ReadingStatus.FINISHED ? totalPages : currentPage;
        boolean changed = this.status != status || this.currentPage != changedPage;
        this.status = status;
        this.currentPage = changedPage;
        updateReadingTimeWhenChanged(changed, now);
    }

    public void changeCurrentPage(int currentPage, Integer totalPages, Instant now) {
        validatePage(currentPage, totalPages);
        ReadingStatus changedStatus = resolveStatusForPage(currentPage, totalPages);
        boolean changed = this.currentPage != currentPage || this.status != changedStatus;
        this.currentPage = currentPage;
        this.status = changedStatus;
        updateReadingTimeWhenChanged(changed, now);
    }

    public void rate(BigDecimal rating, Instant now) {
        validateRating(rating);
        this.rating = rating;
        this.ratingUpdatedAt = now;
    }

    public void removeRating() {
        this.rating = null;
        this.ratingUpdatedAt = null;
    }

    private static int initialPage(ReadingStatus status, Integer totalPages) {
        return status == ReadingStatus.FINISHED ? totalPages : 0;
    }

    private ReadingStatus resolveStatusForPage(int currentPage, Integer totalPages) {
        if (currentPage == 0) {
            return status == ReadingStatus.WANT_TO_READ
                    ? ReadingStatus.WANT_TO_READ
                    : ReadingStatus.READING;
        }
        if (totalPages != null && currentPage == totalPages) {
            return ReadingStatus.FINISHED;
        }
        return ReadingStatus.READING;
    }

    private void updateReadingTimeWhenChanged(boolean changed, Instant now) {
        if (changed) {
            readingUpdatedAt = now;
        }
    }

    private static void validatePage(int currentPage, Integer totalPages) {
        if (currentPage < 0 || (totalPages != null && currentPage > totalPages)) {
            throw invalidReadingState();
        }
    }

    private static void validateRating(BigDecimal rating) {
        if (rating == null || rating.compareTo(new BigDecimal("0.1")) < 0
                || rating.compareTo(new BigDecimal("5.0")) > 0 || rating.scale() > 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private static BusinessException invalidReadingState() {
        return new BusinessException(ErrorCode.INVALID_READING_STATE);
    }

    public Long getId() { return id; }
    public long getMemberId() { return memberId; }
    public long getBookId() { return bookId; }
    public ReadingStatus getStatus() { return status; }
    public int getCurrentPage() { return currentPage; }
    public BigDecimal getRating() { return rating; }
    public Instant getAddedAt() { return addedAt; }
    public Instant getReadingUpdatedAt() { return readingUpdatedAt; }
    public Instant getRatingUpdatedAt() { return ratingUpdatedAt; }
}
