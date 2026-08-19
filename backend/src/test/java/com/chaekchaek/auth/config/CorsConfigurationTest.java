package com.chaekchaek.auth.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CorsConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("로컬 프론트 Origin의 Preflight 요청은 허용된다")
    void should_Allow_PreflightRequest_When_LocalhostFrontendOriginIsAllowed() throws Exception {
        mockMvc.perform(options("/api/v1/members/me")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"
                        ))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"
                ))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"
                ));

        mockMvc.perform(options("/api/v1/members/me")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type, Authorization"
                        ))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"
                ))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"
                ));
    }

    @Test
    @DisplayName("프로덕션 프론트 Origin의 Preflight 요청은 허용된다")
    void should_Allow_PreflightRequest_When_ProductionFrontendOriginIsAllowed() throws Exception {
        mockMvc.perform(options("/api/v1/members/me")
                        .header(HttpHeaders.ORIGIN, "https://chaekchaek.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type, Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://chaekchaek.com"
                ))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"
                ));
    }

    @Test
    @DisplayName("등록되지 않은 Origin의 Preflight 요청은 거부된다")
    void should_Reject_PreflightRequest_When_UnregisteredFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/members/me")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"
                        ))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN
                ));
    }
}
