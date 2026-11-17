package com.chaekchaek.incident;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.chaekchaek.incident.slack.SlackMentionHandler;
import com.chaekchaek.incident.slack.SlackSocketListener;
import com.chaekchaek.incident.slack.dto.SlackIdentity;
import java.net.http.WebSocket;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import tools.jackson.databind.json.JsonMapper;

class SlackSocketListenerTest {
    private final SlackMentionHandler handler = mock(SlackMentionHandler.class);
    private final WebSocket socket = mock(WebSocket.class);
    private final Consumer<WebSocket> closed = mock(Consumer.class);
    private SlackSocketListener listener;

    @BeforeEach
    void setUp() {
        listener = new SlackSocketListener(
                handler,
                new SlackIdentity(
                        "TTEAM",
                        "UBOT",
                        "BBOT"
                ),
                JsonMapper.builder().build(),
                active -> {},
                closed,
                () -> {}
        );
    }

    @Test
    @DisplayName("분할된 이벤트를 저장한 뒤에만 전송 봉투를 확인 응답한다")
    void should_AcknowledgeAfterPersistence_When_FragmentedEventArrives() {
        // given
        listener.onText(socket, "{\"type\":\"events_api\",", false);
        verifyNoInteractions(handler);
        // when
        listener.onText(socket, "\"envelope_id\":\"envelope-1\",\"payload\":{}}", true);
        // then
        var order = inOrder(handler, socket);
        order.verify(handler).accept(any(), any());
        order.verify(socket).sendText("{\"envelope_id\":\"envelope-1\"}", true);
    }

    @Test
    @DisplayName("저장에 실패하면 확인 응답 없이 연결을 끊어 Slack 재전송을 받는다")
    void should_NotAcknowledge_When_DatabaseFails() {
        // given
        doThrow(new DataAccessResourceFailureException("database unavailable")).when(handler).accept(any(), any());
        // when
        listener.onText(socket, "{\"type\":\"events_api\",\"envelope_id\":\"e1\",\"payload\":{}}", true);
        // then
        verify(socket, never()).sendText(any(), anyBoolean());
        verify(socket).abort();
        verify(closed).accept(socket);
    }

    @Test
    @DisplayName("재접속 안내가 오면 현재 연결을 닫는다")
    void should_ReleaseConnection_When_SlackRequestsRefresh() {
        // given, when
        listener.onText(socket, "{\"type\":\"disconnect\",\"reason\":\"refresh_requested\"}", true);
        // then
        verify(socket).abort();
        verify(closed).accept(socket);
        verifyNoInteractions(handler);
    }

    @Test
    @DisplayName("입력 크기를 넘는 이벤트는 처리하지 않는다")
    void should_Disconnect_When_EnvelopeIsTooLarge() {
        // given, when
        listener.onText(socket, "x".repeat(262145), true);
        // then
        verify(socket).abort();
        verifyNoInteractions(handler);
    }
}
