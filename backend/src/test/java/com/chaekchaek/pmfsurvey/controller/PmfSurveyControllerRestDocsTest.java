package com.chaekchaek.pmfsurvey.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.pmfsurvey.domain.PmfSurveyQuestionCatalog;
import com.chaekchaek.pmfsurvey.dto.PmfSurveyAnswerCountResponse;
import com.chaekchaek.pmfsurvey.dto.PmfSurveyAvailabilityResponse;
import com.chaekchaek.pmfsurvey.dto.PmfSurveyQuestionSummaryResponse;
import com.chaekchaek.pmfsurvey.dto.PmfSurveySubmissionResponse;
import com.chaekchaek.pmfsurvey.dto.PmfSurveySummaryResponse;
import com.chaekchaek.pmfsurvey.service.PmfSurveyService;
import com.epages.restdocs.apispec.ResourceDocumentation;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = {PmfSurveyController.class, AdminPmfSurveyController.class},
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class
)
@AutoConfigureRestDocs
class PmfSurveyControllerRestDocsTest {

    private static final String SURVEY_PATH = "/api/v1/pmf-survey";
    private static final String RESPONSES_PATH = "/api/v1/pmf-survey/responses";
    private static final String ADMIN_RESPONSES_PATH = "/api/v1/admin/pmf-survey/responses";
    private static final String TAG = "PMF 설문";

    private static final FieldDescriptor[] AVAILABILITY_FIELDS = {
            fieldWithPath("eligible").type(JsonFieldType.BOOLEAN).description("최근 14일 핵심 행동 및 팀 제외 기준을 만족하는지 여부"),
            fieldWithPath("alreadySubmitted").type(JsonFieldType.BOOLEAN).description("이미 응답을 제출했는지 여부"),
            fieldWithPath("submissionToken").type(JsonFieldType.STRING)
                    .description("조회 시점의 대상 여부를 증명하는 actor별 서명 토큰, 대상이 아니거나 이미 응답했다면 null"),
            fieldWithPath("questions").type(JsonFieldType.ARRAY).description("응답 가능한 문항 목록, 대상이 아니거나 이미 제출했다면 빈 배열"),
            fieldWithPath("questions[].questionCode").type(JsonFieldType.STRING).description("문항 코드"),
            fieldWithPath("questions[].question").type(JsonFieldType.STRING).description("화면에 표시할 질문"),
            fieldWithPath("questions[].multipleSelection").type(JsonFieldType.BOOLEAN).description("복수 선택 문항 여부"),
            fieldWithPath("questions[].options").type(JsonFieldType.ARRAY).description("문항 선택지"),
            fieldWithPath("questions[].options[].answerCode").type(JsonFieldType.STRING).description("제출할 선택지 코드"),
            fieldWithPath("questions[].options[].label").type(JsonFieldType.STRING).description("화면에 표시할 선택지")
    };

    private static final FieldDescriptor[] SUBMIT_REQUEST_FIELDS = {
            fieldWithPath("submissionToken").type(JsonFieldType.STRING)
                    .description("설문 조회 API가 현재 사용자에게 발급한 제출용 서명 토큰"),
            fieldWithPath("answers").type(JsonFieldType.ARRAY).description("문항별 선택 답변 목록"),
            fieldWithPath("answers[].questionCode").type(JsonFieldType.STRING).description("문항 코드"),
            fieldWithPath("answers[].answerCodes").type(JsonFieldType.ARRAY)
                    .description("선택한 답변 코드 목록, 단일 선택 문항은 하나만 전달")
    };

    private static final FieldDescriptor[] SUBMISSION_FIELDS = {
            fieldWithPath("submittedAt").type(JsonFieldType.STRING).description("서버가 기록한 제출 시각")
    };

    private static final FieldDescriptor[] SUMMARY_FIELDS = {
            fieldWithPath("respondentCount").type(JsonFieldType.NUMBER).description("응답자 수"),
            fieldWithPath("questions").type(JsonFieldType.ARRAY).description("문항별 선택지 집계"),
            fieldWithPath("questions[].questionCode").type(JsonFieldType.STRING).description("문항 코드"),
            fieldWithPath("questions[].question").type(JsonFieldType.STRING).description("문항 내용"),
            fieldWithPath("questions[].answerCounts").type(JsonFieldType.ARRAY).description("선택지별 응답자 수"),
            fieldWithPath("questions[].answerCounts[].answerCode").type(JsonFieldType.STRING).description("선택지 코드"),
            fieldWithPath("questions[].answerCounts[].label").type(JsonFieldType.STRING).description("선택지 문구"),
            fieldWithPath("questions[].answerCounts[].respondentCount").type(JsonFieldType.NUMBER)
                    .description("해당 선택지를 고른 응답자 수")
    };

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PmfSurveyService pmfSurveyService;

    @Test
    @DisplayName("응답 가능한 PMF 문항과 선택지를 조회하고 문서화한다")
    void should_ReturnSurvey_When_ActorIsEligible() throws Exception {
        when(pmfSurveyService.getSurvey()).thenReturn(new PmfSurveyAvailabilityResponse(
                true,
                false,
                "signed-pmf-eligibility-token",
                PmfSurveyQuestionCatalog.questions()
        ));

        mockMvc.perform(get(SURVEY_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.alreadySubmitted").value(false))
                .andExpect(jsonPath("$.questions[0].questionCode").value("PMF_CORE"))
                .andExpect(jsonPath("$.questions[2].multipleSelection").value(true))
                .andDo(document(
                        "pmf-survey-get",
                        responseFields(AVAILABILITY_FIELDS),
                        resource(ResourceSnippetParameters.builder()
                                .summary("PMF 설문 조회")
                                .description("현재 사용자의 최근 14일 핵심 행동과 팀 제외 여부, 기존 응답 여부를 확인한다. 응답 가능한 사용자는 문항, 선택지, actor에 묶인 제출용 토큰을 받는다. 제출 시 핵심 행동 기간을 다시 확인하지 않는다.")
                                .tag(TAG)
                                .responseSchema(Schema.schema("PmfSurveyAvailabilityResponse"))
                                .responseFields(AVAILABILITY_FIELDS)
                                .build())
                ));
    }

    @Test
    @DisplayName("PMF 설문 답변을 제출하고 문서화한다")
    void should_SubmitSurveyAnswers_When_RequestIsValid() throws Exception {
        Instant submittedAt = Instant.parse("2026-11-05T03:00:00Z");
        when(pmfSurveyService.submit(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PmfSurveySubmissionResponse(submittedAt));

        mockMvc.perform(post(RESPONSES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": [
                                    {"questionCode":"PMF_CORE","answerCodes":["VERY_DISAPPOINTED"]},
                                    {"questionCode":"MOST_MISSED_FEATURE","answerCodes":["LIBRARY_AND_SHELVES"]},
                                    {"questionCode":"UNKNOWN_FEATURES","answerCodes":["OTHERS_FEED","SEARCH_BOOKS"]},
                                    {"questionCode":"RELAUNCH_INTENT","answerCodes":["YES"]}
                                  ],
                                  "submissionToken":"signed-pmf-eligibility-token"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.submittedAt").value("2026-11-05T03:00:00Z"))
                .andDo(document(
                        "pmf-survey-submit",
                        requestFields(SUBMIT_REQUEST_FIELDS),
                        responseFields(SUBMISSION_FIELDS),
                        resource(ResourceSnippetParameters.builder()
                                .summary("PMF 설문 응답 제출")
                                .description("설문 조회에서 받은 제출용 토큰으로 조회 당시의 대상 자격을 확인하고, 인증된 사용자의 actorId를 서버에서 연결해 답변을 저장한다. 조회 후 핵심 행동의 14일 기간이 지나도 제출할 수 있다. 복수 선택 문항은 answerCodes에 여러 코드를 전달한다.")
                                .tag(TAG)
                                .requestSchema(Schema.schema("PmfSurveySubmitRequest"))
                                .requestFields(SUBMIT_REQUEST_FIELDS)
                                .responseSchema(Schema.schema("PmfSurveySubmissionResponse"))
                                .responseFields(SUBMISSION_FIELDS)
                                .build())
                ));
    }

    @Test
    @WithMockUser
    @DisplayName("관리자는 문항별 응답 집계를 조회하고 문서화한다")
    void should_ReturnSurveySummary_When_AdminRequestsAggregation() throws Exception {
        when(pmfSurveyService.getSummary()).thenReturn(summary());

        mockMvc.perform(get(ADMIN_RESPONSES_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respondentCount").value(2))
                .andExpect(jsonPath("$.questions[0].answerCounts[0].respondentCount").value(1))
                .andDo(document(
                        "pmf-survey-admin-summary",
                        responseFields(SUMMARY_FIELDS),
                        resource(ResourceSnippetParameters.builder()
                                .summary("PMF 설문 응답 집계 조회")
                                .description("관리자가 응답자 총수와 각 문항의 선택지별 응답자 수를 조회한다. 선택되지 않은 선택지도 0으로 반환한다.")
                                .tag(TAG)
                                .responseSchema(Schema.schema("PmfSurveySummaryResponse"))
                                .responseFields(SUMMARY_FIELDS)
                                .build())
                ));
    }

    private PmfSurveySummaryResponse summary() {
        List<PmfSurveyQuestionSummaryResponse> questions = PmfSurveyQuestionCatalog.questions()
                .stream()
                .map(question -> new PmfSurveyQuestionSummaryResponse(
                        question.questionCode(),
                        question.question(),
                        question.options().stream()
                                .map(option -> new PmfSurveyAnswerCountResponse(
                                        option.answerCode(),
                                        option.label(),
                                        option.answerCode().equals("VERY_DISAPPOINTED") ? 1 : 0
                                ))
                                .toList()
                ))
                .toList();
        return new PmfSurveySummaryResponse(2, questions);
    }
}
