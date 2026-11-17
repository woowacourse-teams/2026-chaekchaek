package com.chaekchaek.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaekchaek.incident.common.JsonHttpClient;
import com.chaekchaek.incident.common.RemoteCallException;
import com.chaekchaek.incident.question.dto.PreparedReply;
import com.chaekchaek.incident.slack.client.SlackApiClient;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class SlackApiClientTest {
    private final JsonMapper mapper = JsonMapper.builder().build();
    private MockWebServer server;
    private SlackApiClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        client = new SlackApiClient(Fixtures.slackProperties(server.url("/").toString()), new JsonHttpClient(mapper));
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    @DisplayName("봇 토큰으로 원본 메시지 하나를 조회한다")
    void should_ReadExactParent_When_ResolvingThread() throws Exception {
        // given
        server.enqueue(new MockResponse().setBody("{\"ok\":true,\"messages\":[{\"ts\":\"1794910000.000001\"}]}"));
        // when
        var parent = client.parentMessage(Fixtures.task(1, null));
        // then
        var request = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer xoxb-test");
        assertThat(request.getRequestUrl().queryParameter("latest")).isEqualTo("1794910000.000001");
        assertThat(request.getRequestUrl().queryParameter("limit")).isEqualTo("1");
        assertThat(request.getRequestUrl().queryParameter("inclusive")).isEqualTo("true");
        assertThat(parent.path("ts").asText()).isEqualTo("1794910000.000001");
    }

    @Test
    @DisplayName("답변은 요청한 스레드로만 보내고 채널에 다시 게시하지 않는다")
    void should_ReplyInThread_When_PostingAnswer() throws Exception {
        // given
        server.enqueue(new MockResponse().setBody("{\"ok\":true,\"channel\":\"CDEV\"}"));
        // when
        client.reply(Fixtures.task(1, null), PreparedReply.notice("자료 확인이 필요합니다"));
        // then
        var request = server.takeRequest(1, TimeUnit.SECONDS);
        var body = mapper.readTree(request.getBody().readUtf8());
        assertThat(request.getPath()).isEqualTo("/chat.postMessage");
        assertThat(body.path("channel").asText()).isEqualTo("CDEV");
        assertThat(body.path("thread_ts").asText()).isEqualTo("1794910000.000001");
        assertThat(body.path("reply_broadcast").asBoolean()).isFalse();
        assertThat(body.path("mrkdwn").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("HTTP 성공이어도 Slack 오류이면 발송 실패로 처리한다")
    void should_Fail_When_SlackReturnsLogicalError() {
        // given
        server.enqueue(new MockResponse().setBody("{\"ok\":false,\"error\":\"not_in_channel\",\"detail\":\"secret\"}"));
        // when, then
        assertThatThrownBy(() -> client.reply(Fixtures.task(1, null), PreparedReply.notice("답변")))
                .isInstanceOf(RemoteCallException.class)
                .hasMessage("SLACK_not_in_channel");
    }

    @Test
    @DisplayName("앱 토큰으로 Slack의 새 연결 주소를 요청한다")
    void should_UseAppToken_When_OpeningSocket() throws Exception {
        // given
        server.enqueue(new MockResponse().setBody("{\"ok\":true,\"url\":\"wss://wss-primary.slack.com/link?ticket=test\"}"));
        // when
        var url = client.connectionUrl();
        // then
        var request = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer xapp-test");
        assertThat(request.getPath()).isEqualTo("/apps.connections.open");
        assertThat(url.getScheme()).isEqualTo("wss");
    }
}
