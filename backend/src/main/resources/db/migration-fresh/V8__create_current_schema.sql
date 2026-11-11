-- Fresh-install schema snapshot, aligned with the application schema after V8.
-- Keep historical migrations in db/migration unchanged. Put future migrations
-- (V9 and later) in db/migration-next so both fresh and legacy databases see them.

CREATE TABLE `member` (
    `member_id` BIGINT NOT NULL AUTO_INCREMENT,
    `nickname` VARCHAR(10) NULL,
    `profile_image_url` TEXT NULL,
    `anonymous_nickname` VARCHAR(100) NOT NULL,
    `display_anonymous` BOOLEAN NOT NULL,
    `account_status` VARCHAR(20) NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    `withdrawn_at` DATETIME(6) NULL,
    PRIMARY KEY (`member_id`),
    UNIQUE KEY `uk_member_nickname` (`nickname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `actor` (
    `actor_id` BIGINT NOT NULL AUTO_INCREMENT,
    `member_id` BIGINT NULL,
    `actor_type` VARCHAR(20) NOT NULL,
    `guest_token_hash` VARCHAR(64) NULL,
    `guest_nickname` VARCHAR(100) NULL,
    `created_at` DATETIME(6) NOT NULL,
    `guest_token_issued_at` DATETIME(6) NULL,
    `expires_at` DATETIME(6) NULL,
    `revoked_at` DATETIME(6) NULL,
    PRIMARY KEY (`actor_id`),
    CONSTRAINT `uk_actor_member` UNIQUE (`member_id`),
    CONSTRAINT `uk_actor_guest_token_hash` UNIQUE (`guest_token_hash`),
    CONSTRAINT `fk_actor_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `social_account` (
    `social_account_id` BIGINT NOT NULL AUTO_INCREMENT,
    `member_id` BIGINT NOT NULL,
    `provider` VARCHAR(50) NOT NULL,
    `provider_user_id` VARCHAR(255) NOT NULL,
    `connected_at` DATETIME(6) NOT NULL,
    `provider_refresh_token` VARCHAR(2048) NULL,
    PRIMARY KEY (`social_account_id`),
    UNIQUE KEY `uk_social_account_provider_user_id` (`provider`, `provider_user_id`),
    KEY `ix_social_account_member_id` (`member_id`),
    CONSTRAINT `fk_social_account_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `refresh_token` (
    `refresh_token_id` BIGINT NOT NULL AUTO_INCREMENT,
    `member_id` BIGINT NOT NULL,
    `token_hash` VARCHAR(64) NOT NULL,
    `issued_at` DATETIME(6) NOT NULL,
    `expires_at` DATETIME(6) NOT NULL,
    `revoked_at` DATETIME(6) NULL,
    PRIMARY KEY (`refresh_token_id`),
    UNIQUE KEY `uk_refresh_token_token_hash` (`token_hash`),
    KEY `ix_refresh_token_member_id` (`member_id`),
    CONSTRAINT `fk_refresh_token_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `book` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `isbn13` VARCHAR(13) NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `cover_image_url` VARCHAR(1000) NOT NULL,
    `spine_image_url` VARCHAR(1000) NULL,
    `back_image_url` VARCHAR(1000) NULL,
    `description` TEXT NULL,
    `question` VARCHAR(1000) NULL,
    `plot` TEXT NULL,
    `extract` TEXT NULL,
    `publisher` VARCHAR(255) NOT NULL,
    `category` VARCHAR(255) NOT NULL,
    `published_date` DATE NULL,
    `total_pages` INT NULL,
    `purchase_url` VARCHAR(1000) NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_books_isbn13` UNIQUE (`isbn13`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `book_author` (
    `book_id` BIGINT NOT NULL,
    `display_order` INT NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    KEY `ix_book_author_book_id` (`book_id`),
    CONSTRAINT `fk_book_author_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `book_translator` (
    `book_id` BIGINT NOT NULL,
    `display_order` INT NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    KEY `ix_book_translator_book_id` (`book_id`),
    CONSTRAINT `fk_book_translator_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `recommended_book` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `book_id` BIGINT NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_recommended_books_book_id` UNIQUE (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `library` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `member_id` BIGINT NOT NULL,
    `name` VARCHAR(104) NOT NULL,
    `is_default` BOOLEAN NOT NULL DEFAULT FALSE,
    `display_order` BIGINT NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NULL,
    `default_member_id` BIGINT GENERATED ALWAYS AS (
        CASE WHEN `is_default` THEN `member_id` ELSE NULL END
    ) STORED,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_libraries_member_name` (`member_id`, `name`),
    UNIQUE KEY `uk_library_default_member` (`default_member_id`),
    KEY `ix_library_member_order_id` (`member_id`, `display_order`, `id`),
    CONSTRAINT `fk_libraries_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`),
    CONSTRAINT `ck_library_display_order_positive` CHECK (`display_order` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `library_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `member_id` BIGINT NOT NULL,
    `book_id` BIGINT NOT NULL,
    `status` VARCHAR(20) NULL,
    `current_page` INT NULL,
    `rating` DECIMAL(2,1) NULL,
    `added_at` DATETIME(6) NOT NULL,
    `reading_updated_at` DATETIME(6) NULL,
    `rating_updated_at` DATETIME(6) NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_library_item_member_book` (`member_id`, `book_id`),
    CONSTRAINT `ck_library_item_reading_state` CHECK (
        (`status` IS NULL AND `current_page` IS NULL AND `reading_updated_at` IS NULL)
        OR (`status` IS NOT NULL AND `current_page` IS NOT NULL AND `reading_updated_at` IS NOT NULL)
    ),
    CONSTRAINT `ck_library_item_rating_state` CHECK (
        (`rating` IS NULL AND `rating_updated_at` IS NULL)
        OR (`rating` IS NOT NULL AND `rating_updated_at` IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `library_book` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `library_id` BIGINT NOT NULL,
    `book_id` BIGINT NOT NULL,
    `display_order` BIGINT NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_library_book_library_book` (`library_id`, `book_id`),
    KEY `ix_library_book_library_order_id` (`library_id`, `display_order`, `id`),
    KEY `ix_library_book_book_library` (`book_id`, `library_id`),
    CONSTRAINT `fk_library_book_library` FOREIGN KEY (`library_id`) REFERENCES `library` (`id`),
    CONSTRAINT `fk_library_book_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`),
    CONSTRAINT `ck_library_book_display_order_positive` CHECK (`display_order` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `review` (
    `review_id` BIGINT NOT NULL AUTO_INCREMENT,
    `book_id` BIGINT NOT NULL,
    `actor_id` BIGINT NOT NULL,
    `content` VARCHAR(1000) NOT NULL,
    `quote` VARCHAR(500) NULL,
    `chapter` VARCHAR(255) NULL,
    `current_page` INT NULL,
    `is_spoiler` BOOLEAN NOT NULL,
    `anonymous` BOOLEAN NOT NULL,
    `deleted_at` DATETIME(6) NULL,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`review_id`),
    KEY `idx_review_book_created` (`book_id`, `created_at`),
    KEY `idx_review_book_page` (`book_id`, `current_page`),
    KEY `idx_review_actor_book` (`actor_id`, `book_id`),
    KEY `idx_review_feed` (`deleted_at`, `created_at`, `review_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `reply` (
    `reply_id` BIGINT NOT NULL AUTO_INCREMENT,
    `review_id` BIGINT NOT NULL,
    `actor_id` BIGINT NOT NULL,
    `content` VARCHAR(200) NOT NULL,
    `anonymous` BOOLEAN NOT NULL,
    `deleted_at` DATETIME(6) NULL,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`reply_id`),
    KEY `idx_reply_review_created` (`review_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `reaction` (
    `review_id` BIGINT NOT NULL,
    `actor_id` BIGINT NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`review_id`, `actor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `reply_reaction` (
    `reply_id` BIGINT NOT NULL,
    `actor_id` BIGINT NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`reply_id`, `actor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

