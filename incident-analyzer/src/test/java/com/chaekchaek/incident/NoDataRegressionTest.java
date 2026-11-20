package com.chaekchaek.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chaekchaek.incident.alert.dto.IncidentSnapshot;
import com.chaekchaek.incident.analysis.AnalysisValidator;
import com.chaekchaek.incident.analysis.IncidentInvestigator;
import com.chaekchaek.incident.analysis.client.LlmClient;
import com.chaekchaek.incident.analysis.dto.InvestigationResult;
import com.chaekchaek.incident.analysis.service.ModelBudgetService;
import com.chaekchaek.incident.common.JsonHttpClient;
import com.chaekchaek.incident.evidence.EvidenceCollector;
import com.chaekchaek.incident.evidence.QueryExecutor;
import com.chaekchaek.incident.evidence.QueryName;
import com.chaekchaek.incident.evidence.SafeLogExtractor;
import com.chaekchaek.incident.evidence.client.LokiClient;
import com.chaekchaek.incident.evidence.client.PrometheusClient;
import com.chaekchaek.incident.evidence.dto.QueryStatus;
import com.chaekchaek.incident.notification.AnalysisMessage;
import com.chaekchaek.incident.question.dto.PreparedReply;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class NoDataRegressionTest {
    private static final Instant AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final String CHECKS = """
            {"nextChecks":[{"check":"같은 구간 histogram 샘플과 scrape 수집 상태, 요청량, HTTP status code 429와 node 2를 확인하세요",
            "expectedObservation":"표본 부재와 실제 요청 부재를 구분하고, 공급자 제한 정책과 요청량을 대조하세요"},
            {"check":"DB 연결 대기의 실제 표본과 pool별 값을 확인하세요","expectedObservation":"같은 pool의 대기 증가와 시간대를 대조하세요"},
            {"check":"오류 유형의 전체 집계와 로그 표본을 대조하세요","expectedObservation":"전체 집계와 제한된 표본의 차이를 확인하세요"}]}
            """;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private final PrometheusClient prometheus = mock(PrometheusClient.class);
    private final LokiClient loki = mock(LokiClient.class);
    private final ModelBudgetService budget = mock(ModelBudgetService.class);
    private MockWebServer server;
    private JsonNode cases;
    private JsonNode rubric;
    private LlmClient llm;
    private String finalOutput;
    private MockResponse finalResponse;
    private boolean longDbRows;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        cases = resource("cases.json").path("cases");
        rubric = resource("rubric.json");
        llm = new LlmClient(
                Fixtures.properties(server.url("/").toString()),
                new JsonHttpClient(mapper),
                mapper,
                new AnalysisValidator(),
                meters
        );
        when(budget.reserve()).thenReturn(true);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
        meters.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"C2", "C4", "C6"})
    @DisplayName("고정된 빈 조회 사례는 서버 관측과 조회 상태를 보존하고 모델 점검 제안만 표시한다")
    void should_RenderCanonicalEvidenceAndChecks_When_OneSelectedQueryHasNoData(String id) throws Exception {
        // given
        enqueueOutput(CHECKS);
        // when
        InvestigationResult result = investigate(id);
        var reply = new AnalysisMessage(Fixtures.properties("http://localhost/"), mapper)
                .reply(incident(id), result.analysis(), result.queries());
        // then
        assertThat(result.failureCode()).isNull();
        assertThat(result.analysis().report().assessment()).isEqualTo("PARTIAL");
        assertThat(result.analysis().report().observedFacts()).isEmpty();
        assertThat(result.analysis().report().hypotheses()).isEmpty();
        assertThat(result.analysis().inputTokens()).isEqualTo(200);
        assertThat(result.analysis().outputTokens()).isEqualTo(100);
        String displayed = mapper.writeValueAsString(reply.message());
        assertThat(displayed).contains("다음 점검 제안", "histogram", "scrape", "요청량", "정상값 0을 의미하지 않습니다");
        assertThat(displayed).doesNotContain("원인 후보:");
        JsonNode checks = mapper.readTree(CHECKS).path("nextChecks");
        for (JsonNode check : checks) {
            assertThat(reply.contextSummary()).contains(check.path("check").asText(), check.path("expectedObservation").asText());
        }
        assertThat(reply.contextSummary()).contains("http_latency_p95", "NO_DATA", AT.minusSeconds(900).toString(), AT.toString(), "prometheus");
        if ("C2".equals(id)) {
            assertThat(displayed).contains("status=502", "upstreamStatus=429", "sampleCount=2", "value=12");
            assertThat(reply.contextSummary()).contains("status=502", "upstreamStatus=429", "sampleCount=2", "value=12", "unit=errors");
        }
        if ("C4".equals(id)) {
            assertThat(displayed).contains("value=0.42", "value=8", "백엔드 수집 성공 여부: 유효한 샘플이 없습니다");
            assertThat(displayed).doesNotContain("백엔드 수집 성공 여부=0");
            assertThat(reply.contextSummary()).contains("value=0.42", "value=8");
        }
        if ("C6".equals(id)) {
            assertThat(result.queries()).extracting(query -> query.status()).containsExactly(QueryStatus.SUCCESS, QueryStatus.NO_DATA);
            String zeroId = result.queries().getFirst().evidenceId();
            assertThat(result.analysis().evidence()).anySatisfy(item -> {
                assertThat(item.id()).isEqualTo(zeroId);
                assertThat(item.description()).isEqualTo(QueryName.DB_POOL_WAIT_PEAK.getDescription());
                assertThat(item.values()).containsEntry("row1", "pool=HikariPool-1, value=0");
            });
            assertThat(reply.contextSummary()).contains("db_pool_wait_peak", "SUCCESS", "value=0", "unit=connections");
        }
        assertQueryLinks(reply, result);
        assertThat(mapper.readValue(mapper.writeValueAsString(reply), PreparedReply.class)).isEqualTo(reply);
        request();
        JsonNode finalRequest = request();
        JsonNode schema = finalRequest.path("text").path("format").path("schema");
        assertThat(schema.path("properties").propertyNames()).containsExactly("nextChecks");
        assertThat(schema.path("additionalProperties").asBoolean()).isFalse();
        assertThat(schema.toString()).doesNotContain("evidenceIds", "observedFacts", "hypotheses");
        verify(budget, times(2)).reserve();
    }

    @Test
    @DisplayName("긴 성공 근거에서도 첫 점검과 판단 기준의 원문 및 완전한 관측 행을 후속 문맥에 남긴다")
    void should_PreserveFirstCheckAndObservedRow_When_LongEvidenceExceedsContextLimit() throws Exception {
        // given
        longDbRows = true;
        String check = longCheck("같은 구간의 histogram과 scrape 상태를 확인하세요. ", " 확인 전에는 장애 원인으로 확정하지 마세요");
        String expected = longCheck("표본 부재와 실제 요청 부재를 구분하세요. ", " 자료 부재만으로 정상값 0이라고 판단하지 마세요");
        enqueueOutput(mapper.writeValueAsString(Map.of("nextChecks", List.of(Map.of(
                "check", check,
                "expectedObservation", expected
        )))));
        // when
        InvestigationResult result = investigate("C6");
        PreparedReply reply = new AnalysisMessage(Fixtures.properties("http://localhost/"), mapper)
                .reply(incident("C6"), result.analysis(), result.queries());
        // then
        assertThat(result.failureCode()).isNull();
        assertThat(check).hasSize(500);
        assertThat(expected).hasSize(500);
        assertThat(reply.contextSummary()).hasSizeLessThanOrEqualTo(1800);
        assertThat(reply.contextSummary()).contains(
                "db_pool_wait_peak [prometheus]: SUCCESS | 근거: E3",
                "http_latency_p95 [prometheus]: NO_DATA",
                AT.minusSeconds(900).toString(),
                AT.toString(),
                "정상값 0을 의미하지 않습니다",
                "다음 점검 제안: " + check + "\n판단 기준: " + expected,
                "E3 [prometheus] DB 연결 대기 구간 최대값 | unit=connections\nrow1=pool="
                        + "p".repeat(99) + "1, value=0",
                "생략"
        );
        JsonNode blocks = mapper.valueToTree(reply.message()).path("blocks");
        String displayed = blocks.valueStream()
                .map(block -> block.path("text").path("text").asText())
                .reduce("", (left, right) -> left + "\n" + right);
        assertThat(displayed).contains(check, expected, "row5=pool=" + "p".repeat(99) + "5, value=0");
    }

    private String longCheck(String prefix, String conclusion) {
        String body = prefix + "수집 시각과 실제 표본, 같은 구간의 요청량을 대조하고 결과를 기록하세요. ".repeat(20);
        return body.substring(0, 500 - conclusion.length()) + conclusion;
    }

    @ParameterizedTest
    @ValueSource(strings = {"C2", "C4"})
    @DisplayName("기존 실제 답변의 무관한 인용은 정상 모델 진입점에서 거절하고 저장하지 않는다")
    void should_RejectPreviousMisattributedReports_When_NoDataContractIsRequired(String id) throws Exception {
        // given
        JsonNode previous = resource("previous-reports.json").path(id);
        enqueueOutput(previous.toString());
        // when
        InvestigationResult result = investigate(id);
        PreparedReply reply = new AnalysisMessage(Fixtures.properties("http://localhost/"), mapper)
                .reply(incident(id), result.analysis(), result.queries());
        // then
        assertThat(result.failureCode()).isEqualTo("INVALID_MODEL_OUTPUT");
        assertThat(result.analysis().evidence()).isNotEmpty();
        assertThat(result.queries()).anyMatch(query -> query.status() == QueryStatus.NO_DATA);
        assertThat(mapper.writeValueAsString(reply)).doesNotContain(previous.path("summary").asText());
        assertThat(meters.get("incident_llm_tokens").tag("direction", "input").counter().count()).isEqualTo(200);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"nextChecks\":[{\"check\":\"_E1_에 p95 자료가 없습니다\",\"expectedObservation\":\"다시 확인\"}]}",
            "{\"nextChecks\":[{\"check\":\"조회 확인\",\"expectedObservation\":\"__E1__에 p95 자료가 없습니다\"}]}",
            "{\"nextChecks\":[{\"check\":\"p95를 확인할 값이 반환되지 않았습니다 E1\",\"expectedObservation\":\"다시 확인\"}]}",
            "{\"nextChecks\":[{\"check\":\"There was no valid p95 sample e1\",\"expectedObservation\":\"check again\"}]}",
            "{\"nextChecks\":[{\"check\":\"다시 확인\",\"expectedObservation\":\"E 1의 값이 없음\"}]}",
            "{\"nextChecks\":[{\"check\":\"근거E\u200B1의 값이 없음\",\"expectedObservation\":\"다시 확인\"}]}",
            "{\"nextChecks\":[{\"check\":\"Ｅ１의 값이 없음\",\"expectedObservation\":\"다시 확인\"}]}",
            "{\"nextChecks\":[{\"check\":\"조회 확인\",\"expectedObservation\":\"결과 확인\",\"evidenceIds\":[\"E1\"]}]}",
            "{\"nextChecks\":[{\"check\":\"조회 확인\",\"expectedObservation\":\"결과 확인\"}],\"summary\":\"p95 없음 E1\"}",
            "{\"nextChecks\":[{\"check\":\"조회 확인\",\"expectedObservation\":\"결과 확인\"}],\"observedFacts\":[]}",
            "{\"nextChecks\":[{\"check\":\"조회 확인\",\"expectedObservation\":\"결과 확인\"}],\"hypotheses\":[]}",
            "{\"nextChecks\":[{\"check\":\"조회 확인\",\"expectedObservation\":\"결과 확인\"}],\"missingEvidence\":[\"자료 없음 E1\"]}",
            "{\"nextChecks\":[]}",
            "{\"nextChecks\":null}",
            "{\"nextChecks\":[{\"check\":42,\"expectedObservation\":\"결과 확인\"}]}"
    })
    @DisplayName("다음 점검 문자열의 직접 근거 인용과 추가 보고 필드는 저장 전에 거절한다")
    void should_RejectCitationAndHiddenFields_When_FinalOutputBypassesSchema(String output) throws Exception {
        // given
        enqueueOutput(output);
        // when
        InvestigationResult result = investigate("C6");
        PreparedReply reply = new AnalysisMessage(Fixtures.properties("http://localhost/"), mapper)
                .reply(incident("C6"), result.analysis(), result.queries());
        // then
        assertThat(result.failureCode()).isEqualTo("INVALID_MODEL_OUTPUT");
        assertThat(reply.contextSummary()).doesNotContain("There was no valid", "Ｅ１", "E 1", "_E1_", "__E1__");
        assertThat(result.analysis().report().hypotheses()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"HTTP,HTTP_429,100", "JSON,ANALYSIS_PROCESSING_ERROR,100", "TOOL,UNEXPECTED_TOOL_CALL,200"})
    @DisplayName("빈 조회 뒤 최종 HTTP와 JSON과 도구 호출 오류도 관측과 수신 사용량을 보존한다")
    void should_PreserveEvidenceAndUsage_When_NoDataFinalResponseFails(
            String fault,
            String code,
            long receivedTokens
    ) throws Exception {
        // given
        finalResponse = new MockResponse().setBody("private-model-output sk-secret");
        if ("HTTP".equals(fault)) {
            finalResponse.setResponseCode(429);
        }
        if ("TOOL".equals(fault)) {
            finalResponse.setBody(mapper.writeValueAsString(Map.of(
                    "status", "completed",
                    "usage", Map.of("input_tokens", 100, "output_tokens", 50),
                    "output", List.of(Map.of("type", "function_call"))
            )));
        }
        // when
        InvestigationResult result = investigate("C6");
        PreparedReply reply = new AnalysisMessage(Fixtures.properties("http://localhost/"), mapper)
                .reply(incident("C6"), result.analysis(), result.queries());
        // then
        assertThat(result.failureCode()).isEqualTo(code);
        assertThat(result.queries()).extracting(query -> query.status()).containsExactly(QueryStatus.SUCCESS, QueryStatus.NO_DATA);
        assertThat(result.analysis().evidence()).anySatisfy(item ->
                assertThat(item.values()).containsEntry("row1", "pool=HikariPool-1, value=0"));
        assertThat(reply.contextSummary()).contains("value=0", "NO_DATA", "http_latency_p95");
        assertThat(mapper.writeValueAsString(reply)).doesNotContain("private-model-output", "sk-secret");
        assertThat(reply.inputTokens()).isEqualTo(100);
        assertThat(reply.outputTokens()).isEqualTo(50);
        assertThat(meters.get("incident_llm_tokens").tag("direction", "input").counter().count()).isEqualTo(receivedTokens);
        verify(budget, times(2)).reserve();
    }

    private InvestigationResult investigate(String id) throws Exception {
        JsonNode fixture = fixture(id);
        when(loki.logs(anyString(), any(), any())).thenReturn(logs(fixture));
        when(loki.errorCount(anyString(), any())).thenReturn(samples(fixture.path("base").path("recent5xx")), samples(fixture.path("base").path("previous5xx")));
        when(prometheus.query(anyString(), any())).thenAnswer(invocation -> metric(fixture, invocation.getArgument(0)));
        when(loki.query(anyString(), any())).thenAnswer(invocation -> queryRows(fixture, invocation.getArgument(0)));
        var collector = new EvidenceCollector(loki, prometheus, new SafeLogExtractor(), mapper);
        Clock clock = Clock.fixed(AT, ZoneOffset.UTC);
        var investigator = new IncidentInvestigator(llm, new QueryExecutor(prometheus, loki, clock, meters), budget, clock);
        enqueueSelection(id);
        enqueueFinal();
        return investigator.investigate(collector.collect(incident(id)), incident(id), fixture.path("question").asText(), List.of(), AT.plusSeconds(90));
    }

    private JsonNode metric(JsonNode fixture, String expression) {
        String key = null;
        if (expression.startsWith("min(up")) {
            key = "backendUp";
        }
        if (expression.startsWith("max(time() - timestamp(up")) {
            key = "backendAgeSeconds";
        }
        if (expression.startsWith("min(node_memory_")) {
            key = "hostMemoryRatio";
        }
        if (expression.startsWith("max(time() - timestamp(node_memory_")) {
            key = "hostAgeSeconds";
        }
        if (expression.startsWith("sum(hikaricp_")) {
            key = "dbPending";
        }
        if (key != null) {
            return samples(fixture.path("base").path(key));
        }
        return queryRows(fixture, expression);
    }

    private JsonNode samples(JsonNode value) {
        var rows = mapper.createArrayNode();
        if (!value.isNull() && !value.isMissingNode()) {
            rows.add(mapper.valueToTree(Map.of("metric", Map.of(), "value", List.of(AT.getEpochSecond(), value.asText()))));
        }
        return rows;
    }

    private JsonNode queryRows(JsonNode fixture, String expression) {
        QueryName query = Arrays.stream(QueryName.values())
                .filter(candidate -> candidate.expression("dev").equals(expression))
                .findFirst()
                .orElseThrow();
        var rows = mapper.createArrayNode();
        if (longDbRows && query == QueryName.DB_POOL_WAIT_PEAK) {
            for (int index = 1; index <= 5; index++) {
                rows.add(mapper.valueToTree(Map.of(
                        "metric", Map.of("pool", "p".repeat(99) + index),
                        "value", List.of(AT.getEpochSecond(), "0")
                )));
            }
            return rows;
        }
        for (JsonNode row : fixture.path("queries").path(query.getId())) {
            rows.add(mapper.valueToTree(Map.of(
                    "metric", row.path("metric"),
                    "value", List.of(AT.getEpochSecond(), row.path("value").asText())
            )));
        }
        return rows;
    }

    private JsonNode logs(JsonNode fixture) {
        var values = mapper.createArrayNode();
        fixture.path("logs").forEach(log -> values.add(mapper.valueToTree(List.of("1", log.toString()))));
        return mapper.valueToTree(List.of(Map.of("values", values)));
    }

    private IncidentSnapshot incident(String id) {
        return new IncidentSnapshot(
                "a".repeat(64),
                "dev",
                "BackendErrors",
                fixture(id).path("profile").asText(),
                AT,
                false
        );
    }

    private JsonNode fixture(String id) {
        return cases.valueStream()
                .filter(fixture -> id.equals(fixture.path("id").asText()))
                .findFirst()
                .orElseThrow();
    }

    private void enqueueSelection(String id) {
        String arguments = mapper.writeValueAsString(Map.of("queries", rubric.path("cases").path(id).path("selected")));
        JsonNode response = mapper.valueToTree(Map.of(
                "status", "completed",
                "usage", Map.of("input_tokens", 100, "output_tokens", 50),
                "output", List.of(Map.of("type", "function_call", "name", "collect_incident_evidence", "call_id", "call_1", "arguments", arguments))
        ));
        server.enqueue(new MockResponse().setBody(response.toString()));
    }

    private void enqueueOutput(String output) {
        finalOutput = output;
    }

    private void enqueueFinal() {
        if (finalResponse != null) {
            server.enqueue(finalResponse);
            return;
        }
        JsonNode response = mapper.valueToTree(Map.of(
                "status", "completed",
                "usage", Map.of("input_tokens", 100, "output_tokens", 50),
                "output", List.of(Map.of("type", "message", "content", List.of(Map.of("type", "output_text", "text", finalOutput))))
        ));
        server.enqueue(new MockResponse().setBody(response.toString()));
    }

    private void assertQueryLinks(PreparedReply reply, InvestigationResult result) {
        JsonNode blocks = mapper.valueToTree(reply.message()).path("blocks");
        JsonNode actions = blocks.get(blocks.size() - 1).path("elements");
        for (int index = 0; index < result.queries().size(); index++) {
            var execution = result.queries().get(index);
            String url = actions.get(index).path("url").asText();
            String encoded = UriComponentsBuilder.fromUriString(url).build().getQueryParams().getFirst("panes");
            JsonNode pane = mapper.readTree(URLDecoder.decode(encoded, StandardCharsets.UTF_8)).path("incident");
            assertThat(pane.path("datasource").asText()).isEqualTo(execution.source());
            assertThat(pane.path("queries").get(0).path("expr").asText()).isEqualTo(execution.expression());
            assertThat(pane.path("range").path("from").asText()).isEqualTo(String.valueOf(execution.from().toEpochMilli()));
            assertThat(pane.path("range").path("to").asText()).isEqualTo(String.valueOf(execution.to().toEpochMilli()));
        }
    }

    private JsonNode resource(String name) throws IOException {
        return mapper.readTree(new ClassPathResource("incident-no-data/" + name).getInputStream());
    }

    private JsonNode request() throws InterruptedException {
        return mapper.readTree(server.takeRequest(1, TimeUnit.SECONDS).getBody().readUtf8());
    }
}
