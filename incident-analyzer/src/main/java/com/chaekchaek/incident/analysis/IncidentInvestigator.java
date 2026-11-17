package com.chaekchaek.incident.analysis;

import com.chaekchaek.incident.alert.dto.IncidentSnapshot;
import com.chaekchaek.incident.analysis.client.LlmClient;
import com.chaekchaek.incident.analysis.dto.AnalysisReport;
import com.chaekchaek.incident.analysis.dto.AnalysisResult;
import com.chaekchaek.incident.analysis.dto.InvestigationResult;
import com.chaekchaek.incident.analysis.dto.LlmTurn;
import com.chaekchaek.incident.analysis.service.ModelBudgetService;
import com.chaekchaek.incident.evidence.QueryExecutor;
import com.chaekchaek.incident.evidence.dto.EvidenceBundle;
import com.chaekchaek.incident.evidence.dto.QueryBatch;
import com.chaekchaek.incident.question.dto.ConversationTurn;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IncidentInvestigator {
    private final LlmClient llm;
    private final QueryExecutor queries;
    private final ModelBudgetService budget;
    private final Clock clock;

    public InvestigationResult investigate(
            EvidenceBundle evidence,
            IncidentSnapshot incident,
            String question,
            List<ConversationTurn> conversation,
            Instant deadline
    ) throws IOException {
        var base = new QueryBatch(
                evidence,
                List.of()
        );
        if (!hasTime(deadline, 35)) {
            return unavailable(base, "답변 제한 시간이 부족합니다", 0, 0);
        }
        if (!budget.reserve()) {
            return unavailable(base, "LLM이 비활성 상태이거나 오늘의 호출 한도에 도달했습니다", 0, 0);
        }
        LlmTurn first = llm.begin(evidence, incident, question, conversation, hasTime(deadline, 75));
        if (!first.requestsQueries()) {
            return new InvestigationResult(
                    first.report(),
                    List.of()
            );
        }
        QueryBatch batch = queries.execute(first.selection(), incident, evidence, deadline);
        return complete(first, batch, incident, deadline);
    }

    private InvestigationResult complete(
            LlmTurn first,
            QueryBatch batch,
            IncidentSnapshot incident,
            Instant deadline
    ) throws IOException {
        if (!hasTime(deadline, 35)) {
            return unavailable(batch, "추가 조회 후 최종 답변 제한 시간이 부족합니다", first.inputTokens(), first.outputTokens());
        }
        if (!llm.continuationFits(first, batch)) {
            return unavailable(batch, "추가 조회 결과가 모델 입력량 제한을 넘었습니다", first.inputTokens(), first.outputTokens());
        }
        if (!budget.reserve()) {
            return unavailable(batch, "추가 조회 후 오늘의 모델 호출 한도에 도달했습니다", first.inputTokens(), first.outputTokens());
        }
        return new InvestigationResult(
                llm.finish(first, batch, incident),
                batch.executions()
        );
    }

    private boolean hasTime(Instant deadline, int seconds) {
        return !clock.instant().plusSeconds(seconds).isAfter(deadline);
    }

    private InvestigationResult unavailable(
            QueryBatch batch,
            String reason,
            long inputTokens,
            long outputTokens
    ) {
        return new InvestigationResult(
                new AnalysisResult(
                        new AnalysisReport(
                                "모델 답변을 생성하지 못했습니다. 조회된 자료와 제한 사유를 확인해 주세요.",
                                "INSUFFICIENT_EVIDENCE",
                                List.of(),
                                List.of(),
                                List.of(reason)
                        ),
                        "none",
                        "v3-query-tools",
                        inputTokens,
                        outputTokens,
                        batch.evidence().missingEvidence(),
                        batch.evidence().items()
                ),
                batch.executions()
        );
    }
}
