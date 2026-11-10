package com.chaekchaek.pmfsurvey.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chaekchaek.common.auth.CurrentActor;
import com.chaekchaek.common.auth.CurrentActorProvider;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.pmfsurvey.domain.PmfSurveyAnswer;
import com.chaekchaek.pmfsurvey.domain.PmfSurveyResponse;
import com.chaekchaek.pmfsurvey.dto.PmfSurveyAnswerRequest;
import com.chaekchaek.pmfsurvey.dto.PmfSurveyAvailabilityResponse;
import com.chaekchaek.pmfsurvey.dto.PmfSurveySubmitRequest;
import com.chaekchaek.pmfsurvey.repository.PmfSurveyAnswerRepository;
import com.chaekchaek.pmfsurvey.repository.PmfSurveyEligibilityReader;
import com.chaekchaek.pmfsurvey.repository.PmfSurveyResponseRepository;
import com.chaekchaek.pmfsurvey.service.PmfSurveyEligibilityTokenProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PmfSurveyServiceTest {

    private static final long ACTOR_ID = 42L;
    private static final long MEMBER_ID = 42L;
    private static final Instant NOW = Instant.parse("2026-11-05T03:00:00Z");

    @Mock
    private CurrentActorProvider currentActorProvider;

    @Mock
    private PmfSurveyResponseRepository responseRepository;

    @Mock
    private PmfSurveyAnswerRepository answerRepository;

    @Mock
    private PmfSurveyEligibilityReader eligibilityReader;

    @Mock
    private PmfSurveyEligibilityTokenProvider eligibilityTokenProvider;

    private PmfSurveyService pmfSurveyService;

    @BeforeEach
    void setUp() {
        pmfSurveyService = new PmfSurveyService(
                currentActorProvider,
                responseRepository,
                answerRepository,
                eligibilityReader,
                eligibilityTokenProvider,
                Clock.fixed(NOW, ZoneOffset.UTC),
                "1,2,3,5,11,12"
        );
        when(currentActorProvider.getCurrentActor()).thenReturn(CurrentActor.member(ACTOR_ID, MEMBER_ID));
    }

    @Test
    @DisplayName("대상이며 미응답한 사용자에게 설문 문항을 반환한다")
    void should_ReturnQuestions_When_ActorIsEligibleAndHasNotResponded() {
        when(responseRepository.existsByActorId(ACTOR_ID)).thenReturn(false);
        when(eligibilityReader.hasCoreActionSince(any(Long.class), any(Long.class), any(Instant.class)))
                .thenReturn(1L);
        when(eligibilityTokenProvider.issue(ACTOR_ID)).thenReturn("signed-token");

        PmfSurveyAvailabilityResponse response = pmfSurveyService.getSurvey();

        assertThat(response.eligible()).isTrue();
        assertThat(response.alreadySubmitted()).isFalse();
        assertThat(response.submissionToken()).isEqualTo("signed-token");
        assertThat(response.questions()).hasSize(4);
        assertThat(response.questions().get(2).multipleSelection()).isTrue();
        verify(eligibilityReader).hasCoreActionSince(ACTOR_ID, MEMBER_ID, NOW.minusSeconds(14 * 24 * 60 * 60L));
    }

    @Test
    @DisplayName("팀원은 핵심 행동이 있어도 설문 대상에서 제외한다")
    void should_NotReturnQuestions_When_ActorIsTeamMember() {
        when(currentActorProvider.getCurrentActor()).thenReturn(CurrentActor.member(101L, 1L));
        when(responseRepository.existsByActorId(101L)).thenReturn(false);

        PmfSurveyAvailabilityResponse response = pmfSurveyService.getSurvey();

        assertThat(response.eligible()).isFalse();
        assertThat(response.questions()).isEmpty();
        verify(eligibilityReader, never()).hasCoreActionSince(any(Long.class), any(Long.class), any(Instant.class));
    }

    @Test
    @DisplayName("복수 선택 문항의 모든 답변을 한 제출에 저장한다")
    void should_SaveEachSelectedOption_When_SubmittingMultiSelectAnswer() {
        when(responseRepository.existsByActorId(ACTOR_ID)).thenReturn(false);
        when(eligibilityTokenProvider.isValidFor("signed-token", ACTOR_ID)).thenReturn(true);
        when(responseRepository.saveAndFlush(any(PmfSurveyResponse.class))).thenAnswer(invocation -> {
            PmfSurveyResponse response = invocation.getArgument(0);
            ReflectionTestUtils.setField(response, "id", 100L);
            return response;
        });
        when(answerRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        pmfSurveyService.submit(validRequest());

        ArgumentCaptor<Iterable<PmfSurveyAnswer>> answersCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(answerRepository).saveAll(answersCaptor.capture());
        List<PmfSurveyAnswer> answers = new ArrayList<>();
        answersCaptor.getValue().forEach(answers::add);
        assertThat(answers).hasSize(5);
        assertThat(answers).filteredOn(answer -> answer.getQuestionCode().equals("UNKNOWN_FEATURES"))
                .extracting(PmfSurveyAnswer::getAnswerCode)
                .containsExactlyInAnyOrder("OTHERS_FEED", "SEARCH_BOOKS");
        assertThat(answers).allSatisfy(answer -> assertThat(answer.getResponseId()).isEqualTo(100L));
        verify(eligibilityReader, never()).hasCoreActionSince(any(Long.class), any(Long.class), any(Instant.class));
    }

    @Test
    @DisplayName("몰랐던 기능 없음과 다른 기능을 함께 고르면 저장하지 않는다")
    void should_RejectExclusiveNoneOption_When_OtherUnknownFeaturesAreSelected() {
        when(responseRepository.existsByActorId(ACTOR_ID)).thenReturn(false);
        when(eligibilityTokenProvider.isValidFor("signed-token", ACTOR_ID)).thenReturn(true);

        PmfSurveySubmitRequest request = new PmfSurveySubmitRequest("signed-token", List.of(
                answer("PMF_CORE", "VERY_DISAPPOINTED"),
                answer("MOST_MISSED_FEATURE", "LIBRARY_AND_SHELVES"),
                new PmfSurveyAnswerRequest("UNKNOWN_FEATURES", List.of("NO_UNKNOWN_FEATURES", "SEARCH_BOOKS")),
                answer("RELAUNCH_INTENT", "YES")
        ));

        assertThatThrownBy(() -> pmfSurveyService.submit(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        verify(responseRepository, never()).saveAndFlush(any(PmfSurveyResponse.class));
        verify(answerRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("이미 제출한 사용자의 재제출을 거절한다")
    void should_RejectResubmission_When_ResponseAlreadyExists() {
        when(responseRepository.existsByActorId(ACTOR_ID)).thenReturn(true);

        assertThatThrownBy(() -> pmfSurveyService.submit(validRequest()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.PMF_SURVEY_ALREADY_RESPONDED));
        verify(eligibilityReader, never()).hasCoreActionSince(any(Long.class), any(Long.class), any(Instant.class));
    }

    private PmfSurveySubmitRequest validRequest() {
        return new PmfSurveySubmitRequest("signed-token", List.of(
                answer("PMF_CORE", "VERY_DISAPPOINTED"),
                answer("MOST_MISSED_FEATURE", "LIBRARY_AND_SHELVES"),
                new PmfSurveyAnswerRequest("UNKNOWN_FEATURES", List.of("OTHERS_FEED", "SEARCH_BOOKS")),
                answer("RELAUNCH_INTENT", "YES")
        ));
    }

    private PmfSurveyAnswerRequest answer(String questionCode, String answerCode) {
        return new PmfSurveyAnswerRequest(questionCode, List.of(answerCode));
    }
}
