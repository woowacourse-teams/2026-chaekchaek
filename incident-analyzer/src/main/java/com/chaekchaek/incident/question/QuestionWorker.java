package com.chaekchaek.incident.question;

import com.chaekchaek.incident.alert.service.IncidentService;
import com.chaekchaek.incident.analysis.IncidentInvestigator;
import com.chaekchaek.incident.common.RemoteCallException;
import com.chaekchaek.incident.evidence.EvidenceCollector;
import com.chaekchaek.incident.notification.AnalysisMessage;
import com.chaekchaek.incident.question.domain.QuestionRejectedException;
import com.chaekchaek.incident.question.dto.PreparedReply;
import com.chaekchaek.incident.question.dto.QuestionTask;
import com.chaekchaek.incident.question.service.QuestionService;
import com.chaekchaek.incident.slack.ParentAlertResolver;
import com.chaekchaek.incident.slack.client.SlackApiClient;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "incident.worker-enabled", havingValue = "true", matchIfMissing = true)
public class QuestionWorker {
    private final QuestionService questions;
    private final IncidentService incidents;
    private final ParentAlertResolver parents;
    private final EvidenceCollector evidence;
    private final IncidentInvestigator investigator;
    private final AnalysisMessage messages;
    private final SlackApiClient slack;
    private final JsonMapper mapper;
    private final MeterRegistry meters;
    private final Clock clock;

    @Scheduled(fixedDelay = 1000)
    public void poll() {
        questions.claimNext().ifPresent(this::process);
    }

    public void process(QuestionTask task) {
        var timer = Timer.start(meters);
        try {
            if (!questions.canDeliver(task)) {
                questions.cancel(task.id());
                return;
            }
            PreparedReply reply = prepared(task);
            if (!questions.canDeliver(task)) {
                questions.cancel(task.id());
                return;
            }
            slack.reply(task, reply);
            questions.delivered(task.id());
            meters.counter("incident_delivery", "outcome", "sent").increment();
        } catch (RuntimeException exception) {
            String code = "QUESTION_DELIVERY_FAILED";
            if (exception instanceof RemoteCallException) {
                code = exception.getMessage();
            }
            questions.fail(task.id(), code);
            meters.counter("incident_delivery", "outcome", "failed").increment();
        } finally {
            timer.stop(meters.timer("incident_processing"));
        }
    }

    private PreparedReply prepared(QuestionTask task) {
        if (task.resultJson() != null) {
            return mapper.readValue(task.resultJson(), PreparedReply.class);
        }
        PreparedReply reply = answer(task);
        questions.prepare(task.id(), reply);
        return reply;
    }

    private PreparedReply answer(QuestionTask task) {
        if (task.attempts() > 1 || clock.instant().isAfter(task.deadlineAt())) {
            return PreparedReply.notice("질문 처리가 중단되었거나 대기 시간이 지났습니다. 다시 멘션하여 질문해 주세요.");
        }
        try {
            var incident = incidents.find(parents.resolve(task)).orElseThrow(() ->
                    new QuestionRejectedException("장애 정보가 아직 저장되지 않았습니다. 잠시 뒤 같은 스레드에서 다시 질문해 주세요."));
            var bundle = evidence.collect(incident);
            if (bundle.items().isEmpty()) {
                return PreparedReply.notice("발생 시점의 관측 자료를 확인할 수 없습니다. Grafana에서 수집 상태와 보존 기간을 확인해 주세요.");
            }
            var result = investigator.investigate(
                    bundle,
                    incident,
                    task.question(),
                    questions.conversation(task),
                    task.deadlineAt()
            );
            return messages.reply(incident, result.analysis(), result.queries());
        } catch (QuestionRejectedException exception) {
            return PreparedReply.notice(exception.getMessage());
        } catch (Exception exception) {
            String code = failureCode(exception);
            questions.analysisFailed(task.id(), code);
            meters.counter("incident_analysis", "outcome", "failed", "reason", code).increment();
            log.warn("incident_question_analysis_failed questionId={} code={}", task.id(), code);
            return PreparedReply.notice("장애 질문에 답하는 중 오류가 발생했습니다. Grafana에서 근거를 확인하거나 다시 멘션하여 질문해 주세요.");
        }
    }

    private String failureCode(Exception exception) {
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
