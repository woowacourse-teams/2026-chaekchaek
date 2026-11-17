package com.chaekchaek.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.chaekchaek.incident.question.dto.QuestionRequest;
import com.chaekchaek.incident.question.service.QuestionService;
import com.chaekchaek.incident.slack.SlackMentionHandler;
import com.chaekchaek.incident.slack.dto.SlackIdentity;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class SlackMentionHandlerTest {
    private final JsonMapper mapper = JsonMapper.builder().build();
    @Mock
    private QuestionService questions;

    @Test
    @DisplayName("사람이 원본 스레드에서 멘션하면 비밀값을 지우고 질문을 저장한다")
    void should_AcceptSanitizedQuestion_When_HumanMentionsInThread() {
        // given
        var payload = payload("TTEAM", "CDEV", "UHUMAN", "1794910000.000001", "");
        // when
        handler().accept(payload, identity());
        // then
        var request = ArgumentCaptor.forClass(QuestionRequest.class);
        verify(questions).accept(request.capture());
        assertThat(request.getValue().question()).isEqualTo("이 오류 [비밀값 제거] [웹훅 주소 제거]");
        assertThat(request.getValue().environment()).isEqualTo("dev");
    }

    @ParameterizedTest
    @CsvSource(value = {
            "TOTHER,CDEV,UHUMAN,1794910000.000001,none",
            "TTEAM,COTHER,UHUMAN,1794910000.000001,none",
            "TTEAM,CDEV,UBOT,1794910000.000001,none",
            "TTEAM,CDEV,UHUMAN,1794910001.000001,none",
            "TTEAM,CDEV,UHUMAN,empty,none",
            "TTEAM,CDEV,UHUMAN,1794910000.000001,BOTHER"
    })
    @DisplayName("다른 채널이나 워크스페이스와 봇 메시지와 스레드 밖 멘션을 무시한다")
    void should_Ignore_When_MentionIsOutsideAllowedThread(
            String team,
            String channel,
            String user,
            String thread,
            String bot
    ) {
        // given
        var payload = payload(team, channel, user, thread.replace("empty", ""), bot.replace("none", ""));
        // when
        handler().accept(payload, identity());
        // then
        verifyNoInteractions(questions);
    }

    private SlackMentionHandler handler() {
        return new SlackMentionHandler(Fixtures.slackProperties("https://slack.com/api/"), questions);
    }

    private SlackIdentity identity() {
        return new SlackIdentity(
                "TTEAM",
                "UBOT",
                "BBOT"
        );
    }

    private JsonNode payload(
            String team,
            String channel,
            String user,
            String thread,
            String bot
    ) {
        var event = mapper.createObjectNode()
                .put("type", "app_mention")
                .put("channel", channel)
                .put("user", user)
                .put("ts", "1794910001.000001")
                .put("thread_ts", thread)
                .put("text", "<@UBOT> 이 오류 sk-private-secret https://hooks.slack.com/services/private");
        if (!bot.isBlank()) {
            event.put("bot_id", bot);
        }
        return mapper.valueToTree(Map.of(
                "type", "event_callback",
                "team_id", team,
                "event_id", "EvQUESTION",
                "event", event
        ));
    }
}
