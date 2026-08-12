package com.chaekchaek.book.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.book.dto.BookItem;
import com.chaekchaek.book.dto.BookSearchResponse;
import com.chaekchaek.book.service.BookSearchService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookController.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookSearchService bookSearchService;

    @Test
    @DisplayName("유효한 요청이라면 도서 검색 결과를 반환한다")
    void should_ReturnBookSearchResponse_When_RequestIsValid() throws Exception {
        // given
        BookItem item = new BookItem(
                "마션",
                "https://image.aladin.co.kr/martian.jpg",
                List.of("앤디 위어"),
                List.of("박아람"),
                "2026-07-01",
                "9788925568683",
                "국내도서>소설>과학소설",
                "알에이치코리아(RHK)"
        );
        BookSearchResponse response = new BookSearchResponse(11, 2, List.of(item));
        when(bookSearchService.search("마션", 1)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/books")
                        .param("query", "마션")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalCount").value(11))
                .andExpect(jsonPath("$.nextPage").value(2))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title").value("마션"))
                .andExpect(jsonPath("$.items[0].coverImageUrl")
                        .value("https://image.aladin.co.kr/martian.jpg"))
                .andExpect(jsonPath("$.items[0].authors.length()").value(1))
                .andExpect(jsonPath("$.items[0].authors[0]").value("앤디 위어"))
                .andExpect(jsonPath("$.items[0].translators.length()").value(1))
                .andExpect(jsonPath("$.items[0].translators[0]").value("박아람"))
                .andExpect(jsonPath("$.items[0].publishedDate").value("2026-07-01"))
                .andExpect(jsonPath("$.items[0].isbn13").value("9788925568683"))
                .andExpect(jsonPath("$.items[0].category").value("국내도서>소설>과학소설"))
                .andExpect(jsonPath("$.items[0].publisher").value("알에이치코리아(RHK)"));

        verify(bookSearchService).search("마션", 1);
    }

    @Test
    @DisplayName("검색어가 누락되었다면 400 응답을 반환한다")
    void should_ReturnBadRequest_When_QueryIsMissing() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/books")
                        .param("page", "1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bookSearchService);
    }

    @Test
    @DisplayName("페이지가 누락되었다면 400 응답을 반환한다")
    void should_ReturnBadRequest_When_PageIsMissing() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/books")
                        .param("query", "마션"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bookSearchService);
    }

    @Test
    @DisplayName("페이지가 숫자가 아니라면 400 응답을 반환한다")
    void should_ReturnBadRequest_When_PageIsNotNumeric() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/books")
                        .param("query", "마션")
                        .param("page", "first"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bookSearchService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    @DisplayName("검색어가 비어 있다면 400 응답을 반환한다")
    void should_ReturnBadRequest_When_QueryIsBlank(String query) throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/books")
                        .param("query", query)
                        .param("page", "1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bookSearchService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    @DisplayName("페이지가 양수가 아니라면 400 응답을 반환한다")
    void should_ReturnBadRequest_When_PageIsNotPositive(String page) throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/books")
                        .param("query", "마션")
                        .param("page", page))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bookSearchService);
    }
}
