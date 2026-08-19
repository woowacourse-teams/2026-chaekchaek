package com.chaekchaek.auth.config;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ForwardedHeaderOAuth2AuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("프록시를 거친 HTTPS OAuth 요청은 HTTPS 콜백 주소를 사용한다")
    void should_UseHttpsRedirectUri_When_RequestComesThroughHttpsProxy()
            throws Exception {
        // given
        MockHttpServletRequestBuilder request = get("/oauth2/authorization/google")
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Host", "api.chaekchaek.com")
                .header("X-Forwarded-Port", "443");

        // when & then
        mockMvc.perform(request)
                .andExpect(status().isFound())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        containsString(
                                "redirect_uri=https://api.chaekchaek.com/login/oauth2/code/google"
                        )
                ));
    }
}
