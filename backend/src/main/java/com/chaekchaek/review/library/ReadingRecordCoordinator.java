package com.chaekchaek.review.library;

/** Coordinates the Library-side automatic record creation and progress advance. */
public interface ReadingRecordCoordinator {

    void recordReview(long memberId, long bookId, Integer currentPage, Integer totalPages);
}
