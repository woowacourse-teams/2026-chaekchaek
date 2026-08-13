package com.chaekchaek.review.library;

/** Coordinates the Library-side automatic record creation and progress advance. */
public interface ReadingRecordCoordinator {

    void recordReview(long memberId, long bookId, Integer currentPage, Integer totalPages);

    /**
     * Validates a review page using the book's stored total-pages value and, when supplied, the
     * request value. Implementations must reject a missing required total, conflicts, and pages
     * beyond the effective total with the appropriate business error.
     */
    default void validateReviewPage(long bookId, Integer currentPage, Integer totalPages) {
    }
}
