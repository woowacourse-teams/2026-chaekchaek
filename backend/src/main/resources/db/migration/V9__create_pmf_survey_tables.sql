CREATE TABLE `pmf_survey_response` (
    `response_id` BIGINT NOT NULL AUTO_INCREMENT,
    `actor_id` BIGINT NOT NULL,
    `submitted_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`response_id`),
    UNIQUE KEY `uk_pmf_survey_response_actor` (`actor_id`),
    CONSTRAINT `fk_pmf_survey_response_actor`
        FOREIGN KEY (`actor_id`) REFERENCES `actor` (`actor_id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `pmf_survey_answer` (
    `response_id` BIGINT NOT NULL,
    `question_code` VARCHAR(50) NOT NULL,
    `answer_code` VARCHAR(50) NOT NULL,
    PRIMARY KEY (`response_id`, `question_code`, `answer_code`),
    CONSTRAINT `fk_pmf_survey_answer_response`
        FOREIGN KEY (`response_id`) REFERENCES `pmf_survey_response` (`response_id`)
        ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
