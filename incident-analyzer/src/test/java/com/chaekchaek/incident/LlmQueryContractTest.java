package com.chaekchaek.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chaekchaek.incident.analysis.AnalysisValidator;
import com.chaekchaek.incident.analysis.IncidentInvestigator;
import com.chaekchaek.incident.analysis.client.LlmClient;
import com.chaekchaek.incident.analysis.service.ModelBudgetService;
import com.chaekchaek.incident.common.JsonHttpClient;
import com.chaekchaek.incident.common.RemoteCallException;
import com.chaekchaek.incident.evidence.QueryName;
import com.chaekchaek.incident.evidence.QueryExecutor;
import com.chaekchaek.incident.evidence.dto.EvidenceBundle;
import com.chaekchaek.incident.evidence.dto.EvidenceItem;
import com.chaekchaek.incident.evidence.dto.QueryBatch;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class LlmQueryContractTest {
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private MockWebServer server;
    private LlmClient llm;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        llm = client(new JsonHttpClient(mapper));
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
        meters.close();
    }

    @Test
    @DisplayName("함수 결과는 모든 첫 응답 항목과 호출 ID를 보존하여 최종 보고로 이어진다")
    void should_ContinueWithOriginalOutput_When_QueryIsSelected() throws Exception {
        // given
        ObjectNode selection = selected("{\"queries\":[\"db_pool_wait_peak\"]}");
        enqueue(selection);
        enqueue(completed("E2"));
        QueryBatch batch = batch();
        // when
        var first = llm.begin(Fixtures.evidence(), Fixtures.incident(), "DB 대기를 확인해 주세요", List.of(), true);
        var result = llm.finish(first, batch, Fixtures.incident());
        // then
        JsonNode initial = request();
        JsonNode last = request();
        JsonNode tool = initial.path("tools").get(0);
        assertThat(tool.path("name").asText()).isEqualTo("collect_incident_evidence");
        assertThat(tool.path("strict").asBoolean()).isTrue();
        assertThat(tool.path("parameters").path("additionalProperties").asBoolean()).isFalse();
        assertThat(tool.path("parameters").path("properties").size()).isEqualTo(1);
        JsonNode queryArray = tool.path("parameters").path("properties").path("queries");
        assertThat(queryArray.path("minItems").asInt()).isEqualTo(1);
        assertThat(queryArray.path("maxItems").asInt()).isEqualTo(4);
        assertThat(queryArray.path("items").path("enum")).isEqualTo(mapper.readTree("""
                ["db_pool_wait_peak","http_latency_p95","error_type_counts","external_api_failure_counts"]
                """));
        assertThat(initial.path("tool_choice").asText()).isEqualTo("auto");
        assertThat(initial.path("parallel_tool_calls").asBoolean()).isFalse();
        assertThat(initial.path("include")).isEqualTo(mapper.readTree("[\"reasoning.encrypted_content\"]"));
        assertThat(last.path("tools")).isEqualTo(initial.path("tools"));
        assertThat(last.path("tool_choice").asText()).isEqualTo("none");
        assertThat(last.path("store").asBoolean()).isFalse();
        assertThat(last.has("previous_response_id")).isFalse();
        JsonNode input = last.path("input");
        assertThat(input.get(0)).isEqualTo(initial.path("input").get(0));
        assertThat(input.get(1)).isEqualTo(selection.path("output").get(0));
        assertThat(input.get(2)).isEqualTo(selection.path("output").get(1));
        assertThat(input.get(3).path("type").asText()).isEqualTo("function_call_output");
        assertThat(input.get(3).path("call_id").asText()).isEqualTo("call_1");
        assertThat(mapper.readTree(input.get(3).path("output").asText()).path("evidence"))
                .isEqualTo(mapper.valueToTree(batch.evidence()));
        JsonNode ids = last.path("text").path("format").path("schema").path("properties")
                .path("observedFacts").path("items").path("properties").path("evidenceIds").path("items").path("enum");
        assertThat(ids).isEqualTo(mapper.readTree("[\"E1\",\"E2\"]"));
        assertThat(first.selection().queries()).containsExactly(QueryName.DB_POOL_WAIT_PEAK);
        assertThat(result.inputTokens()).isEqualTo(200);
        assertThat(result.outputTokens()).isEqualTo(100);
        assertThat(result.evidence()).isEqualTo(batch.evidence().items());
        assertThat(meters.get("incident_llm_tokens").tag("direction", "input").counter().count()).isEqualTo(200);
        assertThat(meters.get("incident_model_request").tag("phase", "initial").counter().count()).isEqualTo(1);
        assertThat(meters.get("incident_model_request").tag("phase", "final").counter().count()).isEqualTo(1);
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("기본 자료로 충분하면 도구가 있어도 첫 응답에서 보고를 마친다")
    void should_ReturnReport_When_FirstTurnDoesNotSelectQueries() throws Exception {
        // given
        enqueue(completed("E1"));
        // when
        var first = llm.begin(Fixtures.evidence(), Fixtures.incident(), "확인 순서", List.of(), true);
        // then
        assertThat(first.requestsQueries()).isFalse();
        assertThat(first.report().report()).isEqualTo(Fixtures.report("E1"));
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"queries\":[\"db_pool_wait_peak\"],\"env\":\"prod\"}",
            "{\"queries\":[\"db_pool_wait_peak\"],\"query\":\"up\"}",
            "{\"queries\":[\"db_pool_wait_peak\",\"db_pool_wait_peak\"]}",
            "{\"queries\":[\"db_pool_wait_peak\",\"http_latency_p95\",\"error_type_counts\",\"external_api_failure_counts\",\"db_pool_wait_peak\"]}",
            "{\"queries\":[\"arbitrary_sql\"]}",
            "{\"queries\":[42]}",
            "{\"queries\":[]}",
            "{\"queries\":null}",
            "not json"
    })
    @DisplayName("허용되지 않은 조회 인자는 실행 가능한 선택으로 반환하지 않는다")
    void should_RejectSelection_When_ArgumentsAreInvalid(String arguments) {
        // given
        enqueue(selected(arguments));
        // when, then
        assertThatThrownBy(() -> llm.begin(Fixtures.evidence(), Fixtures.incident(), "조회", List.of(), true))
                .isInstanceOf(RemoteCallException.class)
                .hasMessage("INVALID_TOOL_ARGUMENTS");
        assertThat(meters.get("incident_llm_tokens").tag("direction", "input").counter().count()).isEqualTo(100);
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"name", "call_id", "multiple"})
    @DisplayName("함수명과 호출 ID가 잘못되거나 함수 호출이 여러 개면 거절한다")
    void should_RejectCall_When_FunctionEnvelopeIsInvalid(String fault) {
        // given
        ObjectNode response = selected("{\"queries\":[\"db_pool_wait_peak\"]}");
        ObjectNode call = (ObjectNode) response.path("output").get(1);
        if ("multiple".equals(fault)) {
            response.withArray("output").add(call.deepCopy());
        } else {
            call.put(fault, "untrusted/function");
        }
        enqueue(response);
        // when, then
        assertThatThrownBy(() -> llm.begin(Fixtures.evidence(), Fixtures.incident(), "조회", List.of(), true))
                .isInstanceOf(RemoteCallException.class);
    }

    @Test
    @DisplayName("최종 응답의 추가 함수 호출은 거절하고 수신한 사용량은 기록한다")
    void should_RejectAdditionalCallAndCountUsage_When_FinalTurnRequestsTool() throws Exception {
        // given
        enqueue(selected("{\"queries\":[\"db_pool_wait_peak\"]}"));
        enqueue(selected("{\"queries\":[\"http_latency_p95\"]}"));
        var first = llm.begin(Fixtures.evidence(), Fixtures.incident(), "조회", List.of(), true);
        // when, then
        assertThatThrownBy(() -> llm.finish(first, batch(), Fixtures.incident()))
                .isInstanceOf(RemoteCallException.class)
                .hasMessage("UNEXPECTED_TOOL_CALL");
        assertThat(meters.get("incident_llm_tokens").tag("direction", "output").counter().count()).isEqualTo(100);
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("도구를 제공하지 않은 첫 요청의 함수 호출도 실행 가능한 선택으로 반환하지 않는다")
    void should_RejectFunctionCall_When_FirstTurnHasNoTools() {
        // given
        enqueue(selected("{\"queries\":[\"db_pool_wait_peak\"]}"));
        // when, then
        assertThatThrownBy(() -> llm.begin(Fixtures.evidence(), Fixtures.incident(), "조회", List.of(), false))
                .isInstanceOf(RemoteCallException.class)
                .hasMessage("UNEXPECTED_TOOL_CALL");
    }

    @Test
    @DisplayName("존재하지 않는 근거를 인용한 응답도 수신한 토큰은 집계한다")
    void should_CountUsage_When_ReportValidationFails() {
        // given
        enqueue(completed("E999"));
        // when, then
        assertThatThrownBy(() -> llm.begin(Fixtures.evidence(), Fixtures.incident(), "조회", List.of(), true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("UNKNOWN_EVIDENCE_REFERENCE");
        assertThat(meters.get("incident_llm_tokens").tag("direction", "input").counter().count()).isEqualTo(100);
    }

    @ParameterizedTest
    @CsvSource({
            "HTTP,HTTP_429,100,50",
            "JSON,ANALYSIS_PROCESSING_ERROR,100,50",
            "REFERENCE,UNKNOWN_EVIDENCE_REFERENCE,200,100"
    })
    @DisplayName("최종 HTTP와 파싱과 근거 검증 실패를 복구하고 수신한 사용량만 지표에 집계한다")
    void should_PreserveEvidenceAndUsage_When_FinalResponseFails(
            String fault,
            String code,
            long inputTokens,
            long outputTokens
    ) throws Exception {
        // given
        enqueue(selected("{\"queries\":[\"db_pool_wait_peak\"]}"));
        server.enqueue(failedFinalResponse(fault));
        QueryExecutor queries = mock(QueryExecutor.class);
        ModelBudgetService budget = mock(ModelBudgetService.class);
        QueryBatch batch = Fixtures.queryBatch();
        when(queries.execute(any(), any(), any(), any())).thenReturn(batch);
        when(budget.reserve()).thenReturn(true);
        var investigator = new IncidentInvestigator(llm, queries, budget, Clock.fixed(Fixtures.NOW, ZoneOffset.UTC));
        // when
        var result = investigator.investigate(
                Fixtures.evidence(),
                Fixtures.incident(),
                "DB 대기를 확인해 주세요",
                List.of(),
                Fixtures.NOW.plusSeconds(90)
        );
        // then
        assertThat(result.failureCode()).isEqualTo(code);
        assertThat(result.analysis().evidence()).isEqualTo(batch.evidence().items());
        assertThat(result.queries()).isEqualTo(batch.executions());
        assertThat(result.analysis().inputTokens()).isEqualTo(100);
        assertThat(result.analysis().outputTokens()).isEqualTo(50);
        assertThat(result.toString()).doesNotContain("private-provider-diagnostic", "private-model-output", "sk-secret", "opaque-content");
        assertThat(meters.get("incident_llm_tokens").tag("direction", "input").counter().count()).isEqualTo(inputTokens);
        assertThat(meters.get("incident_llm_tokens").tag("direction", "output").counter().count()).isEqualTo(outputTokens);
        assertThat(meters.get("incident_model_request").tag("phase", "initial").counter().count()).isEqualTo(1);
        assertThat(meters.get("incident_model_request").tag("phase", "final").counter().count()).isEqualTo(1);
        assertThat(server.getRequestCount()).isEqualTo(2);
        request();
        assertThat(request().path("tool_choice").asText()).isEqualTo("none");
        verify(budget, times(2)).reserve();
        verify(queries).execute(any(), any(), any(), any());
    }

    private MockResponse failedFinalResponse(String fault) {
        if ("HTTP".equals(fault)) {
            return new MockResponse().setResponseCode(429).setBody("private-provider-diagnostic sk-secret");
        }
        if ("JSON".equals(fault)) {
            return new MockResponse().setBody("private-model-output sk-secret");
        }
        ObjectNode body = (ObjectNode) completed("E999");
        ObjectNode report = mapper.valueToTree(Fixtures.report("E999"));
        report.put("summary", "private-model-output sk-secret");
        ((ObjectNode) body.path("output").get(0).path("content").get(0))
                .put("text", mapper.writeValueAsString(report));
        return new MockResponse().setHeader("Content-Type", "application/json").setBody(body.toString());
    }

    @Test
    @DisplayName("암호화 추론을 포함한 연속 입력이 너무 크면 최종 HTTP 요청을 보내지 않는다")
    void should_RejectBeforeSending_When_ContinuationExceedsInputLimit() throws Exception {
        // given
        ObjectNode response = selected("{\"queries\":[\"db_pool_wait_peak\"]}");
        ((ObjectNode) response.path("output").get(0)).put("encrypted_content", "x".repeat(20000));
        enqueue(response);
        var first = llm.begin(Fixtures.evidence(), Fixtures.incident(), "조회", List.of(), true);
        // when, then
        assertThat(llm.continuationFits(first, batch())).isFalse();
        assertThatThrownBy(() -> llm.finish(first, batch(), Fixtures.incident()))
                .isInstanceOf(RemoteCallException.class)
                .hasMessage("INPUT_TOO_LARGE");
        assertThat(server.getRequestCount()).isEqualTo(1);
        assertThat(first.toString()).doesNotContain("encrypted_content", "call_1");
    }

    @Test
    @DisplayName("도구 선택 요청은 짧은 제한 시간을 쓰고 최종 보고는 기존 제한 시간을 쓴다")
    void should_SetPhaseTimeouts_When_SendingModelRequests() throws Exception {
        // given
        JsonHttpClient http = mock(JsonHttpClient.class);
        when(http.post(any(), any(), any(), any())).thenReturn(completed("E1"));
        LlmClient client = client(http);
        // when
        client.begin(Fixtures.evidence(), Fixtures.incident(), "조회", List.of(), true);
        client.begin(Fixtures.evidence(), Fixtures.incident(), "조회", List.of(), false);
        // then
        verify(http).post(any(), any(), any(), eq(Duration.ofSeconds(15)));
        verify(http).post(any(), any(), any(), eq(Duration.ofSeconds(30)));
    }

    private LlmClient client(JsonHttpClient http) throws IOException {
        return new LlmClient(
                Fixtures.properties(server.url("/").toString()),
                http,
                mapper,
                new AnalysisValidator(),
                meters
        );
    }

    private QueryBatch batch() {
        var additional = new EvidenceItem(
                "E2",
                "prometheus",
                "DB 연결 대기 최댓값",
                Map.of("row1", "pool=HikariPool-1, value=0")
        );
        return new QueryBatch(
                new EvidenceBundle(
                        Fixtures.incident().startsAt().minusSeconds(900),
                        Fixtures.incident().startsAt(),
                        List.of(Fixtures.evidence().items().getFirst(), additional),
                        List.of()
                ),
                List.of()
        );
    }

    private ObjectNode selected(String arguments) {
        ObjectNode body = (ObjectNode) mapper.readTree("""
                {"status":"completed","usage":{"input_tokens":100,"output_tokens":50},"output":[
                  {"type":"reasoning","id":"rs_1","summary":[],"encrypted_content":"opaque-content"},
                  {"type":"function_call","id":"fc_1","call_id":"call_1","name":"collect_incident_evidence","arguments":""}
                ]}
                """);
        ((ObjectNode) body.path("output").get(1)).put("arguments", arguments);
        return body;
    }

    private JsonNode completed(String evidenceId) {
        ObjectNode body = (ObjectNode) mapper.readTree("""
                {"status":"completed","usage":{"input_tokens":100,"output_tokens":50},"output":[
                  {"type":"message","content":[{"type":"output_text","text":""}]}
                ]}
                """);
        ((ObjectNode) body.path("output").get(0).path("content").get(0))
                .put("text", mapper.writeValueAsString(Fixtures.report(evidenceId)));
        return body;
    }

    private void enqueue(JsonNode response) {
        server.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(response.toString()));
    }

    private JsonNode request() throws InterruptedException {
        return mapper.readTree(server.takeRequest(1, TimeUnit.SECONDS).getBody().readUtf8());
    }
}
