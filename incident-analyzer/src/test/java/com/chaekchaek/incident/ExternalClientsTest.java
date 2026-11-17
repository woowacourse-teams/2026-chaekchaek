package com.chaekchaek.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaekchaek.incident.analysis.AnalysisValidator;
import com.chaekchaek.incident.analysis.client.LlmClient;
import com.chaekchaek.incident.common.JsonHttpClient;
import com.chaekchaek.incident.common.RemoteCallException;
import com.chaekchaek.incident.evidence.client.LokiClient;
import com.chaekchaek.incident.evidence.dto.EvidenceBundle;
import com.chaekchaek.incident.evidence.dto.EvidenceItem;
import com.chaekchaek.incident.notification.AnalysisMessage;
import com.chaekchaek.incident.question.dto.ConversationTurn;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
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
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

class ExternalClientsTest {
    private final JsonMapper mapper = JsonMapper.builder().build();
    private MockWebServer server;
    private LlmClient llm;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        llm = new LlmClient(
                Fixtures.properties(server.url("/").toString()),
                new JsonHttpClient(mapper),
                mapper,
                new AnalysisValidator(),
                new SimpleMeterRegistry()
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    @DisplayName("구조화된 출력과 저장 비활성화를 요청하고 근거와 사용량을 보존한다")
    void should_RequestStrictSchema_When_AnalyzingEvidence() throws Exception {
        // given
        server.enqueue(completed("E1"));
        // when
        var result = llm.analyze(
                Fixtures.evidence(),
                Fixtures.incident(),
                "확인 순서",
                List.of(new ConversationTurn(
                        "이전 질문",
                        "이전 답변"
                ))
        );
        // then
        var request = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-api-key");
        var body = mapper.readTree(request.getBody().readUtf8());
        assertThat(body.path("store").asBoolean()).isFalse();
        assertThat(body.path("text").path("format").path("strict").asBoolean()).isTrue();
        assertThat(body.path("tools").isMissingNode()).isTrue();
        for (String field : List.of("observedFacts", "hypotheses")) {
            var references = body.path("text").path("format").path("schema").path("properties")
                    .path(field).path("items").path("properties").path("evidenceIds");
            assertThat(references.path("minItems").asInt()).isEqualTo(1);
            assertThat(references.path("maxItems").asInt()).isEqualTo(10);
            assertThat(references.path("items").path("enum")).isEqualTo(mapper.readTree("[\"E1\"]"));
        }
        assertThat(body.path("max_output_tokens").asInt()).isEqualTo(2000);
        assertThat(body.path("input").toString()).contains("확인 순서", "이전 질문", "이전 답변", "Backend5xx", "resolved");
        assertThat(result.inputTokens()).isEqualTo(100);
        assertThat(result.evidence()).isEqualTo(Fixtures.evidence().items());
    }

    @Test
    @DisplayName("모델이 존재하지 않는 근거를 인용하면 발송할 결과로 채택하지 않는다")
    void should_Reject_When_ModelInventsEvidenceId() {
        // given
        server.enqueue(completed("E999"));
        // when, then
        assertThatThrownBy(() -> llm.analyze(
                Fixtures.evidence(),
                Fixtures.incident(),
                "확인 순서",
                List.of(new ConversationTurn(
                        "이전 질문",
                        "이전 답변"
                ))
        ))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"status\":\"incomplete\",\"output\":[]}",
            "{\"status\":\"completed\",\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"refusal\"}]}]}"
    })
    @DisplayName("모델 거절과 미완성 응답을 분석 결과로 채택하지 않는다")
    void should_Reject_When_ResponseIsRefusedOrIncomplete(String response) {
        // given
        server.enqueue(new MockResponse().setBody(response));
        // when, then
        assertThatThrownBy(() -> llm.analyze(
                Fixtures.evidence(),
                Fixtures.incident(),
                "확인 순서",
                List.of(new ConversationTurn(
                        "이전 질문",
                        "이전 답변"
                ))
        ))
                .isInstanceOf(RemoteCallException.class);
    }

    @Test
    @DisplayName("모니터링 조회 실패의 응답 본문은 오류에 노출하지 않는다")
    void should_HideResponseBody_When_UpstreamReturnsError() {
        // given
        server.enqueue(new MockResponse().setResponseCode(429).setBody("private-provider-diagnostic"));
        // when, then
        assertThatThrownBy(() -> llm.analyze(
                Fixtures.evidence(),
                Fixtures.incident(),
                "확인 순서",
                List.of(new ConversationTurn(
                        "이전 질문",
                        "이전 답변"
                ))
        ))
                .isInstanceOf(RemoteCallException.class)
                .hasMessageNotContaining("private-provider-diagnostic");
    }

    @Test
    @DisplayName("Loki 조회 범위와 환경을 고정하고 오류 로그만 표본으로 수집한다")
    void should_SendBoundedQuery_When_CollectingLogs() throws Exception {
        // given
        server.enqueue(new MockResponse().setBody("{\"status\":\"success\",\"data\":{\"result\":[]}}"));
        var loki = new LokiClient(Fixtures.properties(server.url("/").toString()), new JsonHttpClient(mapper));
        // when
        loki.logs("dev", Fixtures.NOW.minusSeconds(300), Fixtures.NOW);
        // then
        var url = server.takeRequest(1, TimeUnit.SECONDS).getRequestUrl();
        assertThat(url.queryParameter("query")).contains("env=\"dev\"", "type=\"error\"", "status >= 500", "status < 600");
        assertThat(url.queryParameter("limit")).isEqualTo("200");
        assertThat(url.queryParameter("end")).isEqualTo(Fixtures.NOW.getEpochSecond() + "000000000");
    }

    @Test
    @DisplayName("슬랙 메시지는 근거 스냅샷과 구간 링크를 포함하고 일반 텍스트로 전송한다")
    void should_IncludeEvidenceWithoutMarkup_When_SendingSlack() throws Exception {
        // given
        var messages = new AnalysisMessage(Fixtures.properties(server.url("/").toString()), mapper);
        // when
        var reply = messages.reply(Fixtures.incident(), Fixtures.result());
        // then
        var body = mapper.valueToTree(reply.message());
        assertThat(body.toString()).contains("E1", "value=5", "schemaVersion=1", "panes=");
        assertThat(body.toString()).doesNotContain("mrkdwn");
        assertThat(body.path("blocks").size()).isLessThan(50);
    }

    @Test
    @DisplayName("다음 질문은 해당 요청의 근거 ID만 출력 스키마에서 허용한다")
    void should_IsolateEvidenceEnum_When_AnalyzingAnotherQuestion() throws Exception {
        // given
        server.enqueue(completed("E1"));
        server.enqueue(completed("E2"));
        var secondEvidence = new EvidenceBundle(
                Fixtures.evidence().from(),
                Fixtures.evidence().to(),
                List.of(new EvidenceItem(
                        "E2",
                        "prometheus",
                        "백엔드 수집 성공 여부",
                        Map.of("value", "1")
                )),
                List.of()
        );
        // when
        llm.analyze(Fixtures.evidence(), Fixtures.incident(), "첫 질문", List.of());
        llm.analyze(secondEvidence, Fixtures.incident(), "다음 질문", List.of());
        // then
        server.takeRequest(1, TimeUnit.SECONDS);
        var body = mapper.readTree(server.takeRequest(1, TimeUnit.SECONDS).getBody().readUtf8());
        var references = body.path("text").path("format").path("schema").path("properties")
                .path("hypotheses").path("items").path("properties").path("evidenceIds");
        assertThat(references.path("items").path("enum")).isEqualTo(mapper.readTree("[\"E2\"]"));
    }

    private MockResponse completed(String evidenceId) {
        var body = Map.of(
                "status",
                "completed",
                "usage",
                Map.of("input_tokens", 100, "output_tokens", 50),
                "output",
                List.of(Map.of(
                        "type",
                        "message",
                        "content",
                        List.of(Map.of(
                                "type",
                                "output_text",
                                "text",
                                mapper.writeValueAsString(Fixtures.report(evidenceId))
                        ))
                ))
        );
        return new MockResponse().setHeader("Content-Type", "application/json").setBody(mapper.writeValueAsString(body));
    }
}
