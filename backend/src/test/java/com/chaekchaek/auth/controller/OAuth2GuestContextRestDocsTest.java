package com.chaekchaek.auth.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.auth.oauth.OAuthFrontendRedirectResolver;
import com.chaekchaek.auth.oauth.OAuthGuestContextService;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = OAuth2LoginController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class
)
@AutoConfigureRestDocs
class OAuth2GuestContextRestDocsTest {

    private static final String AUTH_TAG = "인증";
    private static final org.springframework.restdocs.headers.HeaderDescriptor LOCATION_HEADER =
            headerWithName(HttpHeaders.LOCATION).description("Google OAuth 인증 시작 경로");
    private static final FieldDescriptor[] PROBLEM_DETAIL_FIELDS = {
            fieldWithPath("type").type(JsonFieldType.STRING).description("문제 유형 URI"),
            fieldWithPath("title").type(JsonFieldType.STRING).description("HTTP 상태 설명"),
            fieldWithPath("status").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
            fieldWithPath("detail").type(JsonFieldType.STRING).description("오류 상세 메시지"),
            fieldWithPath("instance").type(JsonFieldType.STRING).description("오류 요청 경로").optional(),
            fieldWithPath("code").type(JsonFieldType.STRING).description("클라이언트 분기에 사용할 오류 코드")
    };

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OAuthFrontendRedirectResolver redirectResolver;

    @MockitoBean
    private OAuthGuestContextService guestContextService;

    @Test
    @DisplayName("웹 OAuth 로그인 전에 게스트 컨텍스트를 세션에 저장한다")
    void should_RememberGuestContextBeforeWebOAuthLogin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/oauth2/guest-context")
                        .header("X-Guest-Token", "guest-token"))
                .andExpect(status().isNoContent())
                .andDo(document("oauth2-guest-context",
                        requestHeaders(headerWithName("X-Guest-Token")
                                .description("OAuth 로그인 후 기록 이전에 사용할 현재 게스트 토큰")),
                        resource(ResourceSnippetParameters.builder()
                                .summary("웹 OAuth 게스트 컨텍스트 저장")
                                .description("브라우저가 OAuth 링크로 이동하기 전에 게스트 토큰을 검증하고 로그인 세션에 게스트 Actor를 임시 저장한다")
                                .tag(AUTH_TAG)
                                .requestHeaders(com.epages.restdocs.apispec.ResourceDocumentation
                                        .headerWithName("X-Guest-Token")
                                        .description("OAuth 로그인 후 기록 이전에 사용할 현재 게스트 토큰"))
                                .build())));
    }

    @Test
    @DisplayName("유효하지 않은 게스트 토큰이면 OAuth 컨텍스트 저장을 거부한다")
    void should_RejectGuestContext_When_GuestTokenIsInvalid() throws Exception {
        doThrow(new BusinessException(ErrorCode.INVALID_GUEST_TOKEN))
                .when(guestContextService).rememberGuestActor(any(), anyString());

        mockMvc.perform(post("/api/v1/auth/oauth2/guest-context")
                        .header("X-Guest-Token", "invalid-token"))
                .andExpect(status().isUnauthorized())
                .andDo(document("oauth2-guest-context-invalid-token",
                        requestHeaders(headerWithName("X-Guest-Token")
                                .description("검증할 게스트 토큰")),
                        responseFields(PROBLEM_DETAIL_FIELDS),
                        resource(ResourceSnippetParameters.builder()
                                .summary("웹 OAuth 게스트 컨텍스트 저장 실패")
                                .description("유효하지 않거나 만료된 게스트 토큰은 OAuth 로그인 컨텍스트로 저장하지 않는다")
                                .tag(AUTH_TAG)
                                .requestHeaders(com.epages.restdocs.apispec.ResourceDocumentation
                                        .headerWithName("X-Guest-Token").description("검증할 게스트 토큰"))
                                .responseFields(PROBLEM_DETAIL_FIELDS)
                .build())));
    }

    @Test
    @DisplayName("허용된 프론트 클라이언트의 웹 Google OAuth 로그인을 시작한다")
    void should_RedirectToGoogleOAuth_When_ClientIsAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/auth/oauth2/google")
                        .queryParam("client", "dev"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "/oauth2/authorization/google"))
                .andDo(document("oauth2-google-login",
                        queryParameters(parameterWithName("client")
                                .description("OAuth 로그인 완료 후 돌아갈 프론트 환경. local 또는 dev")),
                        responseHeaders(LOCATION_HEADER),
                        resource(ResourceSnippetParameters.builder()
                                .summary("웹 Google OAuth 로그인 시작")
                                .description("허용된 프론트 환경을 세션에 저장하고 Google OAuth 인증으로 이동한다")
                                .tag(AUTH_TAG)
                                .queryParameters(com.epages.restdocs.apispec.ResourceDocumentation
                                        .parameterWithName("client")
                                        .description("OAuth 로그인 완료 후 돌아갈 프론트 환경. local 또는 dev"))
                                .responseHeaders(LOCATION_HEADER)
                                .build())));
    }

    @Test
    @DisplayName("허용되지 않은 프론트 클라이언트의 웹 Google OAuth 로그인을 거부한다")
    void should_RejectGoogleOAuth_When_ClientIsNotAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/auth/oauth2/google")
                        .queryParam("client", "attacker"))
                .andExpect(status().isBadRequest())
                .andDo(document("oauth2-google-login-invalid-client",
                        queryParameters(parameterWithName("client")
                                .description("OAuth 로그인 완료 후 돌아갈 프론트 환경")),
                        resource(ResourceSnippetParameters.builder()
                                .summary("웹 Google OAuth 로그인 시작 실패")
                                .description("허용 목록에 없는 프론트 환경은 OAuth 인증을 시작할 수 없다")
                                .tag(AUTH_TAG)
                                .queryParameters(com.epages.restdocs.apispec.ResourceDocumentation
                                        .parameterWithName("client")
                                        .description("OAuth 로그인 완료 후 돌아갈 프론트 환경"))
                                .build())));
    }
}
