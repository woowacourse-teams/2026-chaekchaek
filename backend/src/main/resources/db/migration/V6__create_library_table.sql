CREATE TABLE IF NOT EXISTS `library` (
                                         `id` BIGINT NOT NULL AUTO_INCREMENT,
                                         `member_id` BIGINT NOT NULL,
                                         `name` VARCHAR(104) NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_libraries_member_name` (`member_id`, `name`),
    CONSTRAINT `fk_libraries_member`
    FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`)
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_0900_ai_ci;