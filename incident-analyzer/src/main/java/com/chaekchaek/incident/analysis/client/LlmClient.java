package com.chaekchaek.incident.analysis.client;

import com.chaekchaek.incident.alert.dto.IncidentSnapshot;
import com.chaekchaek.incident.analysis.AnalysisValidator;
import com.chaekchaek.incident.analysis.dto.AnalysisReport;
import com.chaekchaek.incident.analysis.dto.AnalysisResult;
import com.chaekchaek.incident.analysis.dto.LlmTurn;
import com.chaekchaek.incident.common.JsonHttpClient;
import com.chaekchaek.incident.common.RemoteCallException;
import com.chaekchaek.incident.config.IncidentProperties;
import com.chaekchaek.incident.evidence.QueryName;
import com.chaekchaek.incident.evidence.dto.EvidenceBundle;
import com.chaekchaek.incident.evidence.dto.QueryBatch;
import com.chaekchaek.incident.evidence.dto.QuerySelection;
import com.chaekchaek.incident.question.dto.ConversationTurn;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@Component
public class LlmClient {
    private static final int MAX_INPUT_CHARACTERS = 20000;
    private static final String TOOL_NAME = "collect_incident_evidence";
    private static final String REPORT_TOOL_NAME = "submit_incident_report";
    private final IncidentProperties properties;
    private final JsonHttpClient http;
    private final JsonMapper mapper;
    private final AnalysisValidator validator;
    private final MeterRegistry meters;
    private final String instructions;
    private final String firstTurnInstructions;
    private final JsonNode schema;

    public LlmClient(
            IncidentProperties properties,
            JsonHttpClient http,
            JsonMapper mapper,
            AnalysisValidator validator,
            MeterRegistry meters
    ) throws IOException {
        this.properties = properties;
        this.http = http;
        this.mapper = mapper;
        this.validator = validator;
        this.meters = meters;
        this.instructions = new ClassPathResource("analysis/instructions.txt").getContentAsString(StandardCharsets.UTF_8);
        this.firstTurnInstructions = new ClassPathResource("analysis/first-turn-instructions.txt").getContentAsString(StandardCharsets.UTF_8);
        this.schema = mapper.readTree(new ClassPathResource("analysis/report-schema.json").getInputStream());
    }

    public AnalysisResult analyze(
            EvidenceBundle evidence,
            IncidentSnapshot incident,
            String question,
            List<ConversationTurn> conversation
    ) throws IOException {
        return begin(evidence, incident, question, conversation, false).report();
    }

    public LlmTurn begin(
            EvidenceBundle evidence,
            IncidentSnapshot incident,
            String question,
            List<ConversationTurn> conversation,
            boolean allowQueries
    ) throws IOException {
        String content = mapper.writeValueAsString(Map.of(
                "incident",
                incident,
                "evidence",
                evidence,
                "question",
                question,
                "conversation",
                conversation
        ));
        JsonNode input = mapper.valueToTree(List.of(Map.of("role", "user", "content", content)));
        var body = request(evidence, incident, input);
        if (allowQueries) {
            body.remove("text");
            body.put("instructions", body.get("instructions") + firstTurnInstructions);
            body.put("tools", List.of(tool(), reportTool(evidence)));
            body.put("tool_choice", "required");
            body.put("parallel_tool_calls", false);
        }
        Duration timeout = Duration.ofSeconds(30);
        if (allowQueries) {
            timeout = Duration.ofSeconds(15);
        }
        JsonNode response = send(body, timeout, "initial");
        return turn(response, input, evidence, allowQueries);
    }

    private LlmTurn turn(
            JsonNode response,
            JsonNode input,
            EvidenceBundle evidence,
            boolean allowQueries
    ) {
        requireCompleted(response);
        var calls = response.path("output")
                .valueStream()
                .filter(item -> "function_call".equals(item.path("type").asText()))
                .toList();
        long inputTokens = usage(response, "input_tokens");
        long outputTokens = usage(response, "output_tokens");
        if (allowQueries) {
            if (calls.size() != 1) {
                throw new RemoteCallException("UNEXPECTED_TOOL_CALL");
            }
            return selectedTurn(calls.getFirst(), response, input, evidence, inputTokens, outputTokens);
        }
        return new LlmTurn(
                input,
                response.path("output"),
                readResult(response, evidence, inputTokens, outputTokens),
                null,
                "",
                inputTokens,
                outputTokens
        );
    }

    private LlmTurn selectedTurn(
            JsonNode call,
            JsonNode response,
            JsonNode input,
            EvidenceBundle evidence,
            long inputTokens,
            long outputTokens
    ) {
        String name = call.path("name").asText("");
        if (!List.of(TOOL_NAME, REPORT_TOOL_NAME).contains(name)
                || !call.path("call_id").isString()
                || !call.path("call_id").asText().matches("[A-Za-z0-9_-]{1,200}")
                || !call.path("arguments").isString()) {
            throw new RemoteCallException("INVALID_TOOL_ARGUMENTS");
        }
        AnalysisResult report = null;
        QuerySelection selection = null;
        if (REPORT_TOOL_NAME.equals(name)) {
            report = report(call.path("arguments").asText(), evidence, inputTokens, outputTokens);
        } else {
            selection = selection(call);
        }
        return new LlmTurn(
                input,
                response.path("output"),
                report,
                selection,
                call.path("call_id").asText(),
                inputTokens,
                outputTokens
        );
    }

    private QuerySelection selection(JsonNode call) {
        String arguments = call.path("arguments").asText("");
        if (arguments.length() > 2000) {
            throw new RemoteCallException("INVALID_TOOL_ARGUMENTS");
        }
        JsonNode parsed;
        try {
            parsed = mapper.readTree(arguments);
        } catch (RuntimeException exception) {
            throw new RemoteCallException("INVALID_TOOL_ARGUMENTS");
        }
        if (parsed == null || !parsed.isObject() || parsed.size() != 1 || !parsed.path("queries").isArray()) {
            throw new RemoteCallException("INVALID_TOOL_ARGUMENTS");
        }
        var queries = new ArrayList<QueryName>();
        for (JsonNode query : parsed.path("queries")) {
            if (!query.isString()) {
                throw new RemoteCallException("INVALID_TOOL_ARGUMENTS");
            }
            queries.add(QueryName.fromId(query.asText()));
        }
        return new QuerySelection(queries);
    }

    public boolean continuationFits(LlmTurn first, QueryBatch batch) {
        return mapper.writeValueAsString(continuation(first, batch)).length() <= MAX_INPUT_CHARACTERS;
    }

    private JsonNode continuation(LlmTurn first, QueryBatch batch) {
        var input = mapper.createArrayNode();
        first.input().forEach(input::add);
        first.output().forEach(input::add);
        input.add(mapper.valueToTree(Map.of(
                "type",
                "function_call_output",
                "call_id",
                first.callId(),
                "output",
                mapper.writeValueAsString(Map.of("evidence", batch.evidence(), "queries", batch.executions()))
        )));
        return input;
    }

    public AnalysisResult finish(LlmTurn first, QueryBatch batch, IncidentSnapshot incident) throws IOException {
        var body = request(batch.evidence(), incident, continuation(first, batch));
        body.put("tools", List.of(tool()));
        body.put("tool_choice", "none");
        body.put("parallel_tool_calls", false);
        JsonNode response = send(body, Duration.ofSeconds(30), "final");
        return readResult(
                response,
                batch.evidence(),
                first.inputTokens() + usage(response, "input_tokens"),
                first.outputTokens() + usage(response, "output_tokens")
        );
    }

    private Map<String, Object> request(
            EvidenceBundle evidence,
            IncidentSnapshot incident,
            JsonNode input
    ) throws IOException {
        if (mapper.writeValueAsString(input).length() > MAX_INPUT_CHARACTERS) {
            throw new RemoteCallException("INPUT_TOO_LARGE");
        }
        String runbook = new ClassPathResource("analysis/" + incident.profile() + ".txt").getContentAsString(StandardCharsets.UTF_8);
        var body = new LinkedHashMap<String, Object>();
        body.put("model", properties.llmModel());
        body.put("store", false);
        body.put("include", List.of("reasoning.encrypted_content"));
        body.put("instructions", instructions + "\n프로젝트 대응 문서:\n" + runbook);
        body.put("input", input);
        body.put("max_output_tokens", 2000);
        var format = Map.of(
                "type",
                "json_schema",
                "name",
                "incident_report",
                "strict",
                true,
                "schema",
                schemaFor(evidence)
        );
        body.put("text", Map.of("format", format));
        return body;
    }

    private JsonNode send(Map<String, Object> body, Duration timeout, String phase) {
        meters.counter("incident_model_request", "phase", phase).increment();
        JsonNode response = http.post(
                properties.llmUrl(),
                body,
                Map.of("Authorization", "Bearer " + properties.llmApiKey()),
                timeout
        );
        meters.counter("incident_llm_tokens", "direction", "input").increment(usage(response, "input_tokens"));
        meters.counter("incident_llm_tokens", "direction", "output").increment(usage(response, "output_tokens"));
        return response;
    }

    private long usage(JsonNode response, String field) {
        return Math.max(0, response.path("usage").path(field).asLong(0));
    }

    private Map<String, Object> tool() {
        var ids = Arrays.stream(QueryName.values())
                .map(QueryName::getId)
                .toList();
        String descriptions = Arrays.stream(QueryName.values())
                .map(query -> query.getId() + ": " + query.getDescription())
                .reduce((left, right) -> left + "; " + right)
                .orElse("");
        var queryArray = Map.of(
                "type",
                "array",
                "items",
                Map.of("type", "string", "enum", ids),
                "minItems",
                1,
                "maxItems",
                4
        );
        return Map.of(
                "type",
                "function",
                "name",
                TOOL_NAME,
                "description",
                "장애 발생 직전 15분의 추가 근거를 조회합니다. 필요한 조회만 선택하세요. " + descriptions,
                "strict",
                true,
                "parameters",
                Map.of(
                        "type",
                        "object",
                        "properties",
                        Map.of("queries", queryArray),
                        "required",
                        List.of("queries"),
                        "additionalProperties",
                        false
                )
        );
    }

    private Map<String, Object> reportTool(EvidenceBundle evidence) {
        return Map.of(
                "type", "function",
                "name", REPORT_TOOL_NAME,
                "description", "현재 관측 자료가 질문에서 요구한 범위를 충족하고 추가 조회가 필요하지 않을 때 보고서를 제출해 답변을 마칩니다. 자료 종류나 관측 구간이 부족하면 collect_incident_evidence를 선택합니다.",
                "strict", true,
                "parameters", schemaFor(evidence)
        );
    }

    private JsonNode schemaFor(EvidenceBundle evidence) {
        if (!evidence.hasEvidence()) {
            throw new RemoteCallException("NO_EVIDENCE");
        }
        ObjectNode requestSchema = (ObjectNode) schema.deepCopy();
        var ids = mapper.createArrayNode();
        evidence.items().forEach(item -> ids.add(item.id()));
        for (String field : List.of("observedFacts", "hypotheses")) {
            ObjectNode references = (ObjectNode) requestSchema.path("properties").path(field)
                    .path("items").path("properties").path("evidenceIds");
            references.put("minItems", 1);
            references.put("maxItems", 10);
            ((ObjectNode) references.path("items")).set("enum", ids.deepCopy());
        }
        return requestSchema;
    }

    private AnalysisResult readResult(
            JsonNode response,
            EvidenceBundle evidence,
            long inputTokens,
            long outputTokens
    ) {
        requireCompleted(response);
        if (response.path("output")
                .valueStream()
                .anyMatch(item -> "function_call".equals(item.path("type").asText()))) {
            throw new RemoteCallException("UNEXPECTED_TOOL_CALL");
        }
        String output = response.path("output")
                .valueStream()
                .filter(item -> "message".equals(item.path("type").asText()))
                .flatMap(item -> item.path("content")
                        .valueStream())
                .filter(item -> "output_text".equals(item.path("type").asText()))
                .map(item -> item.path("text").asText())
                .findFirst()
                .orElseThrow(() -> new RemoteCallException("MISSING_MODEL_OUTPUT"));
        return report(output, evidence, inputTokens, outputTokens);
    }

    private AnalysisResult report(
            String output,
            EvidenceBundle evidence,
            long inputTokens,
            long outputTokens
    ) {
        AnalysisReport report = mapper.readValue(output, AnalysisReport.class);
        if (report == null) {
            throw new IllegalArgumentException("INVALID_MODEL_OUTPUT");
        }
        validator.validate(report, evidence);
        return new AnalysisResult(
                report,
                properties.llmModel(),
                "v3-query-tools",
                inputTokens,
                outputTokens,
                evidence.missingEvidence(),
                evidence.items()
        );
    }

    private void requireCompleted(JsonNode response) {
        if (!"completed".equals(response.path("status").asText())) {
            throw new RemoteCallException("INCOMPLETE_MODEL_OUTPUT");
        }
    }
}
