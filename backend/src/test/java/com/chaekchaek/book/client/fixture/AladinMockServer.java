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

public final class AladinMockServer implements AutoCloseable {

    private static final String TEST_TTB_KEY = "test-ttb-key";

    private final MockWebServer server = new MockWebServer();

    public void start() throws IOException {
        server.start();
    }

    public String baseUrl() {
        return server.url("/").toString();
    }

    public String ttbKey() {
        return TEST_TTB_KEY;
    }

    public void 검색_응답한다(String responseBody) {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseBody));
    }

    public void 본문_없는_200_응답한다() {
        server.enqueue(new MockResponse().setResponseCode(200));
    }

    public void 콘텐츠_없는_204_응답한다() {
        server.enqueue(new MockResponse().setResponseCode(204));
    }

    public void 서버_오류_500_응답한다() {
        server.enqueue(new MockResponse().setResponseCode(500));
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

        HttpUrl url = request.getRequestUrl();
        assertThat(url).isNotNull();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(url.encodedPath())
                    .isEqualTo("/ttb/api/ItemSearch.aspx");
            softly.assertThat(url.queryParameter("ttbkey"))
                    .isEqualTo(TEST_TTB_KEY);
            softly.assertThat(url.queryParameter("Query"))
                    .isEqualTo(query);
            softly.assertThat(url.queryParameter("Start"))
                    .isEqualTo(String.valueOf(page));
            softly.assertThat(url.queryParameter("MaxResults"))
                    .isEqualTo("10");
            softly.assertThat(url.queryParameter("Cover"))
                    .isEqualTo("Big");
            softly.assertThat(url.queryParameter("Output"))
                    .isEqualTo("JS");
            softly.assertThat(url.queryParameter("InputEncoding"))
                    .isEqualTo("utf-8");
            softly.assertThat(url.queryParameter("Version"))
                    .isEqualTo("20131101");
        });
    }

    public void 상세_요청을_검증한다(String isbn13) throws InterruptedException {
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("GET");

        HttpUrl url = request.getRequestUrl();
        assertThat(url).isNotNull();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(url.encodedPath()).isEqualTo("/ttb/api/ItemLookUp.aspx");
            softly.assertThat(url.queryParameter("ttbkey")).isEqualTo(TEST_TTB_KEY);
            softly.assertThat(url.queryParameter("ItemIdType")).isEqualTo("ISBN13");
            softly.assertThat(url.queryParameter("ItemId")).isEqualTo(isbn13);
            softly.assertThat(url.queryParameter("Cover")).isEqualTo("Big");
            softly.assertThat(url.queryParameter("Output")).isEqualTo("JS");
            softly.assertThat(url.queryParameter("Version")).isEqualTo("20131101");
        });
    }

    @Override
    public void close() throws IOException {
        server.shutdown();
    }
}
