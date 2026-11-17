package com.chaekchaek.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;

import com.chaekchaek.incident.alert.domain.IncidentReference;
import com.chaekchaek.incident.alert.service.IncidentService;
import com.chaekchaek.incident.analysis.IncidentInvestigator;
import com.chaekchaek.incident.analysis.dto.InvestigationResult;
import com.chaekchaek.incident.common.RemoteCallException;
import com.chaekchaek.incident.evidence.EvidenceCollector;
import com.chaekchaek.incident.notification.AnalysisMessage;
import com.chaekchaek.incident.question.QuestionWorker;
import com.chaekchaek.incident.question.domain.QuestionRejectedException;
import com.chaekchaek.incident.question.dto.PreparedReply;
import com.chaekchaek.incident.question.service.QuestionService;
import com.chaekchaek.incident.slack.ParentAlertResolver;
import com.chaekchaek.incident.slack.client.SlackApiClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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
        worker = new QuestionWorker(
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
                List.of()
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
