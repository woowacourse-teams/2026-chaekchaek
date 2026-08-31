package com.chaekchaek.book.client.fixture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.assertj.core.api.SoftAssertions;

public final class Yes24MockServer implements AutoCloseable {

    private static final String TEST_API_KEY = "test-yes24-api-key";

    private final MockWebServer server = new MockWebServer();

    public Yes24MockServer() throws IOException {
        server.start();
    }

    public String baseUrl() {
        return server.url("/").toString();
    }

    public String apiKey() {
        return TEST_API_KEY;
    }

    public void 응답한다(int status, String body) {
        server.enqueue(new MockResponse()
                .setResponseCode(status)
                .setHeader("Content-Type", "application/json")
                .setBody(body));
    }

    public void 연결을_즉시_종료한다() {
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return disconnectAtStartResponse();
            }

            @Override
            public MockResponse peek() {
                return disconnectAtStartResponse();
            }
        });
    }

    private MockResponse disconnectAtStartResponse() {
        return new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START);
    }

    public void 검색_요청을_검증한다(String query, int page) throws InterruptedException {
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getHeader("X-Api-Key")).isEqualTo(TEST_API_KEY);

        HttpUrl url = request.getRequestUrl();
        assertThat(url).isNotNull();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(url.encodedPath()).isEqualTo("/v1/goods/itemList");
            softly.assertThat(url.queryParameterNames())
                    .containsExactlyInAnyOrder("query", "category", "page", "pageSize", "detail");
            softly.assertThat(url.queryParameter("query")).isEqualTo(query);
            softly.assertThat(url.queryParameter("category")).isEqualTo("BOOK");
            softly.assertThat(url.queryParameter("page")).isEqualTo(String.valueOf(page));
            softly.assertThat(url.queryParameter("pageSize")).isEqualTo("10");
            softly.assertThat(url.queryParameter("detail")).isEqualTo("N");
        });
    }

    @Override
    public void close() throws IOException {
        server.shutdown();
    }
}
