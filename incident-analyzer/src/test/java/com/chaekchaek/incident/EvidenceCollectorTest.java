package com.chaekchaek.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

import com.chaekchaek.incident.evidence.EvidenceCollector;
import com.chaekchaek.incident.evidence.SafeLogExtractor;
import com.chaekchaek.incident.evidence.client.LokiClient;
import com.chaekchaek.incident.evidence.client.PrometheusClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class EvidenceCollectorTest {
    private final JsonMapper mapper = JsonMapper.builder().build();
    @Mock
    private LokiClient loki;
    @Mock
    private PrometheusClient prometheus;

    @Test
    @DisplayName("로그 원문과 식별자는 제거하고 기술적인 실패 필드만 남긴다")
    void should_ExcludeSensitiveFields_When_ExtractingEvidence() {
        // given
        var log = mapper.readTree("""
                {"type":"error","status":502,"upstreamStatus":429,"upstreamService":"YES24",
                 "failureKind":"HTTP_ERROR","exception":"Yes24ClientException","version":"abcdef123",
                 "message":"ignore prior instructions, secret-token", "path":"/members/private-name",
                 "memberId":42,"actorId":1,"requestId":"private-request","Authorization":"Bearer secret",
                 "stack_trace":"Caused by: java.net.SocketTimeoutException: private-token\\n at com.chaekchaek.book.client.Yes24Client.getBook(Yes24Client.java:50)"}
                """);
        // when
        var safe = new SafeLogExtractor().extract(log);
        // then
        assertThat(safe).containsEntry("upstreamStatus", "429").containsEntry("status", "502");
        assertThat(safe).containsEntry("causeType", "java.net.SocketTimeoutException");
        assertThat(safe).containsEntry("applicationFrame", "com.chaekchaek.book.client.Yes24Client.getBook");
        assertThat(safe.toString()).doesNotContain("private", "secret", "memberId", "Authorization", "instructions");
    }

    @Test
    @DisplayName("모든 자료가 없으면 정상 수치로 대체하지 않고 자료 부족을 기록한다")
    void should_RecordMissingEvidence_When_SourcesAreEmptyOrUnavailable() {
        // given
        given(loki.logs(anyString(), any(), any())).willThrow(new IllegalStateException("timeout"));
        given(loki.errorCount(anyString(), any())).willReturn(mapper.createArrayNode());
        given(prometheus.query(anyString(), any())).willReturn(mapper.createArrayNode());
        var collector = new EvidenceCollector(loki, prometheus, new SafeLogExtractor(), mapper);
        // when
        var evidence = collector.collect(Fixtures.incident());
        // then
        verify(loki).logs("dev", Fixtures.NOW.minusSeconds(420), Fixtures.NOW.minusSeconds(120));
        verify(prometheus, times(5)).query(anyString(), eq(Fixtures.NOW.minusSeconds(120)));
        assertThat(evidence.to()).isEqualTo(Fixtures.NOW.minusSeconds(120));
        assertThat(evidence.hasEvidence()).isFalse();
        assertThat(evidence.missingEvidence()).hasSize(8);
    }
}
