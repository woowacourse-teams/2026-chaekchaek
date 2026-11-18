package com.chaekchaek.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.incident.alert.domain.IncidentReference;
import com.chaekchaek.incident.alert.repository.IncidentRepository;
import com.chaekchaek.incident.alert.service.IncidentService;
import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:webhook;DB_CLOSE_DELAY=-1",
        "incident.webhook-token=" + Fixtures.TOKEN,
        "incident.enabled-environments=dev"
})
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Import(WebhookIntegrationTest.TimeConfiguration.class)
class WebhookIntegrationTest {
    private static final String ENDPOINT = "/internal/alerts/grafana";
    @Autowired
    private MockMvc mvc;
    @Autowired
    private IncidentRepository jobs;
    @Autowired
    private IncidentService incidents;

    @BeforeEach
    void clearJobs() {
        jobs.deleteAll();
    }

    @Test
    @DisplayName("같은 장애를 다시 받아도 한 번만 접수한다")
    void should_PersistOnlyOnce_When_GrafanaRetries() throws Exception {
        // given
        String body = payload("firing", "dev", "api-errors");
        // when
        mvc.perform(post(ENDPOINT).header("Authorization", "Bearer " + Fixtures.TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted())
                .andDo(document("grafana-webhook"));
        send(body, 202);
        // then
        assertThat(jobs.count()).isEqualTo(1);
        assertThat(jobs.findAll()).singleElement().satisfies(incident ->
                assertThat(incident.getStartsAt()).isEqualTo(Fixtures.NOW.minusSeconds(120)));
    }

    @Test
    @DisplayName("복구 알림이 먼저 도착해도 뒤늦은 발생 알림이 복구 상태를 덮어쓰지 않는다")
    void should_KeepResolved_When_FiringArrivesLate() throws Exception {
        // given
        send(payload("resolved", "dev", "api-errors"), 202);
        // when
        send(payload("firing", "dev", "api-errors"), 202);
        // then
        assertThat(jobs.findAll()).singleElement().satisfies(job -> {
            assertThat(job.isResolved()).isTrue();
        });
    }

    @Test
    @DisplayName("복구된 장애도 발생 시각을 유지하여 이후 질문 대상으로 조회한다")
    void should_KeepIncidentAvailable_When_ResolvedBeforeQuestion() throws Exception {
        // given
        send(payload("firing", "dev", "api-errors"), 202);
        send(payload("resolved", "dev", "api-errors"), 202);
        // when
        var incident = incidents.find(new IncidentReference(
                "dev",
                "abc123",
                Fixtures.NOW.minusSeconds(120)
        ));
        // then
        assertThat(incident).get().satisfies(snapshot -> {
            assertThat(snapshot.resolved()).isTrue();
            assertThat(snapshot.startsAt()).isEqualTo(Fixtures.NOW.minusSeconds(120));
        });
    }

    @Test
    @DisplayName("비활성 환경과 허용하지 않은 분석 프로필은 접수하지 않는다")
    void should_IgnoreAlert_When_EnvironmentOrProfileIsDisabled() throws Exception {
        // when
        send(payload("firing", "prod", "api-errors"), 202);
        send(payload("firing", "dev", "unknown"), 202);
        send(payload("firing", "dev", "api-errors").replace("\"analysis_profile\":\"api-errors\",", ""), 202);
        // then
        assertThat(jobs.count()).isZero();
    }

    @Test
    @DisplayName("잘못된 인증은 본문 파싱 전에 거절한다")
    void should_RejectBeforeParsing_When_TokenIsInvalid() throws Exception {
        // when, then
        mvc.perform(post(ENDPOINT).header("Authorization", "Bearer invalid").content("invalid"))
                .andExpect(status().isUnauthorized());
        assertThat(jobs.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}", "{\"orgId\":1,\"alerts\":[null]}", "invalid"})
    @DisplayName("잘못된 입력은 저장하지 않는다")
    void should_RejectMalformedInput_When_ContractIsInvalid(String body) throws Exception {
        // when, then
        send(body, 400);
        assertThat(jobs.count()).isZero();
    }

    @Test
    @DisplayName("일부 알림이 잘렸거나 조직이 다르면 재설정이 필요함을 응답한다")
    void should_Reject_When_TruncatedOrWrongOrganization() throws Exception {
        // when, then
        send(payload("firing", "dev", "api-errors").replace("\"truncatedAlerts\":0", "\"truncatedAlerts\":1"), 400);
        send(payload("firing", "dev", "api-errors").replace("\"orgId\":1", "\"orgId\":2"), 400);
    }

    @Test
    @DisplayName("최대 크기를 넘는 요청을 거절한다")
    void should_Reject_When_BodyIsTooLarge() throws Exception {
        // when, then
        send("x".repeat(262145), 413);
    }

    private void send(String body, int statusCode) throws Exception {
        mvc.perform(post(ENDPOINT).header("Authorization", "Bearer " + Fixtures.TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(statusCode));
    }

    private String payload(String status, String environment, String profile) {
        return """
                {"orgId":1,"truncatedAlerts":0,"receiver":"chaekchaek-dev","status":"firing","version":"1",
                 "groupKey":"example","externalURL":"https://grafana.example.com",
                 "alerts":[{"status":"%s","fingerprint":"abc123","startsAt":"2026-11-17T09:58:00Z",
                   "endsAt":"0001-01-01T00:00:00Z","annotations":{"summary":"원본 설명"},
                   "labels":{"env":"%s","analysis_profile":"%s","alertname":"Backend5xx"}}]}
                """.formatted(status, environment, profile);
    }

    @TestConfiguration
    static class TimeConfiguration {
        @Bean
        @Primary
        Clock testClock() {
            return Clock.fixed(Fixtures.NOW, ZoneOffset.UTC);
        }
    }
}
