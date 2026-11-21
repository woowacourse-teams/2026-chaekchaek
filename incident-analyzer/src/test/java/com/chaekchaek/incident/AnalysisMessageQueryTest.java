package com.chaekchaek.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaekchaek.incident.analysis.dto.AnalysisReport;
import com.chaekchaek.incident.analysis.dto.AnalysisResult;
import com.chaekchaek.incident.config.IncidentProperties;
import com.chaekchaek.incident.evidence.QueryName;
import com.chaekchaek.incident.evidence.dto.EvidenceItem;
import com.chaekchaek.incident.evidence.dto.QueryExecution;
import com.chaekchaek.incident.evidence.dto.QueryStatus;
import com.chaekchaek.incident.notification.AnalysisMessage;
import com.chaekchaek.incident.question.dto.PreparedReply;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class AnalysisMessageQueryTest {
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    @DisplayName("추가 조회 버튼은 외부 Grafana 경로와 발생 시각의 instant 쿼리를 재현한다")
    void should_ReproduceInstantQueries_When_RenderingAdditionalEvidence() {
        // given
        AnalysisMessage messages = new AnalysisMessage(properties(), mapper);
        List<QueryExecution> queries = executions();
        // when
        var reply = messages.reply(Fixtures.incident(), Fixtures.result(), queries);
        // then
        JsonNode blocks = mapper.valueToTree(reply.message()).path("blocks");
        JsonNode buttons = blocks.get(blocks.size() - 1).path("elements");
        assertThat(buttons.size()).isEqualTo(4);
        for (int index = 0; index < queries.size(); index++) {
            QueryExecution execution = queries.get(index);
            String url = buttons.get(index).path("url").asText();
            assertThat(url).startsWith("https://api.chaekchaek.com/grafana/explore?");
            JsonNode pane = pane(url);
            JsonNode query = pane.path("queries").get(0);
            assertThat(pane.path("datasource").asText()).isEqualTo(execution.source());
            assertThat(query.path("datasource").path("uid").asText()).isEqualTo(execution.source());
            assertThat(query.path("expr").asText()).isEqualTo(execution.expression());
            assertThat(pane.path("range").path("from").asText()).isEqualTo(String.valueOf(execution.from().toEpochMilli()));
            assertThat(pane.path("range").path("to").asText()).isEqualTo(String.valueOf(execution.to().toEpochMilli()));
            assertInstantMode(query, execution.source());
        }
        var actions = blocks.valueStream()
                .filter(block -> "actions".equals(block.path("type").asText()))
                .toList();
        JsonNode baseQuery = pane(actions.getFirst().path("elements").get(0).path("url").asText())
                .path("queries").get(0);
        assertThat(baseQuery.path("queryType").asText()).isEqualTo("range");
        assertThat(blocks.toString()).contains("SUCCESS", "NO_DATA", "FAILED", "SKIPPED_DEADLINE", "상위 5개만 표시합니다");
    }

    @Test
    @DisplayName("요청 상세의 재현 링크는 저장된 식과 구간을 range로 연다")
    void should_ReproduceRangeQuery_When_RenderingRequestDetails() {
        // given
        var detail = execution(QueryName.ERROR_REQUEST_DETAILS, QueryStatus.SUCCESS);
        // when
        var reply = new AnalysisMessage(properties(), mapper).reply(Fixtures.incident(), Fixtures.result(), List.of(detail));
        // then
        JsonNode blocks = mapper.valueToTree(reply.message()).path("blocks");
        JsonNode pane = pane(blocks.get(blocks.size() - 1).path("elements").get(0).path("url").asText());
        assertThat(pane.path("queries").get(0).path("queryType").asText()).isEqualTo("range");
        assertThat(pane.path("queries").get(0).path("expr").asText()).isEqualTo(detail.expression());
        assertThat(pane.path("range").path("from").asText()).isEqualTo(String.valueOf(detail.from().toEpochMilli()));
        assertThat(pane.path("range").path("to").asText()).isEqualTo(String.valueOf(detail.to().toEpochMilli()));
    }

    @Test
    @DisplayName("최대 근거와 보고 항목을 표시해도 Slack 블록 한도를 넘지 않는다")
    void should_RemainWithinSlackLimits_When_AllResultListsAreFull() {
        // given
        var facts = Collections.nCopies(10, Fixtures.report("E1").observedFacts().getFirst());
        var hypotheses = Collections.nCopies(3, Fixtures.report("E1").hypotheses().getFirst());
        var evidence = IntStream.rangeClosed(1, 21)
                .mapToObj(index -> new EvidenceItem(
                        "E" + index,
                        "prometheus",
                        "다중 결과",
                        Map.of("row1", "pool=HikariPool-1, value=0")
                ))
                .toList();
        var result = new AnalysisResult(
                new AnalysisReport(
                        "수집된 관측 자료를 확인했습니다",
                        "PARTIAL",
                        facts,
                        hypotheses,
                        Collections.nCopies(10, "자료 제한".repeat(50))
                ),
                "test-model",
                "v3-query-tools",
                200,
                100,
                List.of("빈 조회는 정상 0을 의미하지 않습니다"),
                evidence
        );
        // when
        var reply = new AnalysisMessage(properties(), mapper).reply(Fixtures.incident(), result, executions());
        // then
        JsonNode blocks = mapper.valueToTree(reply.message()).path("blocks");
        assertThat(blocks.size()).isEqualTo(42);
        assertThat(reply.inputTokens()).isEqualTo(200);
        assertThat(reply.outputTokens()).isEqualTo(100);
        for (JsonNode block : blocks) {
            assertThat(block.path("text").path("text").asText().length()).isLessThanOrEqualTo(2800);
        }
        assertThat(mapper.readValue(mapper.writeValueAsString(reply), PreparedReply.class)).isEqualTo(reply);
    }

    @Test
    @DisplayName("도구 도입 전에 저장한 준비 답변 형식을 그대로 읽는다")
    void should_ReadPreviousReply_When_ResultHasNoQueryMetadata() {
        // given
        String saved = """
                {"message":{"text":"기존 답변","blocks":[]},"contextSummary":"기존 문맥","model":"test-model","inputTokens":100,"outputTokens":50}
                """;
        // when
        PreparedReply reply = mapper.readValue(saved, PreparedReply.class);
        // then
        assertThat(reply.message().get("text")).isEqualTo("기존 답변");
        assertThat(reply.contextSummary()).isEqualTo("기존 문맥");
        assertThat(reply.inputTokens()).isEqualTo(100);
    }

    private void assertInstantMode(JsonNode query, String source) {
        if ("prometheus".equals(source)) {
            assertThat(query.path("instant").asBoolean()).isTrue();
            assertThat(query.path("range").asBoolean()).isFalse();
            return;
        }
        assertThat(query.path("queryType").asText()).isEqualTo("instant");
    }

    private JsonNode pane(String url) {
        String encoded = UriComponentsBuilder.fromUriString(url).build().getQueryParams().getFirst("panes");
        return mapper.readTree(URLDecoder.decode(encoded, StandardCharsets.UTF_8)).path("incident");
    }

    private List<QueryExecution> executions() {
        return List.of(
                execution(QueryName.DB_POOL_WAIT_PEAK, QueryStatus.SUCCESS),
                execution(QueryName.HTTP_LATENCY_P95, QueryStatus.NO_DATA),
                execution(QueryName.ERROR_TYPE_COUNTS, QueryStatus.FAILED),
                execution(QueryName.EXTERNAL_API_FAILURE_COUNTS, QueryStatus.SKIPPED_DEADLINE)
        );
    }

    private QueryExecution execution(QueryName query, QueryStatus status) {
        return new QueryExecution(
                query.getId(),
                query.getSource(),
                query.getDescription(),
                query.expression("dev"),
                Fixtures.incident().startsAt().minusSeconds(900),
                Fixtures.incident().startsAt(),
                status,
                true,
                "E18",
                List.of("상위 5개만 표시합니다")
        );
    }

    private IncidentProperties properties() {
        IncidentProperties base = Fixtures.properties("http://localhost/");
        return new IncidentProperties(
                base.webhookToken(),
                base.orgId(),
                base.enabledEnvironments(),
                base.lokiUrl(),
                base.prometheusUrl(),
                URI.create("https://api.chaekchaek.com/grafana"),
                base.llmUrl(),
                base.llmApiKey(),
                base.llmModel(),
                base.llmEnabled(),
                base.dailyCallLimit()
        );
    }
}
