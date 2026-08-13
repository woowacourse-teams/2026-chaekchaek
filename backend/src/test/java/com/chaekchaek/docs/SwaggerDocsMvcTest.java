package com.chaekchaek.docs;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SwaggerDocsMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Swagger UI 링크를 제공한다")
    void should_ServeSwaggerUi() throws Exception {
        mockMvc.perform(get("/docs/swagger-ui.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("./openapi3.yaml")))
                .andExpect(content().string(containsString(
                        "../webjars/swagger-ui/5.32.11/swagger-ui-bundle.js")))
                .andExpect(content().string(containsString(
                        "../webjars/swagger-ui/5.32.11/swagger-ui-standalone-preset.js")))
                .andExpect(content().string(containsString(
                        "../webjars/swagger-ui/5.32.11/swagger-ui.css")));
    }

    @Test
    @DisplayName("Swagger UI에서 GET 요청만 실행하고 외부 설정 및 자격 증명을 차단한다")
    void should_ApplyReadOnlySwaggerUiSecuritySettings() throws Exception {
        mockMvc.perform(get("/docs/swagger-ui.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("supportedSubmitMethods: ['get']")))
                .andExpect(content().string(containsString("queryConfigEnabled: false")))
                .andExpect(content().string(containsString("withCredentials: false")))
                .andExpect(content().string(containsString("request.credentials = 'omit'")))
                .andExpect(content().string(containsString("validatorUrl: null")));
    }

    @Test
    @DisplayName("Swagger UI가 읽을 OpenAPI 명세 링크를 제공한다")
    void should_ServeOpenApiSpecification() throws Exception {
        mockMvc.perform(get("/docs/openapi3.yaml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("openapi: 3.0.1")));
    }

    @Test
    @DisplayName("Swagger UI JavaScript 자산을 제공한다")
    void should_ServeSwaggerUiWebJarAsset() throws Exception {
        mockMvc.perform(get("/webjars/swagger-ui/5.32.11/swagger-ui-bundle.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(emptyString())));
    }
}
