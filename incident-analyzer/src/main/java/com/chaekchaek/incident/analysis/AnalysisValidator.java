package com.chaekchaek.incident.analysis;

import com.chaekchaek.incident.analysis.dto.AnalysisReport;
import com.chaekchaek.incident.evidence.dto.EvidenceBundle;
import com.chaekchaek.incident.evidence.dto.EvidenceItem;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AnalysisValidator {
    public void validate(AnalysisReport report, EvidenceBundle evidence) {
        text(report.summary(), 600);
        if (report.assessment() == null || !Set.of("SUPPORTED", "PARTIAL", "INSUFFICIENT_EVIDENCE").contains(report.assessment())
                || report.observedFacts() == null || report.hypotheses() == null || report.missingEvidence() == null
                || report.observedFacts().size() > 10 || report.hypotheses().size() > 3 || report.missingEvidence().size() > 10) {
            throw new IllegalArgumentException("INVALID_MODEL_OUTPUT");
        }
        Set<String> ids = evidence.items()
                .stream()
                .map(EvidenceItem::id)
                .collect(Collectors.toSet());
        report.observedFacts().forEach(fact -> {
            text(fact.description(), 500);
            references(fact.evidenceIds(), ids);
        });
        report.hypotheses().forEach(candidate -> validateCandidate(candidate, ids));
        report.missingEvidence().forEach(value -> text(value, 300));
    }

    private void validateCandidate(AnalysisReport.Hypothesis candidate, Set<String> ids) {
        text(candidate.candidate(), 300);
        text(candidate.nextCheck(), 500);
        text(candidate.expectedObservation(), 500);
        references(candidate.evidenceIds(), ids);
    }

    private void references(List<String> references, Set<String> ids) {
        if (references == null || references.isEmpty() || references.size() > 10 || !ids.containsAll(references)) {
            throw new IllegalArgumentException("UNKNOWN_EVIDENCE_REFERENCE");
        }
    }

    private void text(String text, int limit) {
        if (text == null || text.isBlank() || text.length() > limit) {
            throw new IllegalArgumentException("INVALID_MODEL_TEXT");
        }
    }
}
