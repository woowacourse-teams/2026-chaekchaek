package com.chaekchaek.pmfsurvey.service;

import com.chaekchaek.admin.authorization.AdminOnly;
import com.chaekchaek.common.auth.CurrentActor;
import com.chaekchaek.common.auth.CurrentActorProvider;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.pmfsurvey.domain.PmfSurveyAnswer;
import com.chaekchaek.pmfsurvey.domain.PmfSurveyQuestionCatalog;
import com.chaekchaek.pmfsurvey.domain.PmfSurveyResponse;
import com.chaekchaek.pmfsurvey.dto.PmfSurveyAnswerCountResponse;
import com.chaekchaek.pmfsurvey.dto.PmfSurveyAnswerRequest;
import com.chaekchaek.pmfsurvey.dto.PmfSurveyAvailabilityResponse;
import com.chaekchaek.pmfsurvey.dto.PmfSurveyQuestionResponse;
import com.chaekchaek.pmfsurvey.dto.PmfSurveyQuestionSummaryResponse;
import com.chaekchaek.pmfsurvey.dto.PmfSurveySubmitRequest;
import com.chaekchaek.pmfsurvey.dto.PmfSurveySubmissionResponse;
import com.chaekchaek.pmfsurvey.dto.PmfSurveySummaryResponse;
import com.chaekchaek.pmfsurvey.repository.PmfSurveyAnswerRepository;
import com.chaekchaek.pmfsurvey.repository.PmfSurveyEligibilityReader;
import com.chaekchaek.pmfsurvey.repository.PmfSurveyResponseRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PmfSurveyService {

    private static final Duration ELIGIBILITY_PERIOD = Duration.ofDays(14);

    private final CurrentActorProvider currentActorProvider;
    private final PmfSurveyResponseRepository responseRepository;
    private final PmfSurveyAnswerRepository answerRepository;
    private final PmfSurveyEligibilityReader eligibilityReader;
    private final PmfSurveyEligibilityTokenProvider eligibilityTokenProvider;
    private final Clock clock;
    private final Set<Long> excludedMemberIds;

    public PmfSurveyService(
            CurrentActorProvider currentActorProvider,
            PmfSurveyResponseRepository responseRepository,
            PmfSurveyAnswerRepository answerRepository,
            PmfSurveyEligibilityReader eligibilityReader,
            PmfSurveyEligibilityTokenProvider eligibilityTokenProvider,
            Clock clock,
            @Value("${app.pmf-survey.excluded-member-ids:}") String excludedMemberIds
    ) {
        this.currentActorProvider = currentActorProvider;
        this.responseRepository = responseRepository;
        this.answerRepository = answerRepository;
        this.eligibilityReader = eligibilityReader;
        this.eligibilityTokenProvider = eligibilityTokenProvider;
        this.clock = clock;
        this.excludedMemberIds = parseMemberIds(excludedMemberIds);
    }

    @Transactional(readOnly = true)
    public PmfSurveyAvailabilityResponse getSurvey() {
        CurrentActor actor = currentActorProvider.getCurrentActor();
        boolean alreadySubmitted = responseRepository.existsByActorId(actor.actorId());
        boolean eligible = isEligible(actor);
        String submissionToken = eligible && !alreadySubmitted
                ? eligibilityTokenProvider.issue(actor.actorId())
                : null;
        List<PmfSurveyQuestionResponse> questions = eligible && !alreadySubmitted
                ? PmfSurveyQuestionCatalog.questions()
                : List.of();
        return new PmfSurveyAvailabilityResponse(eligible, alreadySubmitted, submissionToken, questions);
    }

    @Transactional
    public PmfSurveySubmissionResponse submit(PmfSurveySubmitRequest request) {
        CurrentActor actor = currentActorProvider.getCurrentActor();
        if (responseRepository.existsByActorId(actor.actorId())) {
            throw new BusinessException(ErrorCode.PMF_SURVEY_ALREADY_RESPONDED);
        }
        if (!eligibilityTokenProvider.isValidFor(request.submissionToken(), actor.actorId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        List<ValidatedAnswer> answers = validate(request);
        Instant submittedAt = clock.instant();
        PmfSurveyResponse response;
        try {
            response = responseRepository.saveAndFlush(PmfSurveyResponse.create(actor.actorId(), submittedAt));
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.PMF_SURVEY_ALREADY_RESPONDED);
        }
        answerRepository.saveAll(answers.stream()
                .flatMap(answer -> answer.answerCodes().stream()
                        .map(code -> new PmfSurveyAnswer(response.getId(), answer.questionCode(), code)))
                .toList());
        return new PmfSurveySubmissionResponse(submittedAt);
    }

    @Transactional(readOnly = true)
    @AdminOnly
    public PmfSurveySummaryResponse getSummary() {
        Map<String, Map<String, Long>> counts = answerRepository.countAnswersByQuestionAndAnswer()
                .stream()
                .collect(Collectors.groupingBy(
                        PmfSurveyAnswerRepository.AnswerCount::getQuestionCode,
                        Collectors.toMap(
                                PmfSurveyAnswerRepository.AnswerCount::getAnswerCode,
                                PmfSurveyAnswerRepository.AnswerCount::getAnswerCount
                        )
                ));
        List<PmfSurveyQuestionSummaryResponse> questions = PmfSurveyQuestionCatalog.questions()
                .stream()
                .map(question -> new PmfSurveyQuestionSummaryResponse(
                        question.questionCode(),
                        question.question(),
                        question.options().stream()
                                .map(option -> new PmfSurveyAnswerCountResponse(
                                        option.answerCode(),
                                        option.label(),
                                        counts.getOrDefault(question.questionCode(), Map.of())
                                                .getOrDefault(option.answerCode(), 0L)
                                ))
                                .toList()
                ))
                .toList();
        return new PmfSurveySummaryResponse(responseRepository.count(), questions);
    }

    private boolean isEligible(CurrentActor actor) {
        if (actor.memberId() != null && excludedMemberIds.contains(actor.memberId())) {
            return false;
        }
        Instant since = clock.instant().minus(ELIGIBILITY_PERIOD);
        return eligibilityReader.hasCoreActionSince(actor.actorId(), actor.memberId(), since);
    }

    private List<ValidatedAnswer> validate(PmfSurveySubmitRequest request) {
        Map<String, PmfSurveyAnswerRequest> answersByQuestion = new HashMap<>();
        for (PmfSurveyAnswerRequest answer : request.answers()) {
            if (answersByQuestion.putIfAbsent(answer.questionCode(), answer) != null) {
                throw invalidRequest();
            }
        }
        if (answersByQuestion.size() != PmfSurveyQuestionCatalog.questions().size()) {
            throw invalidRequest();
        }

        List<ValidatedAnswer> validated = new ArrayList<>();
        for (PmfSurveyQuestionResponse question : PmfSurveyQuestionCatalog.questions()) {
            PmfSurveyAnswerRequest answer = answersByQuestion.get(question.questionCode());
            if (answer == null || answer.answerCodes() == null || answer.answerCodes().isEmpty()) {
                throw invalidRequest();
            }
            Set<String> selectedCodes = new HashSet<>(answer.answerCodes());
            if (selectedCodes.size() != answer.answerCodes().size()
                    || (!question.multipleSelection() && selectedCodes.size() != 1)
                    || selectedCodes.stream().anyMatch(code -> question.options().stream()
                            .noneMatch(option -> option.answerCode().equals(code)))) {
                throw invalidRequest();
            }
            if (question.questionCode().equals(PmfSurveyQuestionCatalog.UNKNOWN_FEATURES)
                    && selectedCodes.contains(PmfSurveyQuestionCatalog.NO_UNKNOWN_FEATURES)
                    && selectedCodes.size() > 1) {
                throw invalidRequest();
            }
            validated.add(new ValidatedAnswer(question.questionCode(), List.copyOf(selectedCodes)));
        }
        return validated;
    }

    private BusinessException invalidRequest() {
        return new BusinessException(ErrorCode.INVALID_REQUEST);
    }

    private Set<Long> parseMemberIds(String memberIds) {
        if (memberIds == null || memberIds.isBlank()) {
            return Set.of();
        }
        return java.util.Arrays.stream(memberIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Long::parseLong)
                .collect(Collectors.toUnmodifiableSet());
    }

    private record ValidatedAnswer(
            String questionCode,
            List<String> answerCodes
    ) {
    }
}
