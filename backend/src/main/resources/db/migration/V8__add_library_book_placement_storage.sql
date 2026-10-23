-- Run with application writes stopped. Validation fails before schema changes.
CREATE TEMPORARY TABLE `v8_library_migration_guard` (
    `validation` VARCHAR(64) NOT NULL,
    `violations` BIGINT NOT NULL,
    CONSTRAINT `ck_v8_library_migration_guard` CHECK (`violations` = 0)
);

INSERT INTO `v8_library_migration_guard`
SELECT 'duplicate_member_book', COUNT(*)
FROM (
    SELECT `member_id`, `book_id`
    FROM `library_item`
    GROUP BY `member_id`, `book_id`
    HAVING COUNT(*) > 1
) AS duplicate_records;

INSERT INTO `v8_library_migration_guard`
SELECT 'invalid_reading_or_rating_state', COUNT(*)
FROM `library_item`
WHERE NOT (
    (`status` IS NULL AND `current_page` IS NULL AND `reading_updated_at` IS NULL)
    OR (`status` IS NOT NULL AND `current_page` IS NOT NULL AND `reading_updated_at` IS NOT NULL)
) OR (`rating` IS NULL) <> (`rating_updated_at` IS NULL);

SET @v8_has_library_id = (
    SELECT COUNT(*) > 0
    FROM `information_schema`.`COLUMNS`
    WHERE `TABLE_SCHEMA` = DATABASE()
      AND `TABLE_NAME` = 'library_item'
      AND `COLUMN_NAME` = 'library_id'
);

SET @v8_sql = IF(@v8_has_library_id,
    'INSERT INTO v8_library_migration_guard
     SELECT ''invalid_library_owner'', COUNT(*)
     FROM library_item li
     LEFT JOIN library l ON l.id = li.library_id
     WHERE l.id IS NULL OR li.member_id <> l.member_id',
    'SELECT 1');
PREPARE v8_statement FROM @v8_sql;
EXECUTE v8_statement;
DEALLOCATE PREPARE v8_statement;

ALTER TABLE `library`
    ADD COLUMN `is_default` BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN `display_order` BIGINT NULL;

UPDATE `library` AS library_row
    JOIN (
        SELECT
            ordered_rows.`id`,
            ROW_NUMBER() OVER (
                PARTITION BY ordered_rows.`member_id`
                ORDER BY ordered_rows.`created_at`, ordered_rows.`id`
            ) AS `display_order`
        FROM `library` AS ordered_rows
    ) AS library_order ON library_order.`id` = library_row.`id`
SET library_row.`display_order` = library_order.`display_order`;

ALTER TABLE `library`
    MODIFY COLUMN `display_order` BIGINT NOT NULL,
    ADD COLUMN `default_member_id` BIGINT GENERATED ALWAYS AS (
        CASE WHEN `is_default` THEN `member_id` ELSE NULL END
    ) STORED,
    ADD CONSTRAINT `ck_library_display_order_positive`
        CHECK (`display_order` > 0),
    ADD UNIQUE KEY `uk_library_default_member` (`default_member_id`),
    ADD KEY `ix_library_member_order_id` (`member_id`, `display_order`, `id`);

CREATE TABLE `library_book` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `library_id` BIGINT NOT NULL,
    `book_id` BIGINT NOT NULL,
    `display_order` BIGINT NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_library_book_library_book` (`library_id`, `book_id`),
    KEY `ix_library_book_library_order_id` (`library_id`, `display_order`, `id`),
    KEY `ix_library_book_book_library` (`book_id`, `library_id`),
    CONSTRAINT `fk_library_book_library`
        FOREIGN KEY (`library_id`) REFERENCES `library` (`id`),
    CONSTRAINT `fk_library_book_book`
        FOREIGN KEY (`book_id`) REFERENCES `book` (`id`),
    CONSTRAINT `ck_library_book_display_order_positive`
        CHECK (`display_order` > 0)
) ENGINE=InnoDB;

-- Preserve explicit legacy placements only where the development column exists.
SET @v8_sql = IF(@v8_has_library_id,
    'INSERT INTO library_book (library_id, book_id, display_order)
     SELECT library_id, book_id,
            ROW_NUMBER() OVER (PARTITION BY library_id ORDER BY added_at, id)
     FROM library_item',
    'SELECT 1');
PREPARE v8_statement FROM @v8_sql;
EXECUTE v8_statement;
DEALLOCATE PREPARE v8_statement;

-- Check every copied pair before removing its original reference.
SET @v8_sql = IF(@v8_has_library_id,
    'INSERT INTO v8_library_migration_guard
     SELECT ''missing_copied_placement'', COUNT(*)
     FROM library_item li
     LEFT JOIN library_book lb ON lb.library_id = li.library_id AND lb.book_id = li.book_id
     WHERE lb.id IS NULL',
    'SELECT 1');
PREPARE v8_statement FROM @v8_sql;
EXECUTE v8_statement;
DEALLOCATE PREPARE v8_statement;

-- Production already has an equivalent unique index. Keep its existing name.
SET @v8_has_member_book_unique = (
    SELECT COUNT(*) > 0
    FROM (
        SELECT `INDEX_NAME`
        FROM `information_schema`.`STATISTICS`
        WHERE `TABLE_SCHEMA` = DATABASE()
          AND `TABLE_NAME` = 'library_item'
          AND `NON_UNIQUE` = 0
        GROUP BY `INDEX_NAME`
        HAVING COUNT(*) = 2
           AND GROUP_CONCAT(`COLUMN_NAME` ORDER BY `SEQ_IN_INDEX`)
               IN ('member_id,book_id', 'book_id,member_id')
    ) AS member_book_indexes
);
SET @v8_sql = IF(@v8_has_member_book_unique,
    'SELECT 1',
    'ALTER TABLE library_item ADD UNIQUE KEY uk_library_item_member_book (member_id, book_id)');
PREPARE v8_statement FROM @v8_sql;
EXECUTE v8_statement;
DEALLOCATE PREPARE v8_statement;

-- Discover legacy names rather than assuming identical FK/index names in each DB.
SET @v8_drop_foreign_keys = (
    SELECT GROUP_CONCAT(DISTINCT CONCAT('DROP FOREIGN KEY `',
        REPLACE(`CONSTRAINT_NAME`, '`', '``'), '`') SEPARATOR ', ')
    FROM `information_schema`.`KEY_COLUMN_USAGE`
    WHERE `TABLE_SCHEMA` = DATABASE()
      AND `TABLE_NAME` = 'library_item'
      AND `COLUMN_NAME` = 'library_id'
      AND `REFERENCED_TABLE_NAME` IS NOT NULL
);
SET @v8_sql = IF(@v8_drop_foreign_keys IS NULL,
    'SELECT 1', CONCAT('ALTER TABLE library_item ', @v8_drop_foreign_keys));
PREPARE v8_statement FROM @v8_sql;
EXECUTE v8_statement;
DEALLOCATE PREPARE v8_statement;

SET @v8_drop_indexes = (
    SELECT GROUP_CONCAT(DISTINCT CONCAT('DROP INDEX `',
        REPLACE(`INDEX_NAME`, '`', '``'), '`') SEPARATOR ', ')
    FROM `information_schema`.`STATISTICS`
    WHERE `TABLE_SCHEMA` = DATABASE()
      AND `TABLE_NAME` = 'library_item'
      AND `COLUMN_NAME` = 'library_id'
      AND `INDEX_NAME` <> 'PRIMARY'
);
SET @v8_sql = IF(@v8_drop_indexes IS NULL,
    'SELECT 1', CONCAT('ALTER TABLE library_item ', @v8_drop_indexes));
PREPARE v8_statement FROM @v8_sql;
EXECUTE v8_statement;
DEALLOCATE PREPARE v8_statement;

SET @v8_sql = IF(@v8_has_library_id,
    'ALTER TABLE library_item DROP COLUMN library_id', 'SELECT 1');
PREPARE v8_statement FROM @v8_sql;
EXECUTE v8_statement;
DEALLOCATE PREPARE v8_statement;

ALTER TABLE `library_item`
    MODIFY COLUMN `status` VARCHAR(20) NULL,
    MODIFY COLUMN `current_page` INT NULL,
    MODIFY COLUMN `reading_updated_at` DATETIME(6) NULL,
    ADD CONSTRAINT `ck_library_item_reading_state`
        CHECK (
            (`status` IS NULL AND `current_page` IS NULL AND `reading_updated_at` IS NULL)
            OR (`status` IS NOT NULL AND `current_page` IS NOT NULL AND `reading_updated_at` IS NOT NULL)
        ),
    ADD CONSTRAINT `ck_library_item_rating_state`
        CHECK (
            (`rating` IS NULL AND `rating_updated_at` IS NULL)
            OR (`rating` IS NOT NULL AND `rating_updated_at` IS NOT NULL)
        );

DROP TEMPORARY TABLE `v8_library_migration_guard`;
