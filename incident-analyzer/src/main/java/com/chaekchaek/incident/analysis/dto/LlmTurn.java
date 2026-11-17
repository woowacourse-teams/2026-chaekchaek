package com.chaekchaek.incident.analysis.dto;

import com.chaekchaek.incident.evidence.dto.QuerySelection;
import tools.jackson.databind.JsonNode;

public record LlmTurn(
        JsonNode input,
        JsonNode output,
        AnalysisResult report,
        QuerySelection selection,
        String callId,
        long inputTokens,
        long outputTokens
) {
    public LlmTurn {
        input = input.deepCopy();
        output = output.deepCopy();
    }

    public JsonNode input() {
        return input.deepCopy();
    }

    public JsonNode output() {
        return output.deepCopy();
    }

    public boolean requestsQueries() {
        return selection != null;
    }

    @Override
    public String toString() {
        return "LlmTurn[requestsQueries=" + requestsQueries() + "]";
    }
}
