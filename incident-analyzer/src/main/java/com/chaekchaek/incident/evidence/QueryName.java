package com.chaekchaek.incident.evidence;

import com.chaekchaek.incident.common.RemoteCallException;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QueryName {
    DB_POOL_WAIT_PEAK("db_pool_wait_peak", "prometheus", "DB 연결 대기 구간 최대값", "connections", List.of("pool")),
    HTTP_LATENCY_P95("http_latency_p95", "prometheus", "HTTP 요청 응답 시간 p95", "seconds", List.of()),
    ERROR_TYPE_COUNTS("error_type_counts", "loki", "오류 코드와 예외 종류별 전체 집계 상위 5개", "errors", List.of("errorCode", "exception")),
    EXTERNAL_API_FAILURE_COUNTS("external_api_failure_counts", "loki", "외부 API 실패 유형별 전체 집계 상위 5개", "errors", List.of("upstreamService", "upstreamStatus", "failureKind"));

    private final String id;
    private final String source;
    private final String description;
    private final String unit;
    private final List<String> labels;

    public String expression(String environment) {
        if (!Set.of("dev", "prod").contains(environment)) {
            throw new RemoteCallException("INVALID_QUERY_ENVIRONMENT");
        }
        String app = "{env=\"" + environment + "\",job=\"chaekchaek-backend\"}";
        String logs = "{app=\"chaekchaek\",env=\"" + environment + "\"}";
        return switch (this) {
            case DB_POOL_WAIT_PEAK -> "topk(6, max by (pool) (max_over_time(hikaricp_connections_pending" + app + "[15m])))";
            case HTTP_LATENCY_P95 -> "histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket" + app + "[15m])))";
            case ERROR_TYPE_COUNTS -> "topk(6, sum by (errorCode, exception) (count_over_time(" + logs
                    + " | json kind=\"type\", errorCode=\"errorCode\", exception=\"exception\", status=\"status\""
                    + " | kind=\"error\" | status >= 500 | status < 600 | errorCode != \"\" | __error__=\"\" [15m])))";
            case EXTERNAL_API_FAILURE_COUNTS -> "topk(6, sum by (upstreamService, upstreamStatus, failureKind) (count_over_time(" + logs
                    + " | json kind=\"type\", errorCode=\"errorCode\", status=\"status\", upstreamService=\"upstreamService\", upstreamStatus=\"upstreamStatus\", failureKind=\"failureKind\""
                    + " | kind=\"error\" | status >= 500 | status < 600 | errorCode=\"EXTERNAL_API_ERROR\""
                    + " | upstreamService != \"\" | failureKind != \"\" | __error__=\"\" [15m])))";
        };
    }

    public static QueryName fromId(String id) {
        for (QueryName query : values()) {
            if (query.id.equals(id)) {
                return query;
            }
        }
        throw new RemoteCallException("INVALID_TOOL_ARGUMENTS");
    }
}
