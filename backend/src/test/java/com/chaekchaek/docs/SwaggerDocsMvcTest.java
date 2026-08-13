package com.chaekchaek.docs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
    @DisplayName("비루트 컨텍스트에서 Swagger UI와 상대 참조 명세를 제공한다")
    void should_ServeSwaggerDocsWithRelativeReferences_When_ContextPathIsNotRoot() throws Exception {
        URI swaggerUiUri = URI.create("http://localhost/context/docs/swagger-ui.html");

        MvcResult swaggerUiResult = mockMvc.perform(get(swaggerUiUri.getPath())
                        .contextPath("/context"))
                .andExpect(status().isOk())
                .andReturn();
        String swaggerUi = swaggerUiResult.getResponse().getContentAsString();
        assertThat(swaggerUi).contains("url: './openapi3.yaml'");

        URI openApiUri = swaggerUiUri.resolve("./openapi3.yaml");
        assertThat(openApiUri.getPath()).isEqualTo("/context/docs/openapi3.yaml");
        MvcResult openApiResult = mockMvc.perform(get(openApiUri.getPath())
                        .contextPath("/context"))
                .andExpect(status().isOk())
                .andReturn();
        String openApiSpecification = openApiResult.getResponse().getContentAsString();

        Matcher serverUrlMatcher = Pattern.compile("(?m)^\\s*- url: (\\S+)\\s*$")
                .matcher(openApiSpecification);
        assertThat(serverUrlMatcher.find()).isTrue();
        URI applicationRootUri = openApiUri.resolve(serverUrlMatcher.group(1));
        assertThat(applicationRootUri.getPath()).isEqualTo("/context/");
    }

    @Test
    @DisplayName("Swagger UI JavaScript 자산을 제공한다")
    void should_ServeSwaggerUiWebJarAsset() throws Exception {
        mockMvc.perform(get("/webjars/swagger-ui/5.32.11/swagger-ui-bundle.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(emptyString())));
    }
}
