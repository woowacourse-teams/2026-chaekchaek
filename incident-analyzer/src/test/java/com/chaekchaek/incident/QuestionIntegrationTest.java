package com.chaekchaek.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaekchaek.incident.question.domain.QuestionJob;
import com.chaekchaek.incident.question.domain.QuestionState;
import com.chaekchaek.incident.question.dto.PreparedReply;
import com.chaekchaek.incident.question.repository.QuestionJobRepository;
import com.chaekchaek.incident.question.service.QuestionService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:question;DB_CLOSE_DELAY=-1",
        "incident.webhook-token=" + Fixtures.TOKEN,
        "incident.enabled-environments=dev",
        "incident.slack.enabled=true",
        "incident.slack.app-token=xapp-test",
        "incident.slack.bot-token=xoxb-test",
        "incident.slack.channel-dev=CDEV",
        "incident.slack.alert-bot-id-dev=BALERT",
        "incident.worker-enabled=false",
        "incident.socket-enabled=false"
})
@Import(WebhookIntegrationTest.TimeConfiguration.class)
class QuestionIntegrationTest {
    @Autowired
    private QuestionService questions;
    @Autowired
    private QuestionJobRepository jobs;

    @BeforeEach
    void clearJobs() {
        jobs.deleteAll();
    }

    @Test
    @DisplayName("같은 Slack 이벤트는 한 번 저장하고 다른 질문은 각각 저장한다")
    void should_DeduplicateByEventId_When_SlackRedelivers() {
        // given, when
        questions.accept(Fixtures.request("EvFIRST"));
        questions.accept(Fixtures.request("EvFIRST"));
        questions.accept(Fixtures.request("EvSECOND"));
        // then
        assertThat(jobs.count()).isEqualTo(2);
        assertThat(questions.claimNext()).get().satisfies(task -> assertThat(task.attempts()).isEqualTo(1));
        assertThat(questions.claimNext()).isPresent();
        assertThat(questions.claimNext()).isEmpty();
    }

    @Test
    @DisplayName("스레드에서 직접 답한 최근 두 질문만 대화 맥락으로 사용한다")
    void should_KeepTwoOwnAnswers_When_PreparingFollowUp() {
        // given
        for (String id : List.of("EvFIRST", "EvSECOND", "EvTHIRD")) {
            questions.accept(Fixtures.request(id));
            var task = questions.claimNext().orElseThrow();
            questions.prepare(task.id(), PreparedReply.notice(id + " 답변"));
            questions.delivered(task.id());
        }
        questions.accept(Fixtures.request("EvZFOLLOWUP"));
        // when
        var conversation = questions.conversation(questions.claimNext().orElseThrow());
        // then
        assertThat(conversation).hasSize(2);
        assertThat(conversation.getFirst().answer()).isEqualTo("EvSECOND 답변");
        assertThat(conversation.getLast().answer()).isEqualTo("EvTHIRD 답변");
    }

    @Test
    @DisplayName("분석 실패 안내가 발송된 뒤에도 원인 코드를 보존한다")
    void should_KeepFailureCode_When_NoticeIsDelivered() {
        // given
        questions.accept(Fixtures.request("EvFAILURE"));
        questions.claimNext().orElseThrow();
        // when
        questions.analysisFailed("EvFAILURE", "INVALID_MODEL_TEXT");
        questions.prepare("EvFAILURE", PreparedReply.notice("분석을 완료하지 못했습니다"));
        questions.delivered("EvFAILURE");
        // then
        assertThat(jobs.findById("EvFAILURE")).get().satisfies(job -> {
            assertThat(job.getFailureCode()).isEqualTo("INVALID_MODEL_TEXT");
            assertThat(job.getWorkState()).isEqualTo(QuestionState.SENT);
        });
    }

    @Test
    @DisplayName("발송에 세 번 실패하면 모델 결과를 보존하고 재시도를 끝낸다")
    void should_StopAfterThreeDeliveries_When_SlackKeepsFailing() {
        // given
        var job = new QuestionJob(Fixtures.request("EvRETRY"), Fixtures.NOW);
        job.claim(Fixtures.NOW);
        job.prepare("saved-result");
        // when
        job.fail("SLACK_ERROR", Fixtures.NOW);
        job.claim(Fixtures.NOW.plusSeconds(15));
        job.fail("SLACK_ERROR", Fixtures.NOW.plusSeconds(15));
        job.claim(Fixtures.NOW.plusSeconds(45));
        job.fail("SLACK_ERROR", Fixtures.NOW.plusSeconds(45));
        // then
        assertThat(job.getWorkState()).isEqualTo(QuestionState.FAILED);
        assertThat(job.getResultJson()).isEqualTo("saved-result");
        assertThat(job.getAttempts()).isEqualTo(1);
        assertThat(job.getDeliveryAttempts()).isEqualTo(3);
        assertThatThrownBy(() -> job.claim(Fixtures.NOW.plusSeconds(60)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("처리 도중 재시작되면 임대 만료 후 중복 분석 여부를 구분한다")
    void should_IncrementAttempt_When_LeaseExpiresWithoutResult() {
        // given
        var job = new QuestionJob(Fixtures.request("EvRECOVER"), Fixtures.NOW);
        job.claim(Fixtures.NOW);
        // when
        job.claim(Fixtures.NOW.plusSeconds(121));
        // then
        assertThat(job.getAttempts()).isEqualTo(2);
        assertThat(job.getResultJson()).isNull();
    }
}
