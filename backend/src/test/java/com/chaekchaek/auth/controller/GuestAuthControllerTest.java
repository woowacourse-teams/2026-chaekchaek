package com.chaekchaek.auth.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.auth.token.guest.GuestTokenService;
import com.chaekchaek.auth.token.guest.IssuedGuestToken;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.restdocs.payload.JsonFieldType;

@WebMvcTest(value = GuestAuthController.class, excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class)
@AutoConfigureRestDocs
class GuestAuthControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean GuestTokenService guestTokenService;

    @Test
    void issuesGuestTokenWithoutLogin() throws Exception {
        when(guestTokenService.issue()).thenReturn(new IssuedGuestToken(
                "guest-token", "다정한 파란 참새", LocalDateTime.of(2026, 9, 25, 9, 0)));

        mockMvc.perform(post("/api/v1/auth/guest-token"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.guestToken").value("guest-token"))
                .andExpect(jsonPath("$.nickname").value("다정한 파란 참새"))
                .andExpect(jsonPath("$.expiresAt").value("2026-09-25T09:00:00"))
                .andDo(document("guest-token-issue",
                        responseFields(
                                fieldWithPath("guestToken").type(JsonFieldType.STRING)
                                        .description("게스트 공개 상호작용 인증에 사용할 토큰"),
                                fieldWithPath("nickname").type(JsonFieldType.STRING)
                                        .description("서버가 할당한 게스트 작성자 닉네임"),
                                fieldWithPath("expiresAt").type(JsonFieldType.STRING)
                                        .description("게스트 토큰 만료 시각(UTC)")),
                        resource(ResourceSnippetParameters.builder()
                                .summary("게스트 토큰 발급")
                                .description("로그인 없이 공개 상호작용에 사용할 게스트 토큰과 닉네임을 발급한다")
                                .tag("인증")
                                .responseFields(
                                        fieldWithPath("guestToken").type(JsonFieldType.STRING)
                                                .description("게스트 공개 상호작용 인증에 사용할 토큰"),
                                        fieldWithPath("nickname").type(JsonFieldType.STRING)
                                                .description("서버가 할당한 게스트 작성자 닉네임"),
                                        fieldWithPath("expiresAt").type(JsonFieldType.STRING)
                                                .description("게스트 토큰 만료 시각(UTC)"))
                                .build())));
    }
}
