package com.chaekchaek.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.chaekchaek.incident.common.JsonHttpClient;
import com.chaekchaek.incident.common.RemoteCallException;
import com.chaekchaek.incident.evidence.QueryExecutor;
import com.chaekchaek.incident.evidence.QueryName;
import com.chaekchaek.incident.evidence.client.LokiClient;
import com.chaekchaek.incident.evidence.client.PrometheusClient;
import com.chaekchaek.incident.evidence.dto.QueryExecution;
import com.chaekchaek.incident.evidence.dto.QuerySelection;
import com.chaekchaek.incident.evidence.dto.QueryStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class QueryExecutorTest {
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final Clock clock = Clock.fixed(Fixtures.NOW, ZoneOffset.UTC);
    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    @Mock
    private PrometheusClient prometheus;
    @Mock
    private LokiClient loki;

    @Test
    @DisplayName("여러 pool의 결과를 보존하고 허용하지 않은 레이블은 전달하지 않는다")
    void should_KeepMultipleSafeRows_When_QueryReturnsSeveralSeries() {
        // given
        given(prometheus.query(anyString(), any())).willReturn(mapper.readTree("""
                [{"metric":{"pool":"HikariPool-1","memberId":"secret"},"value":[1,"0"]},
                 {"metric":{"pool":"HikariPool-2","Authorization":"secret"},"value":[1,"3"]}]
                """));
        // when
        var result = executor().execute(selection(QueryName.DB_POOL_WAIT_PEAK), Fixtures.incident(), Fixtures.evidence(), Fixtures.NOW.plusSeconds(90));
        // then
        assertThat(result.evidence().items()).hasSize(2);
        assertThat(result.evidence().items().get(1).values()).containsEntry("row1", "pool=HikariPool-1, value=0");
        assertThat(result.evidence().items().get(1).values()).containsEntry("row2", "pool=HikariPool-2, value=3");
        assertThat(result.evidence().toString()).doesNotContain("secret", "memberId", "Authorization");
        assertThat(result.executions().getFirst().evidenceId()).isEqualTo("E2");
        assertThat(result.evidence().from()).isEqualTo(Fixtures.incident().startsAt().minusSeconds(900));
    }

    @Test
    @DisplayName("상위 다섯 결과만 포함하고 잘림을 명시한다")
    void should_ReportTruncation_When_MoreThanFiveRowsExist() {
        // given
        var rows = mapper.createArrayNode();
        for (int index = 0; index < 6; index++) {
            rows.add(mapper.readTree("{\"metric\":{\"pool\":\"pool" + index + "\"},\"value\":[1,\"1\"]}"));
        }
        given(prometheus.query(anyString(), any())).willReturn(rows);
        // when
        var result = executor().execute(selection(QueryName.DB_POOL_WAIT_PEAK), Fixtures.incident(), Fixtures.evidence(), Fixtures.NOW.plusSeconds(90));
        // then
        assertThat(result.executions().getFirst().truncated()).isTrue();
        assertThat(result.evidence().items().get(1).values()).containsKeys("row1", "row5").doesNotContainKey("row6");
        assertThat(result.evidence().missingEvidence()).anyMatch(note -> note.contains("상위 5개"));
    }

    @Test
    @DisplayName("0은 관측값으로 보존하고 NaN과 빈 vector는 자료 없음으로 구분한다")
    void should_DistinguishZeroFromMissing_When_ValuesAreEmptyOrNonfinite() {
        // given
        given(prometheus.query(anyString(), any())).willReturn(
                mapper.readTree("[{\"metric\":{\"pool\":\"HikariPool-1\"},\"value\":[1,\"0\"]}]"),
                mapper.readTree("[{\"metric\":{},\"value\":[1,\"NaN\"]}]")
        );
        given(loki.query(anyString(), any())).willReturn(mapper.createArrayNode());
        // when
        var result = executor().execute(selection(QueryName.DB_POOL_WAIT_PEAK, QueryName.HTTP_LATENCY_P95, QueryName.ERROR_TYPE_COUNTS), Fixtures.incident(), Fixtures.evidence(), Fixtures.NOW.plusSeconds(90));
        // then
        assertThat(result.executions()).extracting(item -> item.status()).containsExactly(QueryStatus.SUCCESS, QueryStatus.NO_DATA, QueryStatus.NO_DATA);
        assertThat(result.evidence().items()).hasSize(2);
    }

    @Test
    @DisplayName("외부 API 타임아웃에는 응답 상태가 없어도 실패 유형을 보존한다")
    void should_KeepTimeoutEvidence_When_UpstreamStatusIsAbsent() {
        // given
        given(loki.query(anyString(), any())).willReturn(mapper.readTree("""
                [{"metric":{"upstreamService":"YES24","failureKind":"TIMEOUT"},"value":[1,"2"]}]
                """));
        // when
        var result = executor().execute(selection(QueryName.EXTERNAL_API_FAILURE_COUNTS), Fixtures.incident(), Fixtures.evidence(), Fixtures.NOW.plusSeconds(90));
        // then
        assertThat(result.executions().getFirst().status()).isEqualTo(QueryStatus.SUCCESS);
        assertThat(result.evidence().items().get(1).values().get("row1")).isEqualTo("upstreamService=YES24, failureKind=TIMEOUT, value=2");
    }

    @Test
    @DisplayName("위험한 레이블을 제외하고 제한을 기록한다")
    void should_ExcludeUnsafeRows_When_LabelsFailValidation() {
        // given
        given(loki.query(anyString(), any())).willReturn(mapper.readTree("""
                [{"metric":{"errorCode":"secret token","exception":"Oops"},"value":[1,"2"]},
                 {"metric":{"errorCode":"INTERNAL_SERVER_ERROR","exception":"Exception"},"value":[1,"5"]}]
                """));
        // when
        var result = executor().execute(selection(QueryName.ERROR_TYPE_COUNTS), Fixtures.incident(), Fixtures.evidence(), Fixtures.NOW.plusSeconds(90));
        // then
        assertThat(result.executions().getFirst().status()).isEqualTo(QueryStatus.SUCCESS);
        assertThat(result.evidence().items().get(1).values()).doesNotContainKey("row1").containsKey("row2");
        assertThat(result.evidence().toString()).doesNotContain("secret token");
        assertThat(result.executions().getFirst().limitations()).isNotEmpty();
    }

    @Test
    @DisplayName("최종 답변에 필요한 시간이 부족하면 추가 조회를 실행하지 않는다")
    void should_SkipQueries_When_DeadlineIsNear() {
        // given
        var selection = selection(QueryName.DB_POOL_WAIT_PEAK, QueryName.ERROR_TYPE_COUNTS);
        // when
        var result = executor().execute(selection, Fixtures.incident(), Fixtures.evidence(), Fixtures.NOW.plusSeconds(39));
        // then
        verifyNoInteractions(prometheus, loki);
        assertThat(result.executions()).extracting(item -> item.status()).containsOnly(QueryStatus.SKIPPED_DEADLINE);
        assertThat(result.evidence().items()).isEqualTo(Fixtures.evidence().items());
    }

    @Test
    @DisplayName("한 조회가 실패해도 다른 근거를 보존한다")
    void should_KeepOtherEvidence_When_OneQueryFails() {
        // given
        given(prometheus.query(anyString(), any())).willThrow(new RemoteCallException("HTTP_429"));
        given(loki.query(anyString(), any())).willReturn(mapper.readTree("[{\"metric\":{\"errorCode\":\"INTERNAL_SERVER_ERROR\"},\"value\":[1,\"5\"]}]"));
        // when
        var result = executor().execute(selection(QueryName.DB_POOL_WAIT_PEAK, QueryName.ERROR_TYPE_COUNTS), Fixtures.incident(), Fixtures.evidence(), Fixtures.NOW.plusSeconds(90));
        // then
        assertThat(result.executions()).extracting(item -> item.status()).containsExactly(QueryStatus.FAILED, QueryStatus.SUCCESS);
        assertThat(result.evidence().items()).hasSize(2);
        assertThat(meters.get("incident_query").tag("query", "db_pool_wait_peak").tag("outcome", "FAILED").counter().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("중복 조회나 허용하지 않은 환경과 조회 이름을 거절한다")
    void should_RejectInvalidSelection_When_RequestExceedsCatalog() {
        // when & then
        assertThatThrownBy(() -> selection(QueryName.DB_POOL_WAIT_PEAK, QueryName.DB_POOL_WAIT_PEAK)).isInstanceOf(RemoteCallException.class);
        assertThatThrownBy(() -> new QuerySelection(List.of())).isInstanceOf(RemoteCallException.class);
        assertThatThrownBy(() -> QueryName.fromId("arbitrary_sql")).isInstanceOf(RemoteCallException.class);
        assertThatThrownBy(() -> QueryName.DB_POOL_WAIT_PEAK.expression("dev\"} or up")).isInstanceOf(RemoteCallException.class);
    }

    @Test
    @DisplayName("네 가지 조회를 각 데이터 소스에 발생 시각과 고정된 환경으로 한 번씩 전송한다")
    void should_SendBoundedQueries_When_AllToolsAreSelected() throws Exception {
        // given
        try (var server = new MockWebServer()) {
            server.start();
            for (int index = 0; index < 4; index++) {
                server.enqueue(new MockResponse().setBody("{\"status\":\"success\",\"data\":{\"result\":[]}}"));
            }
            var properties = Fixtures.properties(server.url("/").toString());
            var http = new JsonHttpClient(mapper);
            var executor = new QueryExecutor(new PrometheusClient(properties, http), new LokiClient(properties, http), clock, meters);
            // when
            executor.execute(selection(QueryName.values()), Fixtures.incident(), Fixtures.evidence(), Fixtures.NOW.plusSeconds(90));
            // then
            for (int index = 0; index < 4; index++) {
                var request = server.takeRequest();
                assertThat(request.getRequestUrl().queryParameter("time")).isEqualTo(String.valueOf(Fixtures.incident().startsAt().getEpochSecond()));
                assertThat(request.getRequestUrl().queryParameter("query")).contains("env=\"dev\"", "[15m]");
                assertThat(request.getRequestUrl().encodedPath()).isEqualTo(index < 2 ? "/api/v1/query" : "/loki/api/v1/query");
            }
            assertThat(server.getRequestCount()).isEqualTo(4);
        }
    }

    @ParameterizedTest
    @EnumSource(value = QueryStatus.class, names = {"NO_DATA", "FAILED", "SKIPPED_DEADLINE"})
    @DisplayName("관측이 없는 조회에 다른 측정의 근거 ID를 연결할 수 없다")
    void should_RejectEvidenceReference_When_QueryHasNoObservation(QueryStatus status) {
        // when, then
        assertThatThrownBy(() -> new QueryExecution(
                "http_latency_p95",
                "prometheus",
                QueryName.HTTP_LATENCY_P95.getDescription(),
                QueryName.HTTP_LATENCY_P95.expression("dev"),
                Fixtures.incident().startsAt().minusSeconds(900),
                Fixtures.incident().startsAt(),
                status,
                false,
                "E1",
                List.of()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("UNOBSERVED_QUERY_CANNOT_REFERENCE_EVIDENCE");
    }

    private QueryExecutor executor() {
        return new QueryExecutor(prometheus, loki, clock, meters);
    }

    private QuerySelection selection(QueryName... queries) {
        return new QuerySelection(Arrays.asList(queries));
    }
}
