package com.chaekchaek.incident.question.dto;

import java.util.Map;

public record PreparedReply(
        Map<String, Object> message,
        String contextSummary,
        String model,
        long inputTokens,
        long outputTokens
) {
    public PreparedReply {
        message = Map.copyOf(message);
    }

    public static PreparedReply notice(String text) {
        return new PreparedReply(
                Map.of("text", text, "mrkdwn", false, "unfurl_links", false, "unfurl_media", false),
                text,
                "none",
                0,
                0
        );
    }
}
