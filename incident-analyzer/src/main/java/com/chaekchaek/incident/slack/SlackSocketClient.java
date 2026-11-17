package com.chaekchaek.incident.slack;

import com.chaekchaek.incident.slack.client.SlackApiClient;
import com.chaekchaek.incident.slack.dto.SlackIdentity;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@ConditionalOnProperty(name = {"incident.slack.enabled", "incident.socket-enabled"}, havingValue = "true", matchIfMissing = true)
public class SlackSocketClient {
    private final SlackApiClient api;
    private final SlackMentionHandler handler;
    private final JsonMapper mapper;
    private final Clock clock;
    private final MeterRegistry meters;
    private final AtomicReference<WebSocket> socket = new AtomicReference<>();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().name("slack-socket").daemon().factory()
    );
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private volatile Instant lastReceived = Instant.EPOCH;
    private Instant lastPing = Instant.EPOCH;
    private Instant nextConnect = Instant.EPOCH;
    private SlackIdentity identity;
    private int failures;
    private volatile boolean stopped;

    public SlackSocketClient(
            SlackApiClient api,
            SlackMentionHandler handler,
            JsonMapper mapper,
            Clock clock,
            MeterRegistry meters
    ) {
        this.api = api;
        this.handler = handler;
        this.mapper = mapper;
        this.clock = clock;
        this.meters = meters;
        meters.gauge("incident_slack_connected", socket, value -> value.get() == null ? 0 : 1);
    }

    @PostConstruct
    public void start() {
        executor.scheduleWithFixedDelay(this::tick, 0, 1, TimeUnit.SECONDS);
    }

    private void tick() {
        if (stopped) {
            return;
        }
        try {
            WebSocket active = socket.get();
            if (active == null && !clock.instant().isBefore(nextConnect)) {
                connect();
            } else if (active != null) {
                checkConnection(active);
            }
        } catch (RuntimeException exception) {
            failures = Math.min(failures + 1, 5);
            nextConnect = clock.instant().plusSeconds(1L << failures);
            meters.counter("incident_slack_connection_failures").increment();
        }
    }

    private void connect() {
        if (identity == null) {
            identity = api.identity();
        }
        var listener = new SlackSocketListener(
                handler,
                identity,
                mapper,
                this::connected,
                this::disconnected,
                () -> lastReceived = clock.instant()
        );
        http.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .buildAsync(api.connectionUrl(), listener)
                .orTimeout(10, TimeUnit.SECONDS)
                .join();
    }

    private void connected(WebSocket active) {
        if (stopped || !socket.compareAndSet(null, active)) {
            active.abort();
            return;
        }
        lastReceived = clock.instant();
        lastPing = clock.instant();
        failures = 0;
    }

    private void checkConnection(WebSocket active) {
        if (lastReceived.plusSeconds(90).isBefore(clock.instant())) {
            active.abort();
            disconnected(active);
            return;
        }
        if (lastPing.plusSeconds(30).isBefore(clock.instant())) {
            lastPing = clock.instant();
            active.sendPing(ByteBuffer.wrap(new byte[] {1}))
                    .orTimeout(5, TimeUnit.SECONDS)
                    .exceptionally(error -> {
                        active.abort();
                        disconnected(active);
                        return null;
                    });
        }
    }

    private void disconnected(WebSocket active) {
        socket.compareAndSet(active, null);
    }

    @PreDestroy
    public void stop() {
        stopped = true;
        executor.shutdownNow();
        WebSocket active = socket.getAndSet(null);
        if (active != null) {
            active.abort();
        }
        http.close();
    }
}
