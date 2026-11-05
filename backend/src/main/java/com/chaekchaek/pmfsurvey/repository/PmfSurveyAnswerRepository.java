package com.chaekchaek.pmfsurvey.repository;

import com.chaekchaek.pmfsurvey.domain.PmfSurveyAnswer;
import com.chaekchaek.pmfsurvey.domain.PmfSurveyAnswer.PmfSurveyAnswerId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PmfSurveyAnswerRepository extends JpaRepository<PmfSurveyAnswer, PmfSurveyAnswerId> {

    @Query(value = """
            select question_code as questionCode, answer_code as answerCode, count(*) as answerCount
            from pmf_survey_answer
            group by question_code, answer_code
            """, nativeQuery = true)
    List<AnswerCount> countAnswersByQuestionAndAnswer();

    interface AnswerCount {
        String getQuestionCode();
        String getAnswerCode();
        long getAnswerCount();
    }
}
