package com.chaekchaek.incident.slack;

import com.chaekchaek.incident.config.SlackProperties;
import com.chaekchaek.incident.question.dto.QuestionRequest;
import com.chaekchaek.incident.question.service.QuestionService;
import com.chaekchaek.incident.slack.dto.SlackIdentity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
public class SlackMentionHandler {
    private final SlackProperties properties;
    private final QuestionService questions;

    public void accept(JsonNode payload, SlackIdentity identity) {
        JsonNode event = payload.path("event");
        String channel = event.path("channel").asText();
        String threadTs = event.path("thread_ts").asText();
        String user = event.path("user").asText();
        String mention = "<@" + identity.userId() + ">";
        if (!properties.enabled() || !"event_callback".equals(payload.path("type").asText())
                || !"app_mention".equals(event.path("type").asText())
                || !identity.teamId().equals(payload.path("team_id").asText())
                || event.hasNonNull("bot_id") || identity.userId().equals(user) || !user.matches("[UW][A-Z0-9]+")
                || threadTs.isBlank() || threadTs.equals(event.path("ts").asText())
                || !event.path("text").asText().contains(mention)) {
            return;
        }
        properties.environment(channel).ifPresent(environment -> questions.accept(new QuestionRequest(
                payload.path("event_id").asText(),
                environment,
                channel,
                threadTs,
                cleanQuestion(event.path("text").asText().replace(mention, ""))
        )));
    }

    private String cleanQuestion(String text) {
        return text.replaceAll("(?:sk-|xox[baprs]-|xapp-)[A-Za-z0-9_-]+", "[비밀값 제거]")
                .replaceAll("https://hooks\\.slack\\.com/services/[^\\s>]+", "[웹훅 주소 제거]")
                .replaceAll("<@[A-Z0-9]+>", "[사용자]")
                .strip();
    }
}
