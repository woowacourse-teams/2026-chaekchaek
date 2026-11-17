package com.chaekchaek.incident.analysis;

import com.chaekchaek.incident.common.RemoteCallException;
import java.util.Set;

public final class AnalysisFailureCode {
    private AnalysisFailureCode() {
    }

    public static String from(Exception exception) {
        String message = exception.getMessage();
        if (exception instanceof RemoteCallException && message != null
                && message.matches("HTTP_[0-9]{3}|SLACK_[a-z_]{1,64}|[A-Z_]{1,64}")) {
            return message;
        }
        if (exception instanceof IllegalArgumentException && message != null
                && Set.of("INVALID_MODEL_OUTPUT", "INVALID_MODEL_TEXT", "UNKNOWN_EVIDENCE_REFERENCE").contains(message)) {
            return message;
        }
        return "ANALYSIS_PROCESSING_ERROR";
    }
}
