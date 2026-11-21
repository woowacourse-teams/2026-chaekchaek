package com.chaekchaek.incident.evidence;

import com.chaekchaek.incident.alert.dto.IncidentSnapshot;
import com.chaekchaek.incident.evidence.client.LokiClient;
import com.chaekchaek.incident.evidence.client.PrometheusClient;
import com.chaekchaek.incident.evidence.dto.EvidenceBundle;
import com.chaekchaek.incident.evidence.dto.EvidenceItem;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class EvidenceCollector {
    private final LokiClient loki;
    private final PrometheusClient prometheus;
    private final SafeLogExtractor extractor;
    private final JsonMapper mapper;

    public EvidenceBundle collect(IncidentSnapshot task) {
        Instant to = task.startsAt();
        var items = new ArrayList<EvidenceItem>();
        var missing = new ArrayList<String>();
        collectLogs(task, items, missing);
        collectAccessCount(task, 0, items, missing);
        collectAccessCount(task, 300, items, missing);
        for (var metric : metricQueries(task.environment()).entrySet()) {
            collectScalar("prometheus", metric.getKey(), () -> prometheus.query(metric.getValue(), to), Map.of(), items, missing);
        }
        return new EvidenceBundle(
                to.minusSeconds(600),
                to,
                items,
                missing
        );
    }

    private void collectAccessCount(
            IncidentSnapshot task,
            int offsetSeconds,
            List<EvidenceItem> items,
            List<String> missing
    ) {
        Instant to = task.startsAt().minusSeconds(offsetSeconds);
        String description = "발생 전 " + offsetSeconds / 60 + "~" + (offsetSeconds / 60 + 5) + "분 5xx 접근 로그 건수";
        collectScalar(
                "loki",
                description,
                () -> loki.errorCount(task.environment(), to),
                Map.of("from", to.minusSeconds(300).toString(), "to", to.toString()),
                items,
                missing
        );
    }

    private void collectLogs(IncidentSnapshot task, List<EvidenceItem> items, List<String> missing) {
        try {
            JsonNode streams = loki.logs(task.environment(), task.startsAt().minusSeconds(300), task.startsAt());
            Map<Map<String, String>, Integer> groups = new LinkedHashMap<>();
            for (JsonNode stream : streams) {
                addLogGroup(stream, groups);
            }
            groups.entrySet()
                    .stream()
                    .limit(10)
                    .forEach(entry -> addSample(items, entry.getKey(), entry.getValue()));
            missing.add("로그는 최근 최대 200건 중 10개 유형의 표본이며 전체 오류 유형을 보장하지 않습니다");
            if (groups.isEmpty()) {
                missing.add("해당 구간의 구조화된 오류 로그 표본이 없습니다");
            }
        } catch (RuntimeException exception) {
            missing.add("Loki 로그 조회에 실패했습니다");
        }
    }

    private void addLogGroup(JsonNode stream, Map<Map<String, String>, Integer> groups) {
        for (JsonNode entry : stream.path("values")) {
            JsonNode log = mapper.readTree(entry.path(1).asText());
            if ("error".equals(log.path("type").asText())) {
                Map<String, String> safe = extractor.extract(log);
                groups.merge(safe, 1, Integer::sum);
            }
        }
    }

    private void addSample(List<EvidenceItem> items, Map<String, String> fields, int count) {
        var values = new LinkedHashMap<>(fields);
        values.put("sampleCount", String.valueOf(count));
        items.add(new EvidenceItem(
                "E" + (items.size() + 1),
                "loki",
                "대표 오류 유형, 표본 내 발생 건수",
                values
        ));
    }

    private void collectScalar(
            String source,
            String description,
            Supplier<JsonNode> query,
            Map<String, String> interval,
            List<EvidenceItem> items,
            List<String> missing
    ) {
        try {
            JsonNode result = query.get();
            String value = result.path(0).path("value").path(1).asText("");
            if (!value.matches("-?[0-9]+(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?")) {
                missing.add(description + ": 유효한 샘플이 없습니다");
                return;
            }
            var values = new LinkedHashMap<>(interval);
            values.put("value", value);
            items.add(new EvidenceItem(
                    "E" + (items.size() + 1),
                    source,
                    description,
                    values
            ));
        } catch (RuntimeException exception) {
            missing.add(description + ": 조회에 실패했습니다");
        }
    }

    private Map<String, String> metricQueries(String environment) {
        String app = "{env=\"" + environment + "\",job=\"chaekchaek-backend\"}";
        String host = "{env=\"" + environment + "\",job=\"integrations/unix\"}";
        var queries = new LinkedHashMap<String, String>();
        queries.put("백엔드 수집 성공 여부", "min(up" + app + ")");
        queries.put("백엔드 마지막 실제 샘플 경과 초", "max(time() - timestamp(up" + app + "))");
        queries.put("호스트 가용 메모리 비율", "min(node_memory_MemAvailable_bytes" + host + " / node_memory_MemTotal_bytes" + host + ")");
        queries.put("호스트 마지막 실제 샘플 경과 초", "max(time() - timestamp(node_memory_MemAvailable_bytes" + host + "))");
        queries.put("DB 연결 대기 수", "sum(hikaricp_connections_pending{env=\"" + environment + "\",job=\"chaekchaek-backend\"})");
        return queries;
    }
}
