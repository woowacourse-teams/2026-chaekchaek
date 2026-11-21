package com.chaekchaek.incident.evidence;

import com.chaekchaek.incident.common.RemoteCallException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class ErrorRequestDetails {
    private static final int MAX_SAMPLES = 200;
    private static final int MAX_ROWS = 5;
    private static final int MAX_PATH_LENGTH = 100;
    private static final int MAX_ROW_LENGTH = 450;
    private static final String PATH_TRUNCATION_NOTE = "정제된 경로의 길이를 제한했습니다";
    private static final Set<String> METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "TRACE", "CONNECT");
    private static final Set<String> STATIC_SEGMENTS = Set.of(
            "api",
            "v1",
            "v2",
            "oauth2",
            "authorization",
            "google",
            "auth",
            "login",
            "logout",
            "refresh",
            "members",
            "me",
            "books",
            "reviews",
            "libraries",
            "shelves",
            "reactions",
            "comments",
            "search",
            "recommendations",
            "nickname",
            "profile",
            "surveys",
            "health",
            "actuator",
            "wp-includes",
            "wlwmanifest.xml",
            "xmlrpc.php",
            "wp-admin",
            "wp-login.php",
            "robots.txt",
            "favicon.ico"
    );
    private static final Set<String> EXCEPTIONS = Set.of(
            "NoResourceFoundException",
            "NullPointerException",
            "IllegalArgumentException",
            "IllegalStateException",
            "SocketTimeoutException",
            "ConnectException",
            "SocketException",
            "IOException",
            "Yes24ClientException",
            "KakaoClientException",
            "BookClientException",
            "DataAccessException",
            "SQLTransientConnectionException"
    );
    private static final Set<String> FIXED_MESSAGES = Set.of("예상하지 못한 서버 오류", "비즈니스 예외", "도서 API 호출 실패");
    private static final Pattern MISSING_RESOURCE = Pattern.compile(
            "^org\\.springframework\\.web\\.servlet\\.resource\\.NoResourceFoundException: No static resource (.{1,2048}) for request '(.{1,2048})'\\.$"
    );
    private static final Pattern TECHNICAL_MESSAGE = Pattern.compile(
            "^java\\.(?:net|io)\\.(?:ConnectException|SocketTimeoutException|SocketException|IOException): (Connection refused|Read timed out|Broken pipe)$"
    );
    private final JsonMapper mapper;

    public Samples extract(JsonNode streams, Instant from, Instant to) {
        if (!streams.isArray()) {
            throw new RemoteCallException("INVALID_LOKI_RESPONSE");
        }
        Map<Map<String, String>, List<Instant>> groups = new LinkedHashMap<>();
        var notes = new ArrayList<String>();
        var entries = streams.valueStream()
                .flatMap(this::entries)
                .limit(MAX_SAMPLES)
                .toList();
        int excluded = 0;
        for (JsonNode entry : entries) {
            if (!add(entry, from, to, groups, notes)) {
                excluded++;
            }
        }
        return samples(groups, entries.size(), excluded, notes);
    }

    private Stream<JsonNode> entries(JsonNode stream) {
        if (!stream.isObject() || !stream.path("values").isArray()) {
            throw new RemoteCallException("INVALID_LOKI_RESPONSE");
        }
        return stream.path("values").valueStream();
    }

    private boolean add(
            JsonNode entry,
            Instant from,
            Instant to,
            Map<Map<String, String>, List<Instant>> groups,
            List<String> notes
    ) {
        try {
            if (!entry.isArray() || entry.size() != 2 || !entry.path(0).isString() || !entry.path(1).isString()) {
                return false;
            }
            Instant observedAt = timestamp(entry.path(0).asText());
            if (observedAt.isBefore(from) || observedAt.isAfter(to)) {
                return false;
            }
            JsonNode log = mapper.readTree(entry.path(1).asText());
            Map<String, String> fields = fields(log, notes);
            if (fields.isEmpty()) {
                return false;
            }
            groups.computeIfAbsent(fields, ignored -> new ArrayList<>()).add(observedAt);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private Instant timestamp(String value) {
        if (!value.matches("[0-9]{1,19}")) {
            throw new IllegalArgumentException("INVALID_LOG_TIMESTAMP");
        }
        long nanos = Long.parseLong(value);
        return Instant.ofEpochSecond(nanos / 1_000_000_000, nanos % 1_000_000_000);
    }

    private Map<String, String> fields(JsonNode log, List<String> notes) {
        if (log == null || !log.isObject() || !"error".equals(log.path("type").asText())
                || !log.path("status").isIntegralNumber() || !log.path("status").asString().matches("5[0-9]{2}")
                || !METHODS.contains(log.path("method").asText())) {
            return Map.of();
        }
        String path = safePath(log.path("path").asText(""));
        if (path == null) {
            return Map.of();
        }
        var fields = new LinkedHashMap<String, String>();
        fields.put("method", log.path("method").asText());
        fields.put("path", path);
        if (path.contains("{truncated}")) {
            addNote(notes, PATH_TRUNCATION_NOTE);
        }
        fields.put("status", log.path("status").asString());
        String exception = log.path("exception").asText("");
        if (EXCEPTIONS.contains(exception)) {
            fields.put("exception", exception);
        } else {
            addNote(notes, "허용하지 않은 예외 종류는 제외했습니다");
        }
        String summary = messageSummary(log);
        if (!summary.isEmpty()) {
            fields.put("exceptionMessageSummary", summary);
            if (summary.contains("{truncated}")) {
                addNote(notes, PATH_TRUNCATION_NOTE);
            }
        } else {
            addNote(notes, "인식하지 못한 예외 메시지는 제외했습니다");
        }
        return fields;
    }

    private String messageSummary(JsonNode log) {
        String stack = log.path("stack_trace").asText("");
        String firstLine = stack.substring(0, Math.min(stack.length(), 4096)).split("\\R", 2)[0];
        var resource = MISSING_RESOURCE.matcher(firstLine);
        if ("NoResourceFoundException".equals(log.path("exception").asText()) && resource.matches()) {
            String path = safePath("/" + resource.group(1));
            if (path != null && safePath(resource.group(2)) != null) {
                return "No static resource " + path;
            }
        }
        var technical = TECHNICAL_MESSAGE.matcher(firstLine);
        if (technical.matches()) {
            return technical.group(1);
        }
        String message = log.path("message").asText("");
        if (FIXED_MESSAGES.contains(message)) {
            return "applicationMessage=" + message;
        }
        return "";
    }

    private String safePath(String raw) {
        if (!raw.startsWith("/") || raw.startsWith("//") || raw.length() > 4096) {
            return null;
        }
        String path = raw.split("[?#;]", 2)[0];
        if (path.chars().anyMatch(character -> Character.isISOControl(character) || character == '\\')) {
            return null;
        }
        var safe = new ArrayList<String>();
        for (String segment : path.substring(1).split("/", -1)) {
            String value = "{redacted}";
            if (segment.isEmpty() || STATIC_SEGMENTS.contains(segment)) {
                value = segment;
            }
            safe.add(value);
        }
        String result = "/" + String.join("/", safe);
        if (result.length() > MAX_PATH_LENGTH) {
            return result.substring(0, MAX_PATH_LENGTH - 12) + "/{truncated}";
        }
        return result;
    }

    private Samples samples(
            Map<Map<String, String>, List<Instant>> groups,
            int fetched,
            int excluded,
            List<String> notes
    ) {
        var rows = new LinkedHashMap<String, String>();
        boolean truncated = groups.size() > MAX_ROWS || fetched >= MAX_SAMPLES || notes.contains(PATH_TRUNCATION_NOTE);
        for (var group : groups.entrySet()) {
            if (rows.size() >= MAX_ROWS) {
                break;
            }
            String row = row(group.getKey(), group.getValue());
            if (row.length() > MAX_ROW_LENGTH) {
                row = row.substring(0, MAX_ROW_LENGTH - 3) + "...";
                truncated = true;
                addNote(notes, "정제된 대표 행의 길이를 제한했습니다");
            }
            rows.put("row" + (rows.size() + 1), row);
        }
        if (!rows.isEmpty()) {
            rows.put("fetchedSamples", String.valueOf(fetched));
            rows.put("validSamples", String.valueOf(fetched - excluded));
            rows.put("excludedSamples", String.valueOf(excluded));
        }
        if (excluded > 0) {
            notes.add("형식, 상태 또는 관측 시각이 맞지 않는 표본 " + excluded + "건을 제외했습니다");
        }
        if (groups.size() > MAX_ROWS) {
            notes.add("정제 후 요청 유형 중 대표 5개만 포함했습니다");
        }
        if (fetched >= MAX_SAMPLES) {
            notes.add("최대 200건 표본 한도에 도달해 이전 로그가 생략됐을 수 있습니다");
        }
        notes.add("표본 내 건수이며 전체 오류 건수나 원본 경로별 집계가 아닙니다");
        notes.add("경로는 허용한 정적 세그먼트만 남기며 메시지는 인식한 형식의 정제 요약만 제공합니다");
        return new Samples(
                rows,
                truncated,
                notes
        );
    }

    private String row(Map<String, String> fields, List<Instant> times) {
        var parts = new ArrayList<String>();
        fields.forEach((key, value) -> parts.add(key + "=" + value));
        Instant first = times.stream()
                .min(Instant::compareTo)
                .orElseThrow();
        Instant last = times.stream()
                .max(Instant::compareTo)
                .orElseThrow();
        parts.add("sampleCount=" + times.size());
        parts.add("firstSeenAt=" + first);
        parts.add("lastSeenAt=" + last);
        return String.join(", ", parts);
    }

    private void addNote(List<String> notes, String note) {
        if (!notes.contains(note)) {
            notes.add(note);
        }
    }

    public record Samples(
            Map<String, String> rows,
            boolean truncated,
            List<String> limitations
    ) {
        public Samples {
            rows = Map.copyOf(rows);
            limitations = List.copyOf(limitations);
        }
    }
}
