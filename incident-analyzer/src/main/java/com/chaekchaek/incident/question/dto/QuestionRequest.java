package com.chaekchaek.incident.question.dto;

import java.util.Set;

public record QuestionRequest(
        String eventId,
        String environment,
        String channel,
        String threadTs,
        String question
) {
    public QuestionRequest {
        if (eventId == null || !eventId.matches("Ev[A-Za-z0-9]{1,120}")
                || environment == null || !Set.of("dev", "prod").contains(environment)
                || channel == null || !channel.matches("[CG][A-Z0-9]+")
                || threadTs == null || !threadTs.matches("[0-9]{10,16}\\.[0-9]{6}")
                || question == null || question.isBlank() || question.length() > 2000) {
            throw new IllegalArgumentException("알림 스레드와 2000자 이내의 질문이 필요합니다");
        }
    }
}
