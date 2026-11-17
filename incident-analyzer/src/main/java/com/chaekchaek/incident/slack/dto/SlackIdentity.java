package com.chaekchaek.incident.slack.dto;

public record SlackIdentity(
        String teamId,
        String userId,
        String botId
) {
}
