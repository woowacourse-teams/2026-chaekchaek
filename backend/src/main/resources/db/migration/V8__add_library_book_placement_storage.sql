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
