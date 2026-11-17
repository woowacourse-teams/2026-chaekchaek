package com.chaekchaek.incident.evidence;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class SafeLogExtractor {
    private static final Pattern CODE = Pattern.compile("[A-Za-z0-9_.$-]{1,100}");
    private static final Pattern CAUSE = Pattern.compile("Caused by: ((?:java|org\\.springframework|com\\.mysql|com\\.zaxxer|com\\.chaekchaek)[A-Za-z0-9_.$]*(?:Exception|Error))");
    private static final Pattern FRAME = Pattern.compile("at (com\\.chaekchaek\\.[A-Za-z0-9_.$]+)\\(");

    public Map<String, String> extract(JsonNode log) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (String key : new String[]{"type", "errorCode", "exception", "upstreamService", "failureKind"}) {
            putCode(fields, key, log.path(key).asText(""));
        }
        putNumber(fields, "status", log.path("status"), 100, 599);
        putNumber(fields, "upstreamStatus", log.path("upstreamStatus"), 100, 599);
        putNumber(fields, "durationMs", log.path("durationMs"), 0, 3_600_000);
        String version = log.path("version").asText("");
        if (version.matches("[a-f0-9]{7,40}")) {
            fields.put("version", version);
        }
        putStack(fields, log.path("stack_trace").asText(""));
        return fields;
    }

    private void putCode(Map<String, String> fields, String key, String value) {
        if (CODE.matcher(value).matches()) {
            fields.put(key, value);
        }
    }

    private void putNumber(Map<String, String> fields, String key, JsonNode node, long min, long max) {
        if (node.isIntegralNumber() && node.asLong() >= min && node.asLong() <= max) {
            fields.put(key, node.asString());
        }
    }

    private void putStack(Map<String, String> fields, String stack) {
        String bounded = stack.substring(0, Math.min(stack.length(), 12000));
        var causes = CAUSE.matcher(bounded);
        if (causes.find()) {
            fields.put("causeType", causes.group(1));
        }
        var frames = FRAME.matcher(bounded);
        if (frames.find()) {
            fields.put("applicationFrame", frames.group(1));
        }
    }
}
