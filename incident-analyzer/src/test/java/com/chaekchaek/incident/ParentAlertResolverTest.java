package com.chaekchaek.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.chaekchaek.incident.question.domain.QuestionRejectedException;
import com.chaekchaek.incident.slack.ParentAlertResolver;
import com.chaekchaek.incident.slack.client.SlackApiClient;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class ParentAlertResolverTest {
    private final JsonMapper mapper = JsonMapper.builder().build();
    @Mock
    private SlackApiClient slack;

    @Test
    @DisplayName("등록된 원본 발신자의 장애 참조를 읽는다")
    void should_Resolve_When_ParentIsOriginalAlert() {
        // given
        parent("BALERT", "1794910000.000001", "CC-INCIDENT|dev|fingerprint|2026-11-17T10:00:00Z");
        // when
        var reference = resolver().resolve(Fixtures.task(1, null));
        // then
        assertThat(reference.fingerprint()).isEqualTo("fingerprint");
        assertThat(reference.startsAt()).isEqualTo(Fixtures.NOW);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "BOTHER"})
    @DisplayName("사람이 복사하거나 다른 봇이 보낸 알림은 거부한다")
    void should_Reject_When_ParentAuthorIsUntrusted(String author) {
        // given
        parent(author, "1794910000.000001", "CC-INCIDENT|dev|fingerprint|2026-11-17T10:00:00Z");
        // when, then
        assertThatThrownBy(() -> resolver().resolve(Fixtures.task(1, null)))
                .isInstanceOf(QuestionRejectedException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "일반 알림",
            "CC-INCIDENT|prod|fingerprint|2026-11-17T10:00:00Z",
            "CC-INCIDENT|dev|fingerprint|2026-99-17T10:00:00Z",
            "CC-INCIDENT|dev|one|2026-11-17T10:00:00Z CC-INCIDENT|dev|two|2026-11-17T10:00:00Z"
    })
    @DisplayName("환경과 장애를 하나로 식별할 수 없으면 분석하지 않는다")
    void should_Reject_When_ReferenceIsAmbiguousOrInvalid(String text) {
        // given
        parent("BALERT", "1794910000.000001", text);
        // when, then
        assertThatThrownBy(() -> resolver().resolve(Fixtures.task(1, null)))
                .isInstanceOf(QuestionRejectedException.class);
    }

    @Test
    @DisplayName("조회 결과가 요청한 원본 시각과 다르면 거부한다")
    void should_Reject_When_ParentWasDeleted() {
        // given
        parent("BALERT", "1794910000.000000", "CC-INCIDENT|dev|fingerprint|2026-11-17T10:00:00Z");
        // when, then
        assertThatThrownBy(() -> resolver().resolve(Fixtures.task(1, null)))
                .isInstanceOf(QuestionRejectedException.class);
    }

    private ParentAlertResolver resolver() {
        return new ParentAlertResolver(slack, Fixtures.slackProperties("https://slack.com/api/"));
    }

    private void parent(String botId, String timestamp, String text) {
        given(slack.parentMessage(any())).willReturn(mapper.valueToTree(Map.of(
                "bot_id", botId,
                "ts", timestamp,
                "text", text
        )));
    }
}
