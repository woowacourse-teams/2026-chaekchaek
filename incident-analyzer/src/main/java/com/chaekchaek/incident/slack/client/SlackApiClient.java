package com.chaekchaek.incident.slack.client;

import com.chaekchaek.incident.common.JsonHttpClient;
import com.chaekchaek.incident.common.RemoteCallException;
import com.chaekchaek.incident.config.SlackProperties;
import com.chaekchaek.incident.question.dto.PreparedReply;
import com.chaekchaek.incident.question.dto.QuestionTask;
import com.chaekchaek.incident.slack.dto.SlackIdentity;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
public class SlackApiClient {
    private final SlackProperties properties;
    private final JsonHttpClient http;

    public SlackIdentity identity() {
        JsonNode response = checked(http.get(
                properties.apiUrl().resolve("auth.test"),
                headers(properties.botToken())
        ));
        return new SlackIdentity(
                response.path("team_id").asText(),
                response.path("user_id").asText(),
                response.path("bot_id").asText()
        );
    }

    public URI connectionUrl() {
        JsonNode response = checked(http.post(
                properties.apiUrl().resolve("apps.connections.open"),
                Map.of(),
                headers(properties.appToken()),
                Duration.ofSeconds(5)
        ));
        URI url = URI.create(response.path("url").asText());
        if (!"wss".equals(url.getScheme()) || url.getHost() == null || !url.getHost().endsWith(".slack.com")) {
            throw new RemoteCallException("INVALID_SLACK_SOCKET_URL");
        }
        return url;
    }

    public JsonNode parentMessage(QuestionTask task) {
        URI uri = UriComponentsBuilder.fromUri(properties.apiUrl().resolve("conversations.history"))
                .queryParam("channel", task.channel())
                .queryParam("latest", task.threadTs())
                .queryParam("inclusive", true)
                .queryParam("limit", 1)
                .build()
                .toUri();
        return checked(http.get(uri, headers(properties.botToken()))).path("messages").path(0);
    }

    public void reply(QuestionTask task, PreparedReply reply) {
        var body = new LinkedHashMap<String, Object>(reply.message());
        body.put("channel", task.channel());
        body.put("thread_ts", task.threadTs());
        body.put("reply_broadcast", false);
        JsonNode response = checked(http.post(
                properties.apiUrl().resolve("chat.postMessage"),
                body,
                headers(properties.botToken()),
                Duration.ofSeconds(5)
        ));
        if (!task.channel().equals(response.path("channel").asText())) {
            throw new RemoteCallException("UNEXPECTED_SLACK_CHANNEL");
        }
    }

    private JsonNode checked(JsonNode response) {
        if (!response.path("ok").asBoolean(false)) {
            String code = response.path("error").asText("");
            if (!code.matches("[a-z_]{1,64}")) {
                code = "invalid_response";
            }
            throw new RemoteCallException("SLACK_" + code);
        }
        return response;
    }

    private Map<String, String> headers(String token) {
        return Map.of("Authorization", "Bearer " + token);
    }
}
