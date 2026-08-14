package com.chaekchaek.auth.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.auth.dto.MobileTokenResponse;
import com.chaekchaek.auth.service.MobileGoogleLoginService;
import com.chaekchaek.common.exception.ApiExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class MobileAuthControllerTest {

    private MobileGoogleLoginService mobileGoogleLoginService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mobileGoogleLoginService =
                org.mockito.Mockito.mock(
                        MobileGoogleLoginService.class
                );

        LocalValidatorFactoryBean validator =
                new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new MobileAuthController(
                                mobileGoogleLoginService
                        )
                )
                .setControllerAdvice(
                        new ApiExceptionHandler()
                )
                .setValidator(validator)
                .build();
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
                        .value(1_209_600));
    }

    @Test
    @DisplayName("Google ID Token이 비어 있으면 요청을 거부한다")
    void should_ReturnBadRequest_When_IdTokenIsBlank()
            throws Exception {
        mockMvc.perform(post(
                        "/api/v1/auth/mobile/google"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idToken": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST"));
    }
}