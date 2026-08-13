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

    private Integer totalPages;

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
        this.totalPages = totalPages;
        this.currentPage = initialPage(status, totalPages);
        this.addedAt = now;
        this.readingUpdatedAt = now;
    }

    public static LibraryItem create(long memberId, long bookId, ReadingStatus status,
                                     Integer totalPages, Instant now) {
        validateTotalPages(totalPages);
        if (status == ReadingStatus.FINISHED && totalPages == null) {
            throw new IllegalArgumentException("Finished books require total pages");
        }
        return new LibraryItem(memberId, bookId, status, totalPages, now);
    }

    public void changeStatus(ReadingStatus status, Integer totalPages, Instant now) {
        validateTotalPages(totalPages);
        Integer effectiveTotalPages = totalPages != null ? totalPages : this.totalPages;
        if (status == ReadingStatus.FINISHED && effectiveTotalPages == null) {
            throw new IllegalArgumentException("Finished books require total pages");
        }
        int changedPage = status == ReadingStatus.WANT_TO_READ
                ? 0
                : status == ReadingStatus.FINISHED ? effectiveTotalPages : currentPage;
        boolean changed = this.status != status || this.currentPage != changedPage;
        this.status = status;
        this.totalPages = effectiveTotalPages;
        this.currentPage = changedPage;
        updateReadingTimeWhenChanged(changed, now);
    }

    public void changeCurrentPage(int currentPage, Integer totalPages, Instant now) {
        validatePage(currentPage, totalPages);
        Integer effectiveTotalPages = totalPages != null ? totalPages : this.totalPages;
        validatePage(currentPage, effectiveTotalPages);
        ReadingStatus changedStatus = resolveStatusForPage(currentPage, effectiveTotalPages);
        boolean changed = this.currentPage != currentPage || this.status != changedStatus;
        this.currentPage = currentPage;
        this.totalPages = effectiveTotalPages;
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
            throw new IllegalArgumentException("Current page must be within total pages");
        }
    }

    private static void validateRating(BigDecimal rating) {
        if (rating == null || rating.compareTo(new BigDecimal("0.1")) < 0
                || rating.compareTo(new BigDecimal("5.0")) > 0 || rating.scale() > 1) {
            throw new IllegalArgumentException("Rating must be between 0.1 and 5.0 in 0.1 steps");
        }
    }

    private static void validateTotalPages(Integer totalPages) {
        if (totalPages != null && totalPages <= 0) {
            throw new IllegalArgumentException("Total pages must be positive");
        }
    }

    public Long getId() { return id; }
    public long getMemberId() { return memberId; }
    public long getBookId() { return bookId; }
    public ReadingStatus getStatus() { return status; }
    public int getCurrentPage() { return currentPage; }
    public Integer getTotalPages() { return totalPages; }
    public BigDecimal getRating() { return rating; }
    public Instant getAddedAt() { return addedAt; }
    public Instant getReadingUpdatedAt() { return readingUpdatedAt; }
    public Instant getRatingUpdatedAt() { return ratingUpdatedAt; }
}
