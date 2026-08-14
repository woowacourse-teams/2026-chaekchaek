package com.chaekchaek.review.book;

/**
 * Review's read boundary for a registered book.
 * The Book feature supplies the implementation from its persistent book lookup.
 */
public interface ReviewBookReader {

    void validateBookExists(long bookId);
}
