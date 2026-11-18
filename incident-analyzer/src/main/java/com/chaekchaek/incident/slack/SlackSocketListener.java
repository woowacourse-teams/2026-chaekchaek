package com.chaekchaek.incident.slack;

import com.chaekchaek.incident.slack.dto.SlackIdentity;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class SlackSocketListener implements WebSocket.Listener {
    private static final int MAX_MESSAGE_LENGTH = 262144;
    private final SlackMentionHandler handler;
    private final SlackIdentity identity;
    private final JsonMapper mapper;
    private final Consumer<WebSocket> opened;
    private final Consumer<WebSocket> closed;
    private final Runnable received;
    private final StringBuilder fragments = new StringBuilder();

    public SlackSocketListener(
            SlackMentionHandler handler,
            SlackIdentity identity,
            JsonMapper mapper,
            Consumer<WebSocket> opened,
            Consumer<WebSocket> closed,
            Runnable received
    ) {
        this.handler = handler;
        this.identity = identity;
        this.mapper = mapper;
        this.opened = opened;
        this.closed = closed;
        this.received = received;
    }

    @Override
    public void onOpen(WebSocket socket) {
        opened.accept(socket);
        socket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
        received.run();
        if (fragments.length() + data.length() > MAX_MESSAGE_LENGTH) {
            disconnect(socket);
            return null;
        }
        fragments.append(data);
        if (!last) {
            socket.request(1);
            return null;
        }
        String message = fragments.toString();
        fragments.setLength(0);
        try {
            return handle(socket, mapper.readTree(message));
        } catch (RuntimeException exception) {
            disconnect(socket);
            return null;
        }
    }

    private CompletionStage<?> handle(WebSocket socket, JsonNode envelope) {
        if ("disconnect".equals(envelope.path("type").asText())) {
            disconnect(socket);
            return null;
        }
        String id = envelope.path("envelope_id").asText("");
        if ("events_api".equals(envelope.path("type").asText()) && !id.isBlank()) {
            try {
                handler.accept(envelope.path("payload"), identity);
            } catch (IllegalArgumentException exception) {
                // 형식이 잘못된 이벤트는 저장하지 않고 확인 응답으로 재전송을 끝낸다.
            }
        }
        socket.request(1);
        if (!id.isBlank()) {
            return socket.sendText(mapper.writeValueAsString(Map.of("envelope_id", id)), true);
        }
        return null;
    }

    @Override
    public CompletionStage<?> onPing(WebSocket socket, ByteBuffer message) {
        received.run();
        socket.request(1);
        return socket.sendPong(message);
    }

    @Override
    public CompletionStage<?> onPong(WebSocket socket, ByteBuffer message) {
        received.run();
        socket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket socket, int statusCode, String reason) {
        closed.accept(socket);
        return null;
    }

    @Override
    public void onError(WebSocket socket, Throwable error) {
        disconnect(socket);
    }

    private void disconnect(WebSocket socket) {
        fragments.setLength(0);
        socket.abort();
        closed.accept(socket);
    }
}
