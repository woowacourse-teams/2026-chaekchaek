package com.chaekchaek.incident.question.service;

import com.chaekchaek.incident.config.IncidentProperties;
import com.chaekchaek.incident.config.SlackProperties;
import com.chaekchaek.incident.question.domain.QuestionJob;
import com.chaekchaek.incident.question.domain.QuestionState;
import com.chaekchaek.incident.question.dto.ConversationTurn;
import com.chaekchaek.incident.question.dto.PreparedReply;
import com.chaekchaek.incident.question.dto.QuestionRequest;
import com.chaekchaek.incident.question.dto.QuestionTask;
import com.chaekchaek.incident.question.repository.QuestionJobRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class QuestionService {
    private final QuestionJobRepository jobs;
    private final SlackProperties slack;
    private final IncidentProperties incident;
    private final Clock clock;
    private final JsonMapper mapper;

    @Transactional(timeout = 2)
    public void accept(QuestionRequest request) {
        if (allowed(request.environment(), request.channel()) && !jobs.existsById(request.eventId())) {
            jobs.saveAndFlush(new QuestionJob(request, clock.instant()));
        }
    }

    @Transactional
    public Optional<QuestionTask> claimNext() {
        if (!slack.enabled() || slack.channels().isEmpty() || incident.enabledEnvironments().isEmpty()) {
            return Optional.empty();
        }
        var candidates = jobs.findAvailable(
                List.of(QuestionState.PENDING, QuestionState.READY),
                QuestionState.PROCESSING,
                clock.instant(),
                List.copyOf(incident.enabledEnvironments()),
                List.copyOf(slack.channels()),
                PageRequest.of(0, 1)
        );
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        var job = candidates.getFirst();
        if (!allowed(job.getEnvironment(), job.getChannel())) {
            job.cancel();
            return Optional.empty();
        }
        job.claim(clock.instant());
        return Optional.of(snapshot(job));
    }

    private QuestionTask snapshot(QuestionJob job) {
        return new QuestionTask(
                job.getId(),
                job.getEnvironment(),
                job.getChannel(),
                job.getThreadTs(),
                job.getQuestion(),
                job.getReceivedAt(),
                job.getDeadlineAt(),
                job.getAttempts(),
                job.getResultJson()
        );
    }

    @Transactional(readOnly = true)
    public boolean canDeliver(QuestionTask task) {
        return allowed(task.environment(), task.channel());
    }

    private boolean allowed(String environment, String channel) {
        return slack.enabled() && incident.enabledEnvironments().contains(environment)
                && slack.environment(channel).filter(environment::equals).isPresent();
    }

    @Transactional(readOnly = true)
    public List<ConversationTurn> conversation(QuestionTask task) {
        return jobs.findConversation(
                        task.channel(),
                        task.threadTs(),
                        QuestionState.SENT,
                        task.id(),
                        task.receivedAt(),
                        PageRequest.of(0, 2)
                )
                .reversed()
                .stream()
                .map(job -> new ConversationTurn(
                        job.getQuestion(),
                        mapper.readValue(job.getResultJson(), PreparedReply.class).contextSummary()
                ))
                .toList();
    }

    @Transactional
    public void analysisFailed(String id, String code) {
        jobs.lockById(id).orElseThrow().recordAnalysisFailure(code);
    }

    @Transactional
    public void prepare(String id, PreparedReply reply) {
        jobs.lockById(id).orElseThrow().prepare(mapper.writeValueAsString(reply));
    }

    @Transactional
    public void delivered(String id) {
        jobs.lockById(id).orElseThrow().sent();
    }

    @Transactional
    public void cancel(String id) {
        jobs.lockById(id).orElseThrow().cancel();
    }

    @Transactional
    public void fail(String id, String code) {
        jobs.lockById(id).orElseThrow().fail(code, clock.instant());
    }
}
