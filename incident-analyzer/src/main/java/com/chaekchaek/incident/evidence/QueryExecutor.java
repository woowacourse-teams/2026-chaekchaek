package com.chaekchaek.incident.evidence;

import com.chaekchaek.incident.alert.dto.IncidentSnapshot;
import com.chaekchaek.incident.evidence.client.LokiClient;
import com.chaekchaek.incident.evidence.client.PrometheusClient;
import com.chaekchaek.incident.evidence.dto.EvidenceBundle;
import com.chaekchaek.incident.evidence.dto.EvidenceItem;
import com.chaekchaek.incident.evidence.dto.QueryBatch;
import com.chaekchaek.incident.evidence.dto.QueryExecution;
import com.chaekchaek.incident.evidence.dto.QuerySelection;
import com.chaekchaek.incident.evidence.dto.QueryStatus;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
public class QueryExecutor {
    private static final int MAX_ROWS = 5;
    private final PrometheusClient prometheus;
    private final LokiClient loki;
    private final Clock clock;
    private final MeterRegistry meters;

    public QueryBatch execute(
            QuerySelection selection,
            IncidentSnapshot incident,
            EvidenceBundle base,
            Instant deadline
    ) {
        var items = new ArrayList<>(base.items());
        var missing = new ArrayList<>(base.missingEvidence());
        var executions = new ArrayList<QueryExecution>();
        for (QueryName name : selection.queries()) {
            QueryExecution execution = collect(name, incident, deadline, items);
            executions.add(execution);
            execution.limitations().forEach(note -> missing.add(name.getDescription() + ": " + note));
            meters.counter("incident_query", "query", name.getId(), "outcome", execution.status().name()).increment();
        }
        return new QueryBatch(
                new EvidenceBundle(
                        incident.startsAt().minusSeconds(900),
                        incident.startsAt(),
                        items,
                        missing
                ),
                executions
        );
    }

    private QueryExecution collect(QueryName name, IncidentSnapshot incident, Instant deadline, List<EvidenceItem> items) {
        String expression = name.expression(incident.environment());
        var notes = new ArrayList<String>();
        if (clock.instant().plusSeconds(40).isAfter(deadline)) {
            notes.add("최종 답변 시간을 확보하기 위해 추가 조회를 생략했습니다");
            return execution(name, incident, expression, QueryStatus.SKIPPED_DEADLINE, false, "", notes);
        }
        try {
            JsonNode result = query(name, expression, incident.startsAt());
            boolean truncated = result.size() > MAX_ROWS;
            Map<String, String> rows = rows(name, result, notes);
            if (truncated) {
                notes.add("상위 5개 결과만 포함했습니다");
            }
            if (rows.isEmpty()) {
                notes.add("유효한 관측 자료가 없습니다. 정상값 0을 의미하지 않습니다");
                return execution(name, incident, expression, QueryStatus.NO_DATA, truncated, "", notes);
            }
            String id = "E" + (items.size() + 1);
            rows.put("unit", name.getUnit());
            rows.put("from", incident.startsAt().minusSeconds(900).toString());
            rows.put("to", incident.startsAt().toString());
            items.add(new EvidenceItem(
                    id,
                    name.getSource(),
                    name.getDescription(),
                    rows
            ));
            return execution(name, incident, expression, QueryStatus.SUCCESS, truncated, id, notes);
        } catch (RuntimeException exception) {
            notes.add("관측 API 조회에 실패했습니다");
            return execution(name, incident, expression, QueryStatus.FAILED, false, "", notes);
        }
    }

    private JsonNode query(QueryName name, String expression, Instant at) {
        if ("loki".equals(name.getSource())) {
            return loki.query(expression, at);
        }
        return prometheus.query(expression, at);
    }

    private Map<String, String> rows(QueryName name, JsonNode result, List<String> notes) {
        Map<String, String> rows = new LinkedHashMap<>();
        if (!result.isArray()) {
            return rows;
        }
        for (int index = 0; index < Math.min(MAX_ROWS, result.size()); index++) {
            String row = row(name, result.path(index));
            if (row == null) {
                notes.add("형식이나 길이 제한을 충족하지 않는 결과를 제외했습니다");
                continue;
            }
            rows.put("row" + (index + 1), row);
        }
        return rows;
    }

    private String row(QueryName name, JsonNode sample) {
        String value = sample.path("value").path(1).asText("");
        if (!finiteNonnegative(value)) {
            return null;
        }
        var parts = new ArrayList<String>();
        for (String key : name.getLabels()) {
            String label = sample.path("metric").path(key).asText("");
            if (label.isEmpty() && !Set.of("exception", "upstreamStatus").contains(key)) {
                return null;
            }
            if (!label.isEmpty() && !validLabel(key, label)) {
                return null;
            }
            if (!label.isEmpty()) {
                parts.add(key + "=" + label);
            }
        }
        parts.add("value=" + value);
        String row = String.join(", ", parts);
        if (row.length() > 240) {
            return null;
        }
        return row;
    }

    private boolean finiteNonnegative(String value) {
        if (value.length() > 64 || !value.matches("[0-9]+(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?")) {
            return false;
        }
        return Double.isFinite(Double.parseDouble(value));
    }

    private boolean validLabel(String key, String value) {
        if ("upstreamStatus".equals(key)) {
            return value.matches("[1-5][0-9]{2}");
        }
        return value.matches("[A-Za-z0-9_.$-]{1,100}");
    }

    private QueryExecution execution(
            QueryName name,
            IncidentSnapshot incident,
            String expression,
            QueryStatus status,
            boolean truncated,
            String evidenceId,
            List<String> notes
    ) {
        return new QueryExecution(
                name.getId(),
                name.getSource(),
                name.getDescription(),
                expression,
                incident.startsAt().minusSeconds(900),
                incident.startsAt(),
                status,
                truncated,
                evidenceId,
                notes
        );
    }
}
