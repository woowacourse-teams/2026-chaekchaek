package com.chaekchaek.pmfsurvey.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "pmf_survey_answer")
@IdClass(PmfSurveyAnswer.PmfSurveyAnswerId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PmfSurveyAnswer {

    @Id
    @Column(name = "response_id", nullable = false)
    private long responseId;

    @Id
    @Column(name = "question_code", nullable = false, length = 50)
    private String questionCode;

    @Id
    @Column(name = "answer_code", nullable = false, length = 50)
    private String answerCode;

    public PmfSurveyAnswer(long responseId, String questionCode, String answerCode) {
        this.responseId = responseId;
        this.questionCode = questionCode;
        this.answerCode = answerCode;
    }

    @EqualsAndHashCode
    public static class PmfSurveyAnswerId implements Serializable {
        private long responseId;
        private String questionCode;
        private String answerCode;

        public PmfSurveyAnswerId() {
        }

        public PmfSurveyAnswerId(long responseId, String questionCode, String answerCode) {
            this.responseId = responseId;
            this.questionCode = questionCode;
            this.answerCode = answerCode;
        }
    }
}
