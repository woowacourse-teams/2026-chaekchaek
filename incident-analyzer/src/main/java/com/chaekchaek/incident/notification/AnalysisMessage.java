package com.chaekchaek.incident.notification;

import com.chaekchaek.incident.alert.dto.IncidentSnapshot;
import com.chaekchaek.incident.analysis.dto.AnalysisResult;
import com.chaekchaek.incident.config.IncidentProperties;
import com.chaekchaek.incident.evidence.QueryName;
import com.chaekchaek.incident.evidence.dto.QueryExecution;
import com.chaekchaek.incident.question.dto.PreparedReply;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class AnalysisMessage {
    private final IncidentProperties properties;
    private final JsonMapper mapper;

    public PreparedReply reply(IncidentSnapshot incident, AnalysisResult result) {
        return reply(incident, result, List.of());
    }

    public PreparedReply reply(
            IncidentSnapshot incident,
            AnalysisResult result,
            List<QueryExecution> queries
    ) {
        String context = result.report().summary() + "\n" + result.report().hypotheses()
                .stream()
                .map(candidate -> candidate.candidate() + ": " + candidate.nextCheck())
                .collect(Collectors.joining("\n"));
        return new PreparedReply(
                message(incident, result, queries),
                context.substring(0, Math.min(context.length(), 1800)),
                result.model(),
                result.inputTokens(),
                result.outputTokens()
        );
    }

    private Map<String, Object> message(
            IncidentSnapshot task,
            AnalysisResult result,
            List<QueryExecution> queries
    ) {
        var blocks = new ArrayList<Map<String, Object>>();
        String time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
                .withZone(ZoneId.of("Asia/Seoul"))
                .format(task.startsAt());
        blocks.add(section("[" + task.environment() + "] 장애 질문 답변 " + task.id().substring(0, 12)
                + "\n" + task.alertName() + " | " + time + "\n" + result.report().summary()
                + "\n판단: " + result.report().assessment()));
        result.report().observedFacts().forEach(fact -> blocks.add(section("관찰 내용: " + fact.description()
                + "\n근거: " + String.join(", ", fact.evidenceIds()))));
        result.report().hypotheses().forEach(candidate -> blocks.add(section("원인 후보: " + candidate.candidate()
                + "\n근거: " + String.join(", ", candidate.evidenceIds())
                + "\n확인: " + candidate.nextCheck() + "\n판단 기준: " + candidate.expectedObservation())));
        result.evidence().forEach(item -> blocks.add(section(item.id() + " [" + item.source() + "] "
                + item.description() + "\n" + item.values().entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", ")))));
        blocks.add(section("자료 제한: " + String.join("\n", result.evidenceNotes())
                + "\n" + String.join("\n", result.report().missingEvidence())));
        blocks.add(Map.of(
                "type",
                "actions",
                "elements",
                List.of(Map.of(
                        "type",
                        "button",
                        "text",
                        Map.of("type", "plain_text", "text", "Grafana에서 근거 확인"),
                        "url",
                        evidenceLink(task)
                ))
        ));
        appendQueries(blocks, queries);
        return Map.of(
                "text",
                "책췍 " + task.environment() + " 장애 분석 " + task.id().substring(0, 12),
                "blocks",
                List.copyOf(blocks),
                "unfurl_links",
                false,
                "unfurl_media",
                false
        );
    }

    private void appendQueries(List<Map<String, Object>> blocks, List<QueryExecution> queries) {
        if (queries.isEmpty()) {
            return;
        }
        var buttons = new ArrayList<Map<String, Object>>();
        for (QueryExecution query : queries) {
            blocks.add(section("추가 조회: " + query.description() + "\n상태: " + query.status()
                    + " | 근거: " + query.evidenceId() + "\n구간: " + query.from() + " ~ " + query.to()
                    + "\n쿼리: " + query.expression() + "\n제한: " + String.join(", ", query.limitations())));
            buttons.add(Map.of(
                    "type",
                    "button",
                    "text",
                    Map.of("type", "plain_text", "text", "추가 조회 " + (buttons.size() + 1) + " 재현"),
                    "url",
                    exploreLink(
                            query.source(),
                            query.expression(),
                            query.from(),
                            query.to(),
                            !QueryName.ERROR_REQUEST_DETAILS.getId().equals(query.queryId())
                    )
            ));
        }
        blocks.add(Map.of("type", "actions", "elements", List.copyOf(buttons)));
    }

    private String evidenceLink(IncidentSnapshot task) {
        return exploreLink(
                "loki",
                QueryName.ERROR_REQUEST_DETAILS.expression(task.environment()),
                task.startsAt().minusSeconds(300),
                task.startsAt(),
                false
        );
    }

    private String exploreLink(
            String source,
            String expression,
            Instant from,
            Instant to,
            boolean instant
    ) {
        var query = new LinkedHashMap<String, Object>();
        query.put("refId", "A");
        query.put("datasource", Map.of("uid", source, "type", source));
        query.put("expr", expression);
        if (instant && "prometheus".equals(source)) {
            query.put("instant", true);
            query.put("range", false);
        }
        if ("loki".equals(source)) {
            String queryType = "range";
            if (instant) {
                queryType = "instant";
            }
            query.put("queryType", queryType);
        }
        var pane = Map.of(
                "datasource",
                source,
                "queries",
                List.of(query),
                "range",
                Map.of(
                        "from",
                        String.valueOf(from.toEpochMilli()),
                        "to",
                        String.valueOf(to.toEpochMilli())
                )
        );
        return UriComponentsBuilder.fromUri(properties.grafanaUrl())
                .path("/explore")
                .queryParam("schemaVersion", 1)
                .queryParam("orgId", properties.orgId())
                .queryParam("panes", "{panes}")
                .encode()
                .buildAndExpand(mapper.writeValueAsString(Map.of("incident", pane)))
                .toUriString();
    }

    private Map<String, Object> section(String text) {
        return Map.of(
                "type",
                "section",
                "text",
                Map.of("type", "plain_text", "text", text.substring(0, Math.min(text.length(), 2800)))
        );
    }
}
