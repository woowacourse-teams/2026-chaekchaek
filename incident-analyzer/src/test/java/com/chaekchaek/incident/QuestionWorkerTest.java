package com.chaekchaek.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.chaekchaek.incident.alert.domain.IncidentReference;
import com.chaekchaek.incident.alert.service.IncidentService;
import com.chaekchaek.incident.analysis.IncidentInvestigator;
import com.chaekchaek.incident.analysis.client.LlmClient;
import com.chaekchaek.incident.analysis.dto.AnalysisReport;
import com.chaekchaek.incident.analysis.dto.AnalysisResult;
import com.chaekchaek.incident.analysis.dto.InvestigationResult;
import com.chaekchaek.incident.analysis.dto.LlmTurn;
import com.chaekchaek.incident.analysis.dto.NextCheck;
import com.chaekchaek.incident.analysis.service.ModelBudgetService;
import com.chaekchaek.incident.common.RemoteCallException;
import com.chaekchaek.incident.evidence.EvidenceCollector;
import com.chaekchaek.incident.evidence.QueryExecutor;
import com.chaekchaek.incident.evidence.QueryName;
import com.chaekchaek.incident.evidence.dto.EvidenceBundle;
import com.chaekchaek.incident.evidence.dto.QueryBatch;
import com.chaekchaek.incident.evidence.dto.QueryExecution;
import com.chaekchaek.incident.evidence.dto.QuerySelection;
import com.chaekchaek.incident.evidence.dto.QueryStatus;
import com.chaekchaek.incident.notification.AnalysisMessage;
import com.chaekchaek.incident.question.QuestionWorker;
import com.chaekchaek.incident.question.domain.QuestionRejectedException;
import com.chaekchaek.incident.question.dto.PreparedReply;
import com.chaekchaek.incident.question.service.QuestionService;
import com.chaekchaek.incident.slack.ParentAlertResolver;
import com.chaekchaek.incident.slack.client.SlackApiClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class QuestionWorkerTest {
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    @Mock
    private QuestionService questions;
    @Mock
    private IncidentService incidents;
    @Mock
    private ParentAlertResolver parents;
    @Mock
    private EvidenceCollector evidence;
    @Mock
    private IncidentInvestigator investigator;
    @Mock
    private SlackApiClient slack;
    private QuestionWorker worker;

    @BeforeEach
    void setUp() {
        worker = worker(investigator);
        given(questions.canDeliver(any())).willReturn(true);
    }

    @Test
    @DisplayName("복구된 장애도 질문에 답하고 준비한 결과를 발송 전에 저장한다")
    void should_AnswerResolvedIncident_When_HumanAsks() throws Exception {
        // given
        arrangeIncident();
        given(evidence.collect(Fixtures.incident())).willReturn(Fixtures.evidence());
        given(questions.conversation(any())).willReturn(List.of());
        given(investigator.investigate(any(), eq(Fixtures.incident()), any(), any(), any())).willReturn(new InvestigationResult(
                Fixtures.result(),
                List.of(),
                null
        ));
        // when
        worker.process(Fixtures.task(1, null));
        // then
        var order = inOrder(questions, slack);
        order.verify(questions).prepare(eq("EvQUESTION"), any());
        order.verify(slack).reply(eq(Fixtures.task(1, null)), any());
        order.verify(questions).delivered("EvQUESTION");
        verify(investigator).investigate(Fixtures.evidence(), Fixtures.incident(), "어디를 먼저 확인해야 하나요?", List.of(), Fixtures.NOW.plusSeconds(90));
        assertThat(meters.find("incident_llm_tokens").counter()).isNull();
    }

    @Test
    @DisplayName("발송 재시도는 저장된 답변만 사용하고 모델을 다시 호출하지 않는다")
    void should_ReuseReply_When_DeliveryIsRetried() {
        // given
        var reply = PreparedReply.notice("저장된 답변");
        var task = Fixtures.task(1, mapper.writeValueAsString(reply));
        // when
        worker.process(task);
        // then
        verify(slack).reply(task, reply);
        verifyNoInteractions(parents, incidents, evidence, investigator);
        assertThat(meters.find("incident_llm_tokens").counter()).isNull();
    }

    @Test
    @DisplayName("처리 도중 재시작된 질문은 모델 호출을 반복하지 않는다")
    void should_AvoidSecondModelCall_When_ProcessingWasInterrupted() {
        // given, when
        worker.process(Fixtures.task(2, null));
        // then
        verify(questions).prepare(eq("EvQUESTION"), any());
        verifyNoInteractions(parents, incidents, evidence, investigator);
    }

    @Test
    @DisplayName("원본이 검증되지 않은 질문에는 안내만 보내고 분석하지 않는다")
    void should_SkipAnalysis_When_ParentIsUntrusted() {
        // given
        given(parents.resolve(any())).willThrow(new QuestionRejectedException("원본 스레드에서 질문해 주세요"));
        // when
        worker.process(Fixtures.task(1, null));
        // then
        verify(slack).reply(eq(Fixtures.task(1, null)), any());
        verifyNoInteractions(incidents, evidence, investigator);
    }

    @Test
    @DisplayName("발송 실패를 기록하고 준비한 결과를 보존한다")
    void should_RecordFailure_When_SlackFails() {
        // given
        var reply = PreparedReply.notice("저장된 답변");
        var task = Fixtures.task(1, mapper.writeValueAsString(reply));
        doThrow(new RemoteCallException("SLACK_rate_limited")).when(slack).reply(task, reply);
        // when
        worker.process(task);
        // then
        verify(questions).fail("EvQUESTION", "SLACK_rate_limited");
        verifyNoInteractions(investigator);
    }

    @Test
    @DisplayName("기본 관측이 없으면 기존 안내를 보내고 모델과 추가 조회를 시작하지 않는다")
    void should_SkipInvestigation_When_BaseEvidenceIsEmpty() {
        // given
        arrangeIncident();
        given(evidence.collect(Fixtures.incident())).willReturn(new EvidenceBundle(
                Fixtures.incident().startsAt().minusSeconds(600),
                Fixtures.incident().startsAt(),
                List.of(),
                List.of("관측 자료가 없습니다")
        ));
        // when
        worker.process(Fixtures.task(1, null));
        // then
        var reply = ArgumentCaptor.forClass(PreparedReply.class);
        verify(questions).prepare(eq("EvQUESTION"), reply.capture());
        assertThat(reply.getValue().message().toString()).contains("발생 시점의 관측 자료를 확인할 수 없습니다");
        assertThat(reply.getValue().inputTokens()).isZero();
        assertThat(reply.getValue().outputTokens()).isZero();
        verifyNoInteractions(investigator);
    }

    @ParameterizedTest
    @ValueSource(strings = {"INVALID_MODEL_TEXT", "UNKNOWN_EVIDENCE_REFERENCE"})
    @DisplayName("모델 출력 검증 실패를 원인 코드로 남긴다")
    void should_RecordSafeFailureCode_When_ModelOutputIsInvalid(String code) throws Exception {
        // given
        arrangeIncident();
        given(evidence.collect(Fixtures.incident())).willReturn(Fixtures.evidence());
        given(investigator.investigate(any(), any(), any(), any(), any())).willThrow(new IllegalArgumentException(code));
        // when
        worker.process(Fixtures.task(1, null));
        // then
        verify(questions).analysisFailed("EvQUESTION", code);
        verify(questions).delivered("EvQUESTION");
    }

    @Test
    @DisplayName("예외 메시지에 비밀값이 있어도 저장하거나 발송하지 않는다")
    void should_HideExceptionDetails_When_UnexpectedAnalysisFails() {
        // given
        given(parents.resolve(any())).willThrow(new IllegalStateException("private-question sk-secret"));
        // when
        worker.process(Fixtures.task(1, null));
        // then
        verify(questions).analysisFailed("EvQUESTION", "ANALYSIS_PROCESSING_ERROR");
        var reply = ArgumentCaptor.forClass(PreparedReply.class);
        verify(questions).prepare(eq("EvQUESTION"), reply.capture());
        assertThat(reply.getValue().message().toString()).doesNotContain("private-question", "sk-secret");
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    @DisplayName("최종 모델 실패 뒤 근거 답변을 저장하고 재발송해도 분석 실패와 조회를 반복하지 않는다")
    void should_PreserveEvidenceAndFailure_When_FinalAnswerIsRetried(CapturedOutput output) throws Exception {
        // given
        arrangeIncident();
        given(evidence.collect(Fixtures.incident())).willReturn(Fixtures.evidence());
        LlmClient llm = mock(LlmClient.class);
        QueryExecutor queries = mock(QueryExecutor.class);
        ModelBudgetService budget = mock(ModelBudgetService.class);
        var first = new LlmTurn(
                mapper.readTree("[{\"content\":\"private-question sk-secret\"}]"),
                mapper.readTree("[{\"type\":\"reasoning\",\"encrypted_content\":\"opaque-private-reasoning\"}]"),
                null,
                new QuerySelection(List.of(QueryName.DB_POOL_WAIT_PEAK)),
                "call_1",
                100,
                50
        );
        var batch = Fixtures.queryBatch();
        given(budget.reserve()).willReturn(true);
        given(llm.begin(any(), any(), any(), any(), eq(true))).willReturn(first);
        given(queries.execute(any(), any(), any(), any())).willReturn(batch);
        given(llm.continuationFits(first, batch)).willReturn(true);
        given(llm.finish(first, batch, Fixtures.incident())).willThrow(new IllegalStateException("private-question sk-secret"));
        var actualInvestigator = new IncidentInvestigator(llm, queries, budget, Clock.fixed(Fixtures.NOW, ZoneOffset.UTC));
        var actualWorker = worker(actualInvestigator);
        var task = Fixtures.task(1, null);
        // when
        actualWorker.process(task);
        // then
        var reply = ArgumentCaptor.forClass(PreparedReply.class);
        var order = inOrder(questions, slack);
        order.verify(questions).analysisFailed("EvQUESTION", "ANALYSIS_PROCESSING_ERROR");
        order.verify(questions).prepare(eq("EvQUESTION"), reply.capture());
        order.verify(slack).reply(task, reply.getValue());
        order.verify(questions).delivered("EvQUESTION");
        JsonNode body = mapper.valueToTree(reply.getValue().message());
        assertThat(body.toString()).contains("E1", "E2", "value=5", "pool=HikariPool-1, value=3", "SUCCESS", "표본만 확인했습니다");
        assertThat(body.toString()).doesNotContain("private-question", "sk-secret", "opaque-private-reasoning", "encrypted_content");
        var actions = body.path("blocks")
                .valueStream()
                .filter(block -> "actions".equals(block.path("type").asText()))
                .toList();
        assertThat(actions).hasSize(2);
        JsonNode baseQuery = pane(actions.getFirst().path("elements").get(0).path("url").asText()).path("queries").get(0);
        assertThat(baseQuery.path("queryType").asText()).isEqualTo("range");
        JsonNode additional = pane(actions.getLast().path("elements").get(0).path("url").asText());
        assertThat(additional.path("datasource").asText()).isEqualTo("prometheus");
        assertThat(additional.path("queries").get(0).path("expr").asText()).isEqualTo(QueryName.DB_POOL_WAIT_PEAK.expression("dev"));
        assertThat(additional.path("queries").get(0).path("instant").asBoolean()).isTrue();
        assertThat(additional.path("range").path("from").asText()).isEqualTo(String.valueOf(batch.evidence().from().toEpochMilli()));
        assertThat(additional.path("range").path("to").asText()).isEqualTo(String.valueOf(batch.evidence().to().toEpochMilli()));
        assertThat(reply.getValue().inputTokens()).isEqualTo(100);
        assertThat(reply.getValue().outputTokens()).isEqualTo(50);

        // when
        var retry = Fixtures.task(1, mapper.writeValueAsString(reply.getValue()));
        actualWorker.process(retry);
        // then
        verify(slack).reply(retry, reply.getValue());
        verify(questions).analysisFailed("EvQUESTION", "ANALYSIS_PROCESSING_ERROR");
        verify(questions).prepare(eq("EvQUESTION"), any());
        verify(questions, times(2)).delivered("EvQUESTION");
        verify(budget, times(2)).reserve();
        verify(llm).begin(any(), any(), any(), any(), eq(true));
        verify(llm).finish(first, batch, Fixtures.incident());
        verify(queries).execute(any(), any(), any(), any());
        assertThat(meters.get("incident_analysis").tag("outcome", "failed").tag("reason", "ANALYSIS_PROCESSING_ERROR").counter().count()).isEqualTo(1);
        assertThat(meters.find("incident_llm_tokens").counter()).isNull();
        assertThat(output.getAll()).contains("code=ANALYSIS_PROCESSING_ERROR").doesNotContain("private-question", "sk-secret", "opaque-private-reasoning");
    }

    @Test
    @DisplayName("빈 조회 답변의 Slack 발송이 실패해도 저장한 관측과 점검 제안을 그대로 재전송한다")
    void should_ReusePreparedNoDataReply_When_FirstSlackDeliveryFails() throws Exception {
        // given
        arrangeIncident();
        given(evidence.collect(Fixtures.incident())).willReturn(Fixtures.evidence());
        LlmClient llm = mock(LlmClient.class);
        QueryExecutor queries = mock(QueryExecutor.class);
        ModelBudgetService budget = mock(ModelBudgetService.class);
        var first = new LlmTurn(
                mapper.createArrayNode(),
                mapper.createArrayNode(),
                null,
                new QuerySelection(List.of(QueryName.DB_POOL_WAIT_PEAK, QueryName.HTTP_LATENCY_P95)),
                "call_1",
                100,
                50
        );
        var successful = Fixtures.queryBatch();
        var batch = new QueryBatch(
                successful.evidence(),
                List.of(successful.executions().getFirst(), new QueryExecution(
                        "http_latency_p95",
                        "prometheus",
                        QueryName.HTTP_LATENCY_P95.getDescription(),
                        QueryName.HTTP_LATENCY_P95.expression("dev"),
                        Fixtures.incident().startsAt().minusSeconds(900),
                        Fixtures.incident().startsAt(),
                        QueryStatus.NO_DATA,
                        false,
                        "",
                        List.of("유효한 관측 자료가 없습니다. 정상값 0을 의미하지 않습니다")
                ))
        );
        var result = new AnalysisResult(
                new AnalysisReport(
                        "일부 추가 조회에 유효한 관측 자료가 없습니다",
                        "PARTIAL",
                        List.of(),
                        List.of(),
                        List.of()
                ),
                "test-model",
                "v4-no-data-next-checks",
                200,
                100,
                batch.evidence().missingEvidence(),
                batch.evidence().items(),
                List.of(new NextCheck(
                        "같은 구간 histogram 수집과 요청량을 확인하세요",
                        "표본이 수집됐는지 확인합니다"
                ))
        );
        given(budget.reserve()).willReturn(true);
        given(llm.begin(any(), any(), any(), any(), eq(true))).willReturn(first);
        given(queries.execute(any(), any(), any(), any())).willReturn(batch);
        given(llm.continuationFits(first, batch)).willReturn(true);
        given(llm.finish(first, batch, Fixtures.incident())).willReturn(result);
        doThrow(new RemoteCallException("SLACK_rate_limited")).doNothing().when(slack).reply(any(), any());
        var actualWorker = worker(new IncidentInvestigator(llm, queries, budget, Clock.fixed(Fixtures.NOW, ZoneOffset.UTC)));
        // when
        actualWorker.process(Fixtures.task(1, null));
        // then
        var reply = ArgumentCaptor.forClass(PreparedReply.class);
        verify(questions).prepare(eq("EvQUESTION"), reply.capture());
        verify(questions).fail("EvQUESTION", "SLACK_rate_limited");
        assertThat(reply.getValue().message().toString()).contains("NO_DATA", "다음 점검 제안", "value=3");
        assertThat(reply.getValue().contextSummary()).contains("http_latency_p95", "NO_DATA", "value=3");
        // when
        var retry = Fixtures.task(2, mapper.writeValueAsString(reply.getValue()));
        actualWorker.process(retry);
        // then
        verify(slack).reply(retry, reply.getValue());
        verify(questions).prepare(eq("EvQUESTION"), any());
        verify(questions).delivered("EvQUESTION");
        verify(questions, never()).analysisFailed(any(), any());
        verify(budget, times(2)).reserve();
        verify(llm).begin(any(), any(), any(), any(), eq(true));
        verify(llm).finish(first, batch, Fixtures.incident());
        verify(queries).execute(any(), any(), any(), any());
    }

    private JsonNode pane(String url) {
        String encoded = UriComponentsBuilder.fromUriString(url).build().getQueryParams().getFirst("panes");
        return mapper.readTree(URLDecoder.decode(encoded, StandardCharsets.UTF_8)).path("incident");
    }

    private QuestionWorker worker(IncidentInvestigator investigator) {
        return new QuestionWorker(
                questions,
                incidents,
                parents,
                evidence,
                investigator,
                new AnalysisMessage(Fixtures.properties("http://localhost/"), mapper),
                slack,
                mapper,
                meters,
                Clock.fixed(Fixtures.NOW, ZoneOffset.UTC)
        );
    }

    private void arrangeIncident() {
        var reference = new IncidentReference(
                "dev",
                "fingerprint",
                Fixtures.NOW.minusSeconds(120)
        );
        given(parents.resolve(any())).willReturn(reference);
        given(incidents.find(reference)).willReturn(Optional.of(Fixtures.incident()));
    }
}
