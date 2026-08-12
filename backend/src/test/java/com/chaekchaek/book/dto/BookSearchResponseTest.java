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
    @DisplayName("JSON으로 직렬화하면 기존 속성명을 유지한다")
    void should_PreservePropertyNames_When_SerializingToJson() throws JacksonException {
        // given
        BookItem book = new BookItem(
                "마션",
                "https://image.aladin.co.kr/martian.jpg",
                "앤디 위어",
                "2026-07-01",
                "9788925568683",
                "국내도서>소설>과학소설",
                "알에이치코리아(RHK)"
        );
        BookSearchResponse response = new BookSearchResponse(
                6,
                1,
                10,
                true,
                List.of(book)
        );

        // when
        JsonNode json = objectMapper.valueToTree(response);

        // then
        assertThat(json.propertyNames()).containsExactlyInAnyOrder(
                "totalResults", "startIndex", "itemsPerPage", "hasNext", "items"
        );
        assertThat(json.at("/items/0").propertyNames()).containsExactlyInAnyOrder(
                "title", "coverImageUrl", "author", "publishedDate",
                "isbn13", "category", "publisher"
        );
    }
}
