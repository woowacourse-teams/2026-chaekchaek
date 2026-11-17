package com.chaekchaek.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaekchaek.incident.alert.domain.IncidentReference;
import com.chaekchaek.incident.alert.repository.IncidentRepository;
import com.chaekchaek.incident.analysis.service.ModelBudgetService;
import com.chaekchaek.incident.question.dto.PreparedReply;
import com.chaekchaek.incident.question.service.QuestionService;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

class RestartIntegrationTest {
    @TempDir
    Path directory;

    @Test
    @DisplayName("이전 자동 작업은 장애 정보만 이관하고 재시작해도 실행하지 않는다")
    void should_MigrateIncidentOnly_When_LegacyDatabaseIsRestarted() {
        // given
        var reference = new IncidentReference(
                "dev",
                "legacy",
                Fixtures.NOW
        );
        var timestamp = OffsetDateTime.ofInstant(Fixtures.NOW, ZoneOffset.UTC);
        try (var context = start()) {
            context.getBean(JdbcTemplate.class).update(
                    """
                    INSERT INTO incident_job
                    (id, environment, alert_name, profile, starts_at, received_at, deadline_at,
                     next_attempt_at, resolved, work_state, attempts, delivery_attempts)
                    VALUES (?, 'dev', 'Backend5xx', 'api-errors', ?, ?, ?, ?, true, 'PENDING', 0, 0)
                    """,
                    reference.key(1),
                    timestamp,
                    timestamp,
                    timestamp,
                    timestamp
            );
            assertThat(context.getBean(ModelBudgetService.class).reserve()).isTrue();
            var questions = context.getBean(QuestionService.class);
            questions.accept(Fixtures.request("EvRESTART"));
            questions.claimNext().orElseThrow();
            questions.prepare("EvRESTART", PreparedReply.notice("저장된 답변"));
        }
        // when
        try (var context = start()) {
            var incidents = context.getBean(IncidentRepository.class);
            // then
            assertThat(incidents.findById(reference.key(1))).get().satisfies(incident -> {
                assertThat(incident.isResolved()).isTrue();
                assertThat(incident.getStartsAt()).isEqualTo(Fixtures.NOW);
            });
            assertThat(context.getBean(ModelBudgetService.class).reserve()).isFalse();
            var questions = context.getBean(QuestionService.class);
            var recovered = questions.claimNext().orElseThrow();
            assertThat(recovered.resultJson()).contains("저장된 답변");
            assertThat(recovered.attempts()).isEqualTo(1);
            questions.delivered(recovered.id());
            assertThat(context.getBean(JdbcTemplate.class).queryForObject(
                    "SELECT work_state FROM incident_job WHERE id = ?",
                    String.class,
                    reference.key(1)
            )).isEqualTo("PENDING");
        }
        try (var context = start()) {
            assertThat(context.getBean(IncidentRepository.class).count()).isEqualTo(1);
        }
    }

    private ConfigurableApplicationContext start() {
        return new SpringApplicationBuilder(IncidentAnalyzerApplication.class, WebhookIntegrationTest.TimeConfiguration.class)
                .web(WebApplicationType.NONE)
                .run(
                        "--spring.datasource.url=jdbc:h2:file:" + directory.resolve("incidents") + ";DB_CLOSE_ON_EXIT=FALSE",
                        "--incident.webhook-token=" + Fixtures.TOKEN,
                        "--incident.enabled-environments=dev",
                        "--incident.llm-enabled=true",
                        "--incident.llm-api-key=test-key",
                        "--incident.llm-model=test-model",
                        "--incident.daily-call-limit=1",
                        "--incident.slack.enabled=true",
                        "--incident.slack.app-token=xapp-test",
                        "--incident.slack.bot-token=xoxb-test",
                        "--incident.slack.channel-dev=CDEV",
                        "--incident.slack.alert-bot-id-dev=BALERT",
                        "--incident.worker-enabled=false",
                        "--incident.socket-enabled=false"
                );
    }
}
