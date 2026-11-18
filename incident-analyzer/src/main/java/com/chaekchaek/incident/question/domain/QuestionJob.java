package com.chaekchaek.incident.question.domain;

import com.chaekchaek.incident.question.dto.QuestionRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionJob {
    @Id
    private String id;
    private String environment;
    private String channel;
    private String threadTs;
    @Column(length = 2000)
    private String question;
    private Instant receivedAt;
    private Instant deadlineAt;
    private Instant leaseUntil;
    private Instant nextAttemptAt;
    @Enumerated(EnumType.STRING)
    private QuestionState workState;
    private int attempts;
    private int deliveryAttempts;
    @Lob
    private String resultJson;
    @Column(length = 100)
    private String failureCode;

    public QuestionJob(QuestionRequest request, Instant receivedAt) {
        this.id = request.eventId();
        this.environment = request.environment();
        this.channel = request.channel();
        this.threadTs = request.threadTs();
        this.question = request.question();
        this.receivedAt = receivedAt;
        this.deadlineAt = receivedAt.plusSeconds(90);
        this.nextAttemptAt = receivedAt;
        this.workState = QuestionState.PENDING;
    }

    public void claim(Instant now) {
        boolean expired = workState == QuestionState.PROCESSING && leaseUntil.isBefore(now);
        boolean ready = (workState == QuestionState.PENDING || workState == QuestionState.READY)
                && !nextAttemptAt.isAfter(now);
        if (!expired && !ready) {
            throw new IllegalStateException("대기 중이거나 처리 기한이 지난 질문이어야 합니다");
        }
        workState = QuestionState.PROCESSING;
        leaseUntil = now.plusSeconds(120);
        if (resultJson == null) {
            attempts++;
        } else {
            deliveryAttempts++;
        }
    }

    public void recordAnalysisFailure(String code) {
        if (workState != QuestionState.PROCESSING || resultJson != null) {
            throw new IllegalStateException("분석 중인 질문이어야 합니다");
        }
        failureCode = code;
    }

    public void prepare(String resultJson) {
        if (workState != QuestionState.PROCESSING || this.resultJson != null) {
            throw new IllegalStateException("처리 중인 질문이어야 합니다");
        }
        this.resultJson = resultJson;
        deliveryAttempts = 1;
        workState = QuestionState.READY;
        leaseUntil = null;
    }

    public void sent() {
        if (resultJson == null || (workState != QuestionState.READY && workState != QuestionState.PROCESSING)) {
            throw new IllegalStateException("준비한 답변을 발송 중인 질문이어야 합니다");
        }
        workState = QuestionState.SENT;
        leaseUntil = null;
    }

    public void fail(String code, Instant now) {
        failureCode = code;
        leaseUntil = null;
        if (resultJson != null && deliveryAttempts < 3) {
            workState = QuestionState.READY;
            nextAttemptAt = now.plusSeconds(15L * deliveryAttempts);
        } else {
            workState = QuestionState.FAILED;
        }
    }

    public void cancel() {
        workState = QuestionState.CANCELLED;
        leaseUntil = null;
    }
}
