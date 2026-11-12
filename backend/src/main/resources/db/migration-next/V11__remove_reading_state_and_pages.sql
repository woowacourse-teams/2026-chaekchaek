ALTER TABLE `library_item`
    DROP CHECK `ck_library_item_reading_state`,
    DROP COLUMN `status`,
    DROP COLUMN `current_page`,
    DROP COLUMN `reading_updated_at`;

SET @review_page_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'review'
      AND index_name = 'idx_review_book_page'
);

SET @drop_review_page_index_sql = IF(
    @review_page_index_exists > 0,
    'ALTER TABLE `review` DROP INDEX `idx_review_book_page`',
    'SELECT 1'
);

PREPARE drop_review_page_index_statement FROM @drop_review_page_index_sql;
EXECUTE drop_review_page_index_statement;
DEALLOCATE PREPARE drop_review_page_index_statement;

ALTER TABLE `review`
    DROP COLUMN `current_page`;

ALTER TABLE `book`
    DROP COLUMN `total_pages`;
