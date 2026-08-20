package com.chaekchaek.auth.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.auth.dto.MobileTokenResponse;
import com.chaekchaek.auth.service.MobileAuthTokenService;
import com.chaekchaek.auth.service.MobileAppleLoginService;
import com.chaekchaek.auth.service.MobileGoogleLoginService;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(
        value = MobileAuthController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class
)
@AutoConfigureRestDocs
class MobileAuthControllerTest {

    private static final String AUTH_TAG = "인증";
    private static final FieldDescriptor[] GOOGLE_LOGIN_REQUEST_FIELDS = {
            fieldWithPath("idToken").type(JsonFieldType.STRING)
                    .description("Google SDK에서 발급받은 ID Token")
    };
    private static final FieldDescriptor[] APPLE_LOGIN_REQUEST_FIELDS = {
            fieldWithPath("identityToken").type(JsonFieldType.STRING)
                    .description("Apple에서 발급받은 Identity Token"),
            fieldWithPath("authorizationCode").type(JsonFieldType.STRING)
                    .description("Apple에서 발급받은 일회용 Authorization Code"),
            fieldWithPath("nonce").type(JsonFieldType.STRING)
                    .description("Apple 로그인 요청에 사용한 해시 전 원본 nonce")
    };
    private static final FieldDescriptor[] REFRESH_TOKEN_REQUEST_FIELDS = {
            fieldWithPath("refreshToken").type(JsonFieldType.STRING)
                    .description("재발급 또는 로그아웃할 Refresh Token")
    };
    private static final FieldDescriptor[] TOKEN_RESPONSE_FIELDS = {
            fieldWithPath("accessToken").type(JsonFieldType.STRING)
                    .description("API 호출에 사용할 Access Token"),
            fieldWithPath("refreshToken").type(JsonFieldType.STRING)
                    .description("Access Token 재발급에 사용할 Refresh Token"),
            fieldWithPath("tokenType").type(JsonFieldType.STRING)
                    .description("Access Token 인증 타입. Bearer"),
            fieldWithPath("accessTokenExpiresIn").type(JsonFieldType.NUMBER)
                    .description("Access Token 만료까지 남은 시간(초)"),
            fieldWithPath("refreshTokenExpiresIn").type(JsonFieldType.NUMBER)
                    .description("Refresh Token 만료까지 남은 시간(초)")
    };
    private static final FieldDescriptor[] PROBLEM_DETAIL_FIELDS = {
            fieldWithPath("type").type(JsonFieldType.STRING)
                    .description("현재 about:blank로 고정되는 문제 유형 URI"),
            fieldWithPath("title").type(JsonFieldType.STRING)
                    .description("HTTP 상태 설명"),
            fieldWithPath("status").type(JsonFieldType.NUMBER)
                    .description("HTTP 상태 코드"),
            fieldWithPath("detail").type(JsonFieldType.STRING)
                    .description("오류 상세 메시지. 클라이언트 분기에는 사용하지 않는다"),
            fieldWithPath("instance").type(JsonFieldType.STRING)
                    .description("오류가 발생한 요청 경로"),
            fieldWithPath("code").type(JsonFieldType.STRING)
                    .description("클라이언트 오류 분기에 사용하는 애플리케이션 오류 코드")
    };

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MobileGoogleLoginService mobileGoogleLoginService;

    @MockitoBean
    private MobileAppleLoginService mobileAppleLoginService;

    @MockitoBean
    private MobileAuthTokenService mobileAuthTokenService;

    @Test
    @DisplayName("유효한 Apple 인증 정보로 모바일 로그인한다")
    void should_ReturnTokens_When_AppleLoginSucceeds() throws Exception {
        MobileTokenResponse response = new MobileTokenResponse(
                "access-token", "refresh-token", "Bearer", 1_800, 1_209_600
        );
        when(mobileAppleLoginService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/mobile/apple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identityToken": "apple-identity-token",
                                  "authorizationCode": "apple-authorization-code",
                                  "nonce": "raw-nonce"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andDo(document(
                        "mobile-apple-login",
                        requestFields(APPLE_LOGIN_REQUEST_FIELDS),
                        responseFields(TOKEN_RESPONSE_FIELDS),
                        resource(ResourceSnippetParameters.builder()
                                .summary("모바일 Apple 로그인")
                                .description("Apple 인증 정보로 자체 Access Token과 Refresh Token을 발급한다")
                                .tag(AUTH_TAG)
                                .requestFields(APPLE_LOGIN_REQUEST_FIELDS)
                                .responseFields(TOKEN_RESPONSE_FIELDS)
                                .build())));
    }

    @Test
    @DisplayName("Apple 인증 정보가 비어 있으면 요청을 거부한다")
    void should_ReturnBadRequest_When_AppleAuthorizationIsBlank() throws Exception {
        expectProblemDetail(mockMvc.perform(post("/api/v1/auth/mobile/apple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identityToken": "",
                                  "authorizationCode": "",
                                  "nonce": ""
                                }
                                """)),
                "INVALID_REQUEST", "/api/v1/auth/mobile/apple")
                .andDo(problemDetailDocument(
                        "mobile-apple-login-invalid-request",
                        "모바일 Apple 로그인",
                        "Apple 인증 필수값이 없거나 비어 있으면 요청을 거부한다",
                        APPLE_LOGIN_REQUEST_FIELDS));
    }

    @Test
    @DisplayName("유효한 Google ID Token으로 모바일 로그인한다")
    void should_ReturnTokens_When_GoogleLoginSucceeds()
            throws Exception {
        // given
        MobileTokenResponse response =
                new MobileTokenResponse(
                        "access-token",
                        "refresh-token",
                        "Bearer",
                        1_800,
                        1_209_600
                );

        when(mobileGoogleLoginService.login(
                "google-id-token"
        )).thenReturn(response);

        // when & then
        mockMvc.perform(post(
                        "/api/v1/auth/mobile/google"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idToken": "google-id-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken")
                        .value("access-token"))
                .andExpect(jsonPath("$.refreshToken")
                        .value("refresh-token"))
                .andExpect(jsonPath("$.tokenType")
                        .value("Bearer"))
                .andExpect(jsonPath("$.accessTokenExpiresIn")
                        .value(1_800))
                .andExpect(jsonPath("$.refreshTokenExpiresIn")
                        .value(1_209_600))
                .andDo(document(
                        "mobile-google-login",
                        requestFields(GOOGLE_LOGIN_REQUEST_FIELDS),
                        responseFields(TOKEN_RESPONSE_FIELDS),
                        resource(ResourceSnippetParameters.builder()
                                .summary("모바일 Google 로그인")
                                .description("Google SDK에서 받은 ID Token으로 모바일 Access Token과 Refresh Token을 발급한다. 만료 값의 단위는 초다")
                                .tag(AUTH_TAG)
                                .requestFields(GOOGLE_LOGIN_REQUEST_FIELDS)
                                .responseFields(TOKEN_RESPONSE_FIELDS)
                                .build())));
    }

    @Test
    @DisplayName("Google ID Token이 비어 있으면 요청을 거부한다")
    void should_ReturnBadRequest_When_IdTokenIsBlank()
            throws Exception {
        expectProblemDetail(mockMvc.perform(post(
                        "/api/v1/auth/mobile/google"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idToken": ""
                                }
                                """)),
                "INVALID_REQUEST", "/api/v1/auth/mobile/google")
                .andDo(problemDetailDocument(
                        "mobile-google-login-invalid-request",
                        "모바일 Google 로그인",
                        "Google ID Token이 없거나 비어 있으면 요청을 거부한다",
                        GOOGLE_LOGIN_REQUEST_FIELDS));
    }

    @Test
    @DisplayName("Refresh Token으로 모바일 토큰을 재발급한다")
    void should_ReturnNewTokens_When_ReissueSucceeds()
            throws Exception {
        // given
        MobileTokenResponse response =
                new MobileTokenResponse(
                        "new-access-token",
                        "new-refresh-token",
                        "Bearer",
                        1_800,
                        1_209_600
                );

        when(mobileAuthTokenService.reissue(
                "old-refresh-token"
        )).thenReturn(response);

        // when & then
        mockMvc.perform(post(
                        "/api/v1/auth/mobile/reissue"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "old-refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken")
                        .value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken")
                        .value("new-refresh-token"))
                .andDo(document(
                        "mobile-token-reissue",
                        requestFields(REFRESH_TOKEN_REQUEST_FIELDS),
                        responseFields(TOKEN_RESPONSE_FIELDS),
                        resource(ResourceSnippetParameters.builder()
                                .summary("모바일 토큰 재발급")
                                .description("Refresh Token으로 새 Access Token과 Refresh Token을 발급한다. 기존 Refresh Token은 재발급 후 폐기되며 만료 값의 단위는 초다")
                                .tag(AUTH_TAG)
                                .requestFields(REFRESH_TOKEN_REQUEST_FIELDS)
                                .responseFields(TOKEN_RESPONSE_FIELDS)
                                .build())));
    }

    @Test
    @DisplayName("Refresh Token이 비어 있으면 재발급 요청을 거부한다")
    void should_ReturnBadRequest_When_ReissueTokenIsBlank()
            throws Exception {
        expectProblemDetail(mockMvc.perform(post(
                        "/api/v1/auth/mobile/reissue"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "refreshToken": ""
                            }
                            """)),
                "INVALID_REQUEST", "/api/v1/auth/mobile/reissue")
                .andDo(problemDetailDocument(
                        "mobile-token-reissue-invalid-request",
                        "모바일 토큰 재발급",
                        "Refresh Token이 없거나 비어 있으면 요청을 거부한다",
                        REFRESH_TOKEN_REQUEST_FIELDS));
    }

    @Test
    @DisplayName("Refresh Token으로 모바일 로그아웃한다")
    void should_ReturnNoContent_When_LogoutSucceeds()
            throws Exception {
        mockMvc.perform(post(
                        "/api/v1/auth/mobile/logout"
                )
                .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "refreshToken": "refresh-token"
                            }
                            """))
                .andExpect(status().isNoContent())
                .andDo(document(
                        "mobile-logout",
                        requestFields(REFRESH_TOKEN_REQUEST_FIELDS),
                        resource(ResourceSnippetParameters.builder()
                                .summary("모바일 로그아웃")
                                .description("Refresh Token을 폐기한다. 클라이언트는 저장한 Access Token과 Refresh Token을 함께 삭제한다")
                                .tag(AUTH_TAG)
                                .requestFields(REFRESH_TOKEN_REQUEST_FIELDS)
                                .build())));

        verify(mobileAuthTokenService)
                .logout("refresh-token");
    }

    @Test
    @DisplayName("Refresh Token이 비어 있으면 로그아웃 요청을 거부한다")
    void should_ReturnBadRequest_When_LogoutTokenIsBlank()
            throws Exception {
        expectProblemDetail(mockMvc.perform(post(
                        "/api/v1/auth/mobile/logout"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "refreshToken": ""
                            }
                            """)),
                "INVALID_REQUEST", "/api/v1/auth/mobile/logout")
                .andDo(problemDetailDocument(
                        "mobile-logout-invalid-request",
                        "모바일 로그아웃",
                        "Refresh Token이 없거나 비어 있으면 요청을 거부한다",
                        REFRESH_TOKEN_REQUEST_FIELDS));
    }

    private ResultActions expectProblemDetail(
            ResultActions result,
            String code,
            String instance
    ) throws Exception {
        return result
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value(instance))
                .andExpect(jsonPath("$.code").value(code));
    }

    private org.springframework.test.web.servlet.ResultHandler problemDetailDocument(
            String identifier,
            String summary,
            String description,
            FieldDescriptor[] requestFields
    ) {
        return document(
                identifier,
                requestFields(requestFields),
                responseFields(PROBLEM_DETAIL_FIELDS),
                resource(ResourceSnippetParameters.builder()
                        .summary(summary)
                        .description(description)
                        .tag(AUTH_TAG)
                        .requestFields(requestFields)
                        .responseSchema(Schema.schema("ProblemDetail"))
                        .responseFields(PROBLEM_DETAIL_FIELDS)
                        .build()));
    }
}
