package com.chaekchaek.incident.slack;

import com.chaekchaek.incident.alert.domain.IncidentReference;
import com.chaekchaek.incident.config.SlackProperties;
import com.chaekchaek.incident.question.domain.QuestionRejectedException;
import com.chaekchaek.incident.question.dto.QuestionTask;
import com.chaekchaek.incident.slack.client.SlackApiClient;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
public class ParentAlertResolver {
    private static final Pattern REFERENCE = Pattern.compile(
            "CC-INCIDENT\\|(dev|prod)\\|([A-Za-z0-9_-]{1,128})\\|([0-9T:.+Z-]{20,40})"
    );
    private final SlackApiClient slack;
    private final SlackProperties properties;

    public IncidentReference resolve(QuestionTask task) {
        JsonNode parent = slack.parentMessage(task);
        if (!task.threadTs().equals(parent.path("ts").asText())
                || !properties.alertBotId(task.environment()).equals(parent.path("bot_id").asText())) {
            throw new QuestionRejectedException("등록된 Grafana 원본 알림의 스레드에서 질문해 주세요.");
        }
        String text = parentText(parent);
        var references = references(text);
        if (references.size() > 1) {
            references = references
                    .stream()
                    .filter(reference -> task.question().contains(reference.fingerprint()))
                    .toList();
        }
        if (references.size() != 1 || !references.getFirst().environment().equals(task.environment())) {
            throw new QuestionRejectedException(
                    "장애 참조 하나를 확인할 수 없습니다. 설정 변경 후 받은 원본 알림을 사용하고, 여러 장애가 있으면 질문에 해당 fingerprint를 포함해 주세요."
            );
        }
        return references.getFirst();
    }

    private String parentText(JsonNode parent) {
        var text = new StringBuilder(parent.path("text").asText(""));
        for (JsonNode attachment : parent.path("attachments")) {
            text.append('\n').append(attachment.path("text").asText(""));
        }
        if (text.length() > 40000) {
            throw new QuestionRejectedException("원본 알림이 너무 깁니다. 장애별로 나뉜 알림에서 질문해 주세요.");
        }
        return text.toString();
    }

    private List<IncidentReference> references(String text) {
        var references = new LinkedHashSet<IncidentReference>();
        var matcher = REFERENCE.matcher(text);
        while (matcher.find() && references.size() < 100) {
            try {
                references.add(new IncidentReference(
                        matcher.group(1),
                        matcher.group(2),
                        Instant.parse(matcher.group(3))
                ));
            } catch (IllegalArgumentException | DateTimeException exception) {
                throw new QuestionRejectedException("원본 알림의 장애 참조가 올바르지 않습니다.");
            }
        }
        return List.copyOf(references);
    }
}
