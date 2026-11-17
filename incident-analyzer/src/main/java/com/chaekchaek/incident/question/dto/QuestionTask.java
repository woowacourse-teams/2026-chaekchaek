package com.chaekchaek.incident.question.dto;

import java.time.Instant;

public record QuestionTask(
        String id,
        String environment,
        String channel,
        String threadTs,
        String question,
        Instant receivedAt,
        Instant deadlineAt,
        int attempts,
        String resultJson
) {
}
