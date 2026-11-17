package com.chaekchaek.incident;

import com.chaekchaek.incident.alert.dto.IncidentSnapshot;
import com.chaekchaek.incident.config.SlackProperties;
import com.chaekchaek.incident.question.dto.QuestionRequest;
import com.chaekchaek.incident.question.dto.QuestionTask;
import com.chaekchaek.incident.analysis.dto.AnalysisReport;
import com.chaekchaek.incident.analysis.dto.AnalysisResult;
import com.chaekchaek.incident.config.IncidentProperties;
import com.chaekchaek.incident.evidence.dto.EvidenceBundle;
import com.chaekchaek.incident.evidence.dto.EvidenceItem;
import com.chaekchaek.incident.evidence.QueryName;
import com.chaekchaek.incident.evidence.dto.QueryBatch;
import com.chaekchaek.incident.evidence.dto.QueryExecution;
import com.chaekchaek.incident.evidence.dto.QueryStatus;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class Fixtures {
    static final Instant NOW = Instant.parse("2026-11-17T10:00:00Z");
    static final String TOKEN = "test-webhook-token-at-least-32-characters";

    static IncidentProperties properties(String baseUrl) {
        return new IncidentProperties(
                TOKEN,
                1,
                Set.of("dev"),
                URI.create(baseUrl),
                URI.create(baseUrl),
                URI.create("https://grafana.example.com"),
                URI.create(baseUrl + "v1/responses"),
                "test-api-key",
                "test-model",
                true,
                2
        );
    }

    static IncidentSnapshot incident() {
        return new IncidentSnapshot(
                "a".repeat(64),
                "dev",
                "Backend5xx",
                "api-errors",
                NOW.minusSeconds(120),
                true
        );
    }

    static QuestionRequest request(String id) {
        return new QuestionRequest(
                id,
                "dev",
                "CDEV",
                "1794910000.000001",
                "어디를 먼저 확인해야 하나요?"
        );
    }

    static QuestionTask task(int attempts, String result) {
        return new QuestionTask(
                "EvQUESTION",
                "dev",
                "CDEV",
                "1794910000.000001",
                "어디를 먼저 확인해야 하나요?",
                NOW,
                NOW.plusSeconds(90),
                attempts,
                result
        );
    }

    static SlackProperties slackProperties(String baseUrl) {
        return new SlackProperties(
                true,
                "xapp-test",
                "xoxb-test",
                "CDEV",
                "",
                "BALERT",
                "",
                URI.create(baseUrl)
        );
    }

    static EvidenceBundle evidence() {
        return new EvidenceBundle(
                NOW.minusSeconds(600),
                NOW,
                List.of(new EvidenceItem(
                        "E1",
                        "loki",
                        "최근 5분 오류 수",
                        Map.of("value", "5")
                )),
                List.of("표본만 확인했습니다")
        );
    }

    static AnalysisReport report(String evidenceId) {
        return new AnalysisReport(
                "오류가 증가했습니다",
                "PARTIAL",
                List.of(new AnalysisReport.Finding(
                        "최근 5분에 오류가 발생했습니다",
                        List.of(evidenceId)
                )),
                List.of(new AnalysisReport.Hypothesis(
                        "외부 도서 API 장애 가능성",
                        List.of(evidenceId),
                        "외부 API 상태를 확인하세요",
                        "upstreamStatus를 확인해야 합니다"
                )),
                List.of("외부 응답 상태가 없습니다")
        );
    }

    static QueryBatch queryBatch() {
        var additional = new EvidenceItem(
                "E2",
                "prometheus",
                "DB 연결 대기 최댓값",
                Map.of("row1", "pool=HikariPool-1, value=3", "unit", "connections")
        );
        return new QueryBatch(
                new EvidenceBundle(
                        incident().startsAt().minusSeconds(900),
                        incident().startsAt(),
                        List.of(evidence().items().getFirst(), additional),
                        evidence().missingEvidence()
                ),
                List.of(new QueryExecution(
                        "db_pool_wait_peak",
                        "prometheus",
                        "DB 연결 대기 최댓값",
                        QueryName.DB_POOL_WAIT_PEAK.expression("dev"),
                        incident().startsAt().minusSeconds(900),
                        incident().startsAt(),
                        QueryStatus.SUCCESS,
                        false,
                        "E2",
                        List.of()
                ))
        );
    }

    static AnalysisResult result() {
        return new AnalysisResult(
                report("E1"),
                "test-model",
                "v1",
                100,
                50,
                evidence().missingEvidence(),
                evidence().items()
        );
    }
}
