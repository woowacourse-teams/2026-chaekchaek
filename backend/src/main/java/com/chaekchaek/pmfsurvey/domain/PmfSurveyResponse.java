package com.chaekchaek.pmfsurvey.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "pmf_survey_response")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PmfSurveyResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "response_id")
    private Long id;

    @Column(name = "actor_id", nullable = false)
    private long actorId;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    private PmfSurveyResponse(long actorId, Instant submittedAt) {
        this.actorId = actorId;
        this.submittedAt = submittedAt;
    }

    public static PmfSurveyResponse create(long actorId, Instant submittedAt) {
        if (actorId < 1 || submittedAt == null) {
            throw new IllegalArgumentException("PMF survey response requires an actor and submission time");
        }
        return new PmfSurveyResponse(actorId, submittedAt);
    }
}
