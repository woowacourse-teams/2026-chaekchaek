package com.chaekchaek.auth.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.auth.token.guest.GuestTokenService;
import com.chaekchaek.auth.token.guest.IssuedGuestToken;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.payload.FieldDescriptor;

@WebMvcTest(value = GuestAuthController.class, excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class)
@AutoConfigureRestDocs
class GuestAuthControllerTest {

    private static final int TOKEN_EXPIRATION_DAYS = 90;
    private static final int REFRESH_WINDOW_DAYS = 14;
    private static final LocalDateTime TOKEN_ISSUED_AT =
            LocalDateTime.of(2026, 8, 26, 9, 0);
    private static final LocalDateTime TOKEN_EXPIRES_AT =
            TOKEN_ISSUED_AT.plusDays(TOKEN_EXPIRATION_DAYS);
    private static final String TOKEN_EXPIRES_AT_RESPONSE =
            TOKEN_EXPIRES_AT.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

    private static final FieldDescriptor[] TOKEN_RESPONSE_FIELDS = {
            fieldWithPath("guestToken").type(JsonFieldType.STRING)
                    .description("게스트 공개 상호작용 인증에 사용할 원본 토큰. 응답 시 한 번만 제공"),
            fieldWithPath("nickname").type(JsonFieldType.STRING)
                    .description("서버가 할당한 게스트 작성자 닉네임"),
            fieldWithPath("expiresAt").type(JsonFieldType.STRING)
                    .description("게스트 토큰 만료 시각(UTC). 발급 또는 갱신 시점부터 %d일"
                            .formatted(TOKEN_EXPIRATION_DAYS))
    };
    private static final FieldDescriptor[] PROBLEM_DETAIL_FIELDS = {
            fieldWithPath("type").type(JsonFieldType.STRING).description("문제 유형 URI"),
            fieldWithPath("title").type(JsonFieldType.STRING).description("HTTP 상태 설명"),
            fieldWithPath("status").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
            fieldWithPath("detail").type(JsonFieldType.STRING).description("오류 상세 메시지"),
            fieldWithPath("instance").type(JsonFieldType.STRING).description("오류가 발생한 요청 경로").optional(),
            fieldWithPath("code").type(JsonFieldType.STRING).description("클라이언트 분기에 사용할 오류 코드")
    };

    @Autowired MockMvc mockMvc;
    @MockitoBean GuestTokenService guestTokenService;

    @Test
    void issuesGuestTokenWithoutLogin() throws Exception {
        when(guestTokenService.issue()).thenReturn(new IssuedGuestToken(
                "guest-token", "다정한 파란 참새", TOKEN_EXPIRES_AT));

        mockMvc.perform(post("/api/v1/auth/guest-token"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.guestToken").value("guest-token"))
                .andExpect(jsonPath("$.nickname").value("다정한 파란 참새"))
                .andExpect(jsonPath("$.expiresAt").value(TOKEN_EXPIRES_AT_RESPONSE))
                .andDo(document("guest-token-issue",
                        responseFields(TOKEN_RESPONSE_FIELDS),
                        resource(ResourceSnippetParameters.builder()
                                .summary("게스트 토큰 발급")
                                .description("로그인 없이 공개 상호작용에 사용할 %d일 유효 게스트 토큰과 닉네임을 발급한다. 저장된 토큰이 없을 때만 호출한다"
                                        .formatted(TOKEN_EXPIRATION_DAYS))
                                .tag("인증")
                                .responseFields(TOKEN_RESPONSE_FIELDS)
                                .build())));
    }

    @Test
    void refreshesGuestTokenWithinRefreshWindow() throws Exception {
        when(guestTokenService.refresh("current-token")).thenReturn(new IssuedGuestToken(
                "new-token", "다정한 파란 참새", TOKEN_EXPIRES_AT));

        mockMvc.perform(post("/api/v1/auth/guest-token/refresh")
                        .header("X-Guest-Token", "current-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guestToken").value("new-token"))
                .andExpect(jsonPath("$.nickname").value("다정한 파란 참새"))
                .andExpect(jsonPath("$.expiresAt").value(TOKEN_EXPIRES_AT_RESPONSE))
                .andDo(document("guest-token-refresh",
                        requestHeaders(headerWithName("X-Guest-Token")
                                .description("교체할 현재 게스트 토큰")),
                        responseFields(TOKEN_RESPONSE_FIELDS),
                        resource(ResourceSnippetParameters.builder()
                                .summary("게스트 토큰 갱신")
                                .description("만료까지 %d일 이하로 남은 유효한 토큰을 같은 Actor의 새 %d일 토큰으로 교체한다. 기존 토큰은 즉시 무효화된다"
                                        .formatted(REFRESH_WINDOW_DAYS, TOKEN_EXPIRATION_DAYS))
                                .tag("인증")
                                .requestHeaders(com.epages.restdocs.apispec.ResourceDocumentation
                                        .headerWithName("X-Guest-Token").description("교체할 현재 게스트 토큰"))
                                .responseFields(TOKEN_RESPONSE_FIELDS)
                                .build())));
    }

    @Test
    void documentsInvalidGuestTokenRefresh() throws Exception {
        doThrow(new BusinessException(ErrorCode.INVALID_GUEST_TOKEN))
                .when(guestTokenService).refresh(anyString());

        documentRefreshProblem("invalid-token", "guest-token-refresh-invalid", 401,
                "INVALID_GUEST_TOKEN", "유효하지 않은 게스트 토큰이면 인증 오류를 반환한다");
    }

    @Test
    void documentsExpiredGuestTokenRefresh() throws Exception {
        doThrow(new BusinessException(ErrorCode.UNUSABLE_GUEST_TOKEN))
                .when(guestTokenService).refresh(anyString());

        documentRefreshProblem("expired-token", "guest-token-refresh-expired", 401,
                "UNUSABLE_GUEST_TOKEN", "만료되거나 폐기된 게스트 토큰은 갱신할 수 없다");
    }

    @Test
    void documentsGuestTokenRefreshBeforeWindow() throws Exception {
        doThrow(new BusinessException(ErrorCode.GUEST_TOKEN_REFRESH_NOT_ALLOWED))
                .when(guestTokenService).refresh(anyString());

        documentRefreshProblem("early-token", "guest-token-refresh-not-allowed", 409,
                "GUEST_TOKEN_REFRESH_NOT_ALLOWED",
                "만료까지 %d일보다 많이 남은 토큰은 갱신할 수 없다".formatted(REFRESH_WINDOW_DAYS));
    }

    private void documentRefreshProblem(String token, String identifier, int statusCode,
                                        String code, String description) throws Exception {
        mockMvc.perform(post("/api/v1/auth/guest-token/refresh")
                        .header("X-Guest-Token", token))
                .andExpect(status().is(statusCode))
                .andExpect(jsonPath("$.code").value(code))
                .andDo(document(identifier,
                        requestHeaders(headerWithName("X-Guest-Token").description("갱신할 현재 게스트 토큰")),
                        responseFields(PROBLEM_DETAIL_FIELDS),
                        resource(ResourceSnippetParameters.builder()
                                .summary("게스트 토큰 갱신")
                                .description(description)
                                .tag("인증")
                                .requestHeaders(com.epages.restdocs.apispec.ResourceDocumentation
                                        .headerWithName("X-Guest-Token").description("갱신할 현재 게스트 토큰"))
                                .responseFields(PROBLEM_DETAIL_FIELDS)
                                .build())));
    }
}
