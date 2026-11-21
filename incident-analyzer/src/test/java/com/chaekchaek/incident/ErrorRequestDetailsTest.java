package com.chaekchaek.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaekchaek.incident.common.RemoteCallException;
import com.chaekchaek.incident.evidence.ErrorRequestDetails;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class ErrorRequestDetailsTest {
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final ErrorRequestDetails extractor = new ErrorRequestDetails(mapper);
    private final Instant from = Fixtures.incident().startsAt().minusSeconds(900);
    private final Instant to = Fixtures.incident().startsAt();

    @Test
    @DisplayName("실제 장애 패턴에서 정적 리소스 단서는 남기고 모든 위치의 식별자는 가린다")
    void should_KeepResourceCluesWithoutIdentifiers_When_IncidentContainsPrivatePaths() {
        // given
        String path = "/oauth2/authorization/google/private-name/wp-includes/wlwmanifest.xml?token=secret-query";
        ObjectNode log = log(path);
        log.put("message", "private-name secret-message");
        log.put("headers", "Authorization: secret-header");
        log.put("body", "private-body");
        log.put("memberId", "private-member");
        log.put("requestId", "private-request");
        log.put("stack_trace", stack(path) + "\n at com.chaekchaek.private-name.Filter(secret-frame:42)");
        // when
        var result = extract(stream(log));
        // then
        assertThat(result.rows().get("row1")).contains(
                "method=GET",
                "path=/oauth2/authorization/google/{redacted}/wp-includes/wlwmanifest.xml",
                "exception=NoResourceFoundException",
                "exceptionMessageSummary=No static resource",
                "firstSeenAt=" + to.minusSeconds(1),
                "sampleCount=1"
        );
        assertThat(result.toString()).doesNotContain("private-name", "secret-", "token=", "Authorization", "private-body", "private-member", "private-request", "secret-frame");
        assertThat(result.rows()).containsEntry("fetchedSamples", "1").containsEntry("validSamples", "1");
        assertThat(result.limitations()).anyMatch(note -> note.contains("원본 경로별 집계가 아닙니다"));
    }

    @Test
    @DisplayName("여러 stream에서 같은 정제 경로가 된 표본은 건수와 실제 관측 범위를 함께 남긴다")
    void should_GroupSanitizedPathsAcrossStreams_When_OriginalNamesDiffer() {
        // given
        var streams = mapper.createArrayNode();
        streams.add(stream(log("/books/private-one"), to.minusSeconds(5)));
        streams.add(stream(log("/books/private-two"), to.minusSeconds(1)));
        // when
        var result = extract(streams);
        // then
        assertThat(result.rows()).doesNotContainKey("row2");
        assertThat(result.rows().get("row1")).contains("sampleCount=2", "firstSeenAt=" + to.minusSeconds(5), "lastSeenAt=" + to.minusSeconds(1));
        assertThat(result.rows()).containsEntry("validSamples", "2");
        assertThat(result.toString()).doesNotContain("private-one", "private-two");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/books/123", "/books/bb3d27c8-2ea9-4d9b-a33a-f5171e76ff72", "/members/me@example.com", "/api/%73ecret", "/api/randomusername", "/api/ABC-secret#private-fragment", "/api/private;session=secret"})
    @DisplayName("숫자와 UUID와 이메일과 인코딩과 임의 세그먼트를 정적 경로로 노출하지 않는다")
    void should_RedactUnknownSegments_When_PathContainsIdentifiers(String path) {
        // given, when
        var result = extract(stream(log(path)));
        // then
        assertThat(result.rows().get("row1")).contains("{redacted}");
        assertThat(result.toString()).doesNotContain("123", "bb3d27c8", "me@example.com", "%73ecret", "randomusername", "ABC-secret", "private-fragment", "session=secret");
    }

    @Test
    @DisplayName("일부 표본이 잘못돼도 정상 요청 상세를 보존하고 제외 건수를 남긴다")
    void should_KeepValidSamples_When_OtherEntriesAreMalformed() {
        // given
        var entries = mapper.createArrayNode();
        entries.add(entry(log("/xmlrpc.php"), to.minusSeconds(1)));
        entries.add(mapper.valueToTree(List.of(nanos(to), "not-json")));
        entries.add(entry(log("/books/1"), from.minusNanos(1)));
        entries.add(entry(log("/books/1").put("status", 404), to));
        entries.add(entry(log("/books/1").put("status", "500"), to));
        entries.add(entry(log("/books/1").put("method", "private-method"), to));
        entries.add(entry(log("/api/private\\path"), to));
        entries.add(mapper.valueToTree(List.of("not-a-timestamp", "{}")));
        // when
        var result = extract(mapper.valueToTree(List.of(Map.of("values", entries))));
        // then
        assertThat(result.rows().get("row1")).contains("/xmlrpc.php");
        assertThat(result.rows()).containsEntry("fetchedSamples", "8").containsEntry("validSamples", "1").containsEntry("excludedSamples", "7");
        assertThat(result.limitations()).anyMatch(note -> note.contains("7건을 제외"));
        assertThat(result.toString()).doesNotContain("private-method", "private\\path", "not-json");
    }

    @Test
    @DisplayName("모르는 메시지와 예외 종류는 제외하되 경로만 있는 표본도 보존한다")
    void should_KeepPathAndLimitations_When_ExceptionIsUnknownOrMissing() {
        // given
        ObjectNode unknown = log("/api/books").put("exception", "private-exception").put("message", "private-message").put("stack_trace", "private-stack");
        ObjectNode absent = log("/api/reviews");
        absent.remove(List.of("exception", "message", "stack_trace"));
        // when
        var result = extract(mapper.valueToTree(List.of(stream(unknown), stream(absent))));
        // then
        assertThat(result.rows()).containsKeys("row1", "row2");
        assertThat(result.toString()).doesNotContain("private-exception", "private-message", "private-stack", "exceptionMessageSummary=");
        assertThat(result.limitations()).anyMatch(note -> note.contains("메시지는 제외"));
    }

    @Test
    @DisplayName("정확히 인식한 기술 메시지와 일반 애플리케이션 문구만 요약으로 남긴다")
    void should_AllowFixedMessages_When_StackFormatIsRecognized() {
        // given
        ObjectNode technical = log("/api/books").put("exception", "ConnectException").put("stack_trace", "java.net.ConnectException: Connection refused\nprivate-stack");
        ObjectNode general = log("/api/reviews").put("stack_trace", "unrecognized private-stack").put("message", "예상하지 못한 서버 오류");
        // when
        var result = extract(mapper.valueToTree(List.of(stream(technical), stream(general))));
        // then
        assertThat(result.toString()).contains("exceptionMessageSummary=Connection refused", "exceptionMessageSummary=applicationMessage=예상하지 못한 서버 오류");
        assertThat(result.toString()).doesNotContain("private-stack");
    }

    @Test
    @DisplayName("표본과 유형과 출력 길이 제한을 지키고 생략 가능성을 알린다")
    void should_BoundSamplesAndRows_When_ResponseExceedsLimits() {
        // given
        var entries = mapper.createArrayNode();
        for (int index = 0; index < 205; index++) {
            entries.add(entry(log("/" + "api/".repeat(index % 7 + 1) + "books/".repeat(40)), to));
        }
        // when
        var result = extract(mapper.valueToTree(List.of(Map.of("values", entries))));
        // then
        assertThat(result.truncated()).isTrue();
        assertThat(result.rows()).containsEntry("fetchedSamples", "200").containsKey("row5").doesNotContainKey("row6");
        assertThat(result.toString()).contains("{truncated}");
        assertThat(result.limitations()).anyMatch(note -> note.contains("200건 표본 한도"));
        assertThat(result.limitations()).anyMatch(note -> note.contains("대표 5개"));
        assertThat(result.rows().values()).allMatch(value -> value.length() <= 450);
        assertThat(result.rows().toString().length()).isLessThan(2500);
    }

    @Test
    @DisplayName("정상 빈 결과와 잘못된 응답 구조를 구분한다")
    void should_RejectMalformedEnvelope_When_EmptyStreamsAreValid() {
        // given, when, then
        assertThat(extract(mapper.createArrayNode()).rows()).isEmpty();
        assertThat(extract(stream(log("invalid-path"))).rows()).isEmpty();
        assertThatThrownBy(() -> extract(mapper.readTree("{}"))).isInstanceOf(RemoteCallException.class);
        assertThatThrownBy(() -> extract(mapper.readTree("[{\"values\":{}}]"))).isInstanceOf(RemoteCallException.class);
    }

    private ErrorRequestDetails.Samples extract(JsonNode value) {
        if (value.isObject()) {
            return extractor.extract(mapper.valueToTree(List.of(value)), from, to);
        }
        return extractor.extract(value, from, to);
    }

    private ObjectNode log(String path) {
        return mapper.valueToTree(Map.of(
                "type", "error",
                "status", 500,
                "method", "GET",
                "path", path,
                "exception", "NoResourceFoundException",
                "stack_trace", stack(path)
        ));
    }

    private String stack(String path) {
        return "org.springframework.web.servlet.resource.NoResourceFoundException: No static resource "
                + path.substring(Math.min(1, path.length())) + " for request '" + path + "'.";
    }

    private JsonNode stream(ObjectNode log) {
        return stream(log, to.minusSeconds(1));
    }

    private JsonNode stream(ObjectNode log, Instant time) {
        return mapper.valueToTree(Map.of("values", List.of(entry(log, time))));
    }

    private JsonNode entry(ObjectNode log, Instant time) {
        return mapper.valueToTree(List.of(nanos(time), mapper.writeValueAsString(log)));
    }

    private String nanos(Instant time) {
        return String.valueOf(time.getEpochSecond() * 1_000_000_000 + time.getNano());
    }
}
