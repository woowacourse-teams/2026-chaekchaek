package com.chaekchaek.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.chaekchaek.incident.analysis.IncidentInvestigator;
import com.chaekchaek.incident.analysis.client.LlmClient;
import com.chaekchaek.incident.analysis.dto.InvestigationResult;
import com.chaekchaek.incident.analysis.dto.LlmTurn;
import com.chaekchaek.incident.analysis.service.ModelBudgetService;
import com.chaekchaek.incident.common.RemoteCallException;
import com.chaekchaek.incident.evidence.QueryExecutor;
import com.chaekchaek.incident.evidence.QueryName;
import com.chaekchaek.incident.evidence.dto.QueryBatch;
import com.chaekchaek.incident.evidence.dto.QueryExecution;
import com.chaekchaek.incident.evidence.dto.QuerySelection;
import com.chaekchaek.incident.evidence.dto.QueryStatus;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class IncidentInvestigatorTest {
    private final JsonMapper mapper = JsonMapper.builder().build();
    @Mock
    private LlmClient llm;
    @Mock
    private QueryExecutor queries;
    @Mock
    private ModelBudgetService budget;
    @Mock
    private Clock clock;
    private IncidentInvestigator investigator;

    @BeforeEach
    void setUp() {
        investigator = new IncidentInvestigator(llm, queries, budget, clock);
        given(clock.instant()).willReturn(Fixtures.NOW);
    }

    @ParameterizedTest
    @CsvSource({"90,true", "75,true", "74,false", "35,false"})
    @DisplayName("남은 시간에 따라 첫 요청의 도구 사용을 제한하고 직접 보고는 한 번만 호출한다")
    void should_ReserveOnceAndRestrictTools_When_FirstTurnReturnsReport(int seconds, boolean allowQueries) throws Exception {
        // given
        given(budget.reserve()).willReturn(true);
        given(llm.begin(any(), any(), any(), any(), eq(allowQueries))).willReturn(directReport());
        // when
        var result = investigator.investigate(
                Fixtures.evidence(),
                Fixtures.incident(),
                "확인 순서",
                List.of(),
                Fixtures.NOW.plusSeconds(seconds)
        );
        // then
        assertThat(result.analysis()).isEqualTo(Fixtures.result());
        assertThat(result.queries()).isEmpty();
        var order = inOrder(budget, llm);
        order.verify(budget).reserve();
        order.verify(llm).begin(Fixtures.evidence(), Fixtures.incident(), "확인 순서", List.of(), allowQueries);
        verify(budget).reserve();
        verify(llm, never()).finish(any(), any(), any());
        verifyNoInteractions(queries);
    }

    @Test
    @DisplayName("첫 요청에 쓸 시간이 부족하면 예산을 예약하거나 모델을 호출하지 않는다")
    void should_PreserveBaseEvidence_When_DeadlineIsNear() throws Exception {
        // given, when
        var result = investigator.investigate(
                Fixtures.evidence(),
                Fixtures.incident(),
                "확인 순서",
                List.of(),
                Fixtures.NOW.plusSeconds(34)
        );
        // then
        assertThat(result.analysis().evidence()).isEqualTo(Fixtures.evidence().items());
        assertThat(result.analysis().report().missingEvidence()).containsExactly("답변 제한 시간이 부족합니다");
        verifyNoInteractions(llm, queries, budget);
    }

    @Test
    @DisplayName("첫 호출 예산이 없으면 수집한 기본 근거와 제한 안내를 반환한다")
    void should_PreserveEvidence_When_InitialBudgetIsUnavailable() throws Exception {
        // given
        given(budget.reserve()).willReturn(false);
        // when
        var result = investigate();
        // then
        assertThat(result.analysis().report().assessment()).isEqualTo("INSUFFICIENT_EVIDENCE");
        assertThat(result.analysis().report().missingEvidence()).containsExactly("LLM이 비활성 상태이거나 오늘의 호출 한도에 도달했습니다");
        assertThat(result.analysis().evidence()).isEqualTo(Fixtures.evidence().items());
        verifyNoInteractions(llm, queries);
    }

    @Test
    @DisplayName("조회가 선택되면 각 모델 요청 직전에 별도로 예산을 예약한다")
    void should_ReserveBeforeEachRequest_When_QueriesAreSelected() throws Exception {
        // given
        LlmTurn first = selected();
        QueryBatch batch = batch();
        arrangeSelection(first, batch);
        given(budget.reserve()).willReturn(true);
        given(llm.continuationFits(first, batch)).willReturn(true);
        given(llm.finish(first, batch, Fixtures.incident())).willReturn(Fixtures.result());
        // when
        var result = investigate();
        // then
        var order = inOrder(budget, llm, queries);
        order.verify(budget).reserve();
        order.verify(llm).begin(any(), any(), any(), any(), eq(true));
        order.verify(queries).execute(first.selection(), Fixtures.incident(), Fixtures.evidence(), Fixtures.NOW.plusSeconds(90));
        order.verify(llm).continuationFits(first, batch);
        order.verify(budget).reserve();
        order.verify(llm).finish(first, batch, Fixtures.incident());
        order.verifyNoMoreInteractions();
        assertThat(result.queries()).isEqualTo(batch.executions());
        assertThat(result.analysis()).isEqualTo(Fixtures.result());
    }

    @Test
    @DisplayName("두 번째 호출 예산이 없으면 조회 기록과 첫 사용량을 보존한다")
    void should_PreserveQueryResults_When_FinalBudgetIsUnavailable() throws Exception {
        // given
        LlmTurn first = selected();
        QueryBatch batch = batch();
        arrangeSelection(first, batch);
        given(budget.reserve()).willReturn(true, false);
        given(llm.continuationFits(first, batch)).willReturn(true);
        // when
        var result = investigate();
        // then
        assertThat(result.queries()).isEqualTo(batch.executions());
        assertThat(result.analysis().evidence()).isEqualTo(batch.evidence().items());
        assertThat(result.analysis().inputTokens()).isEqualTo(100);
        assertThat(result.analysis().outputTokens()).isEqualTo(50);
        assertThat(result.analysis().model()).isEqualTo("none");
        assertThat(result.analysis().report().missingEvidence()).containsExactly("추가 조회 후 오늘의 모델 호출 한도에 도달했습니다");
        verify(budget, times(2)).reserve();
        verify(llm, never()).finish(any(), any(), any());
    }

    @Test
    @DisplayName("조회 후 시간이 부족해지면 결과를 보존하고 최종 예산을 예약하지 않는다")
    void should_PreserveQueryResults_When_TimeExpiresAfterCollection() throws Exception {
        // given
        LlmTurn first = selected();
        QueryBatch batch = batch();
        arrangeSelection(first, batch);
        given(budget.reserve()).willReturn(true);
        given(clock.instant()).willReturn(Fixtures.NOW, Fixtures.NOW, Fixtures.NOW.plusSeconds(56));
        // when
        var result = investigate();
        // then
        assertThat(result.queries()).isEqualTo(batch.executions());
        assertThat(result.analysis().inputTokens()).isEqualTo(100);
        assertThat(result.analysis().report().missingEvidence()).containsExactly("추가 조회 후 최종 답변 제한 시간이 부족합니다");
        verify(budget).reserve();
        verify(llm, never()).continuationFits(any(), any());
        verify(llm, never()).finish(any(), any(), any());
    }

    @Test
    @DisplayName("연속 입력이 너무 크면 조회 결과를 보존하고 최종 요청을 생략한다")
    void should_PreserveQueryResults_When_ContinuationIsTooLarge() throws Exception {
        // given
        LlmTurn first = selected();
        QueryBatch batch = batch();
        arrangeSelection(first, batch);
        given(budget.reserve()).willReturn(true);
        given(llm.continuationFits(first, batch)).willReturn(false);
        // when
        var result = investigate();
        // then
        assertThat(result.queries()).isEqualTo(batch.executions());
        assertThat(result.analysis().report().missingEvidence()).containsExactly("추가 조회 결과가 모델 입력량 제한을 넘었습니다");
        verify(budget).reserve();
        verify(llm, never()).finish(any(), any(), any());
    }

    @Test
    @DisplayName("모델 도구 인자 검증이 실패하면 관측 쿼리를 실행하지 않는다")
    void should_SkipAllQueries_When_ToolArgumentsAreRejected() throws Exception {
        // given
        given(budget.reserve()).willReturn(true);
        given(llm.begin(any(), any(), any(), any(), eq(true))).willThrow(new RemoteCallException("INVALID_TOOL_ARGUMENTS"));
        // when, then
        assertThatThrownBy(this::investigate)
                .isInstanceOf(RemoteCallException.class)
                .hasMessage("INVALID_TOOL_ARGUMENTS");
        verifyNoInteractions(queries);
        verify(budget).reserve();
    }

    @Test
    @DisplayName("최종 모델 오류 후 조회나 모델을 다시 실행하지 않는다")
    void should_StopWithoutRetry_When_FinalModelFails() throws Exception {
        // given
        LlmTurn first = selected();
        QueryBatch batch = batch();
        arrangeSelection(first, batch);
        given(budget.reserve()).willReturn(true);
        given(llm.continuationFits(first, batch)).willReturn(true);
        given(llm.finish(first, batch, Fixtures.incident())).willThrow(new RemoteCallException("UNEXPECTED_TOOL_CALL"));
        // when, then
        assertThatThrownBy(this::investigate)
                .isInstanceOf(RemoteCallException.class)
                .hasMessage("UNEXPECTED_TOOL_CALL");
        verify(budget, times(2)).reserve();
        verify(queries).execute(first.selection(), Fixtures.incident(), Fixtures.evidence(), Fixtures.NOW.plusSeconds(90));
        verify(llm).finish(first, batch, Fixtures.incident());
    }

    private InvestigationResult investigate() throws Exception {
        return investigator.investigate(
                Fixtures.evidence(),
                Fixtures.incident(),
                "확인 순서",
                List.of(),
                Fixtures.NOW.plusSeconds(90)
        );
    }

    private void arrangeSelection(LlmTurn first, QueryBatch batch) throws Exception {
        given(llm.begin(any(), any(), any(), any(), eq(true))).willReturn(first);
        given(queries.execute(first.selection(), Fixtures.incident(), Fixtures.evidence(), Fixtures.NOW.plusSeconds(90)))
                .willReturn(batch);
    }

    private LlmTurn selected() {
        return new LlmTurn(
                mapper.createArrayNode(),
                mapper.createArrayNode(),
                null,
                new QuerySelection(List.of(QueryName.DB_POOL_WAIT_PEAK)),
                "call_1",
                100,
                50
        );
    }

    private LlmTurn directReport() {
        return new LlmTurn(
                mapper.createArrayNode(),
                mapper.createArrayNode(),
                Fixtures.result(),
                null,
                "",
                100,
                50
        );
    }

    private QueryBatch batch() {
        return new QueryBatch(
                Fixtures.evidence(),
                List.of(new QueryExecution(
                        "db_pool_wait_peak",
                        "prometheus",
                        "DB 연결 대기 최댓값",
                        QueryName.DB_POOL_WAIT_PEAK.expression("dev"),
                        Fixtures.incident().startsAt().minusSeconds(900),
                        Fixtures.incident().startsAt(),
                        QueryStatus.NO_DATA,
                        false,
                        "",
                        List.of("사용할 수 있는 값이 없습니다")
                ))
        );
    }
}
