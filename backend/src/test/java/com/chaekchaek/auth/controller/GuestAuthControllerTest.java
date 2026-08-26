package com.chaekchaek.auth.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.auth.token.guest.GuestTokenService;
import com.chaekchaek.auth.token.guest.IssuedGuestToken;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = GuestAuthController.class, excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class)
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
                .andExpect(jsonPath("$.expiresAt").value("2026-09-25T09:00:00"));
    }
}
