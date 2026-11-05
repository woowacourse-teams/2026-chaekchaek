package com.chaekchaek.pmfsurvey.repository;

import com.chaekchaek.pmfsurvey.domain.PmfSurveyResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PmfSurveyResponseRepository extends JpaRepository<PmfSurveyResponse, Long> {

    boolean existsByActorId(long actorId);
}
