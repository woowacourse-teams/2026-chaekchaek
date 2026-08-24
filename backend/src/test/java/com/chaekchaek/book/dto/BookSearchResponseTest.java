package com.chaekchaek.book.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class BookSearchResponseTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    @DisplayName("도서 검색 응답을 JSON으로 직렬화하면 계약된 속성명을 사용한다")
    void should_UseContractPropertyNames_When_SerializingToJson() throws JacksonException {
        // given
        BookItem book = new BookItem(
                null,
                "마션",
                "https://image.aladin.co.kr/martian.jpg",
                List.of("앤디 위어"),
                List.of("박아람"),
                "2026-07-01",
                "9788925568683",
                "국내도서>소설>과학소설",
                "알에이치코리아(RHK)",
                3,
                5,
                null
        );
        BookSearchResponse response = new BookSearchResponse(6, 2, List.of(book));

        // when
        JsonNode json = objectMapper.valueToTree(response);

        // then
        assertThat(json.propertyNames()).containsExactlyInAnyOrder(
                "totalCount", "nextPage", "items"
        );
        assertThat(json.at("/items/0").propertyNames()).containsExactlyInAnyOrder(
                "bookId", "title", "coverImageUrl", "authors", "translators", "publishedDate",
                "isbn13", "category", "publisher", "reviewCount", "replyCount", "isRegisteredInMyLibrary"
        );
        assertThat(json.at("/items/0/isRegisteredInMyLibrary").isNull()).isTrue();
    }

    @Test
    @DisplayName("다음 페이지가 없다면 JSON에 null 값을 명시한다")
    void should_IncludeNullNextPage_When_SerializingLastPage() throws JacksonException {
        // given
        BookSearchResponse response = new BookSearchResponse(0, null, List.of());

        // when
        JsonNode json = objectMapper.valueToTree(response);

        // then
        assertThat(json.has("nextPage")).isTrue();
        assertThat(json.at("/nextPage").isNull()).isTrue();
    }
}
