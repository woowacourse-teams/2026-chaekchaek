ALTER TABLE review
    DROP INDEX idx_review_feed,
    ADD INDEX idx_review_feed_book (deleted_at, created_at, review_id, book_id),
    ALGORITHM=INPLACE,
    LOCK=NONE;

ANALYZE TABLE review;
