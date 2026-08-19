package com.chaekchaek.home.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.home.dto.LatestReviewListResponse;
import com.chaekchaek.home.dto.LatestReviewResponse;
import com.chaekchaek.home.dto.PopularBookListResponse;
import com.chaekchaek.home.dto.PopularBookResponse;
import com.chaekchaek.home.service.HomeService;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = HomeController.class, excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class)
@AutoConfigureRestDocs
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HomeService homeService;

    @Test
    @DisplayName("인기 책 목록을 조회하고 문서화한다")
    void should_ReturnPopularBooks_When_FindingPopularBooks() throws Exception {
        // given
        when(homeService.getPopularBooks()).thenReturn(new PopularBookListResponse(List.of(
                new PopularBookResponse(42L, "9788925568683", "마션", "https://example.com/martian.jpg",
                        List.of("앤디 위어"), 12, 30)
        )));

        // when & then
        mockMvc.perform(get("/api/v1/home/popular-books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books[0].bookId").value(42))
                .andExpect(jsonPath("$.books[0].isbn13").value("9788925568683"))
                .andExpect(jsonPath("$.books[0].reviewCount").value(12))
                .andExpect(jsonPath("$.books[0].replyCount").value(30))
                .andDo(document("home-popular-books",
                        responseFields(popularBookResponseFields()),
                        resource(ResourceSnippetParameters.builder()
                                .summary("인기 책 목록 조회")
                                .description("유효 감상과 답글 수의 합이 많은 책을 최대 10권 조회한다. 조회할 책이 없으면 빈 배열을 반환한다")
                                .tag("홈")
                                .responseFields(popularBookResponseFields())
                                .build())));
    }

    @Test
    @DisplayName("인기 책이 없으면 빈 목록을 반환한다")
    void should_ReturnEmptyPopularBooks_When_NoBookIsPopular() throws Exception {
        // given
        when(homeService.getPopularBooks()).thenReturn(new PopularBookListResponse(List.of()));

        // when & then
        mockMvc.perform(get("/api/v1/home/popular-books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books").isEmpty());
    }

    @Test
    @DisplayName("최신 감상 목록을 조회하고 문서화한다")
    void should_ReturnLatestReviews_When_FindingLatestReviews() throws Exception {
        // given
        when(homeService.getLatestReviews()).thenReturn(new LatestReviewListResponse(List.of(
                new LatestReviewResponse("도시는 기억으로 만들어진다는 문장에서 오래 멈췄다.",
                        java.time.Instant.parse("2026-08-18T14:00:00Z"), 12L,
                        42L, "9788936433598", "보이지 않는 도시", "https://example.com/invisible-cities.jpg")
        )));

        // when & then
        mockMvc.perform(get("/api/v1/home/latest-reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviews[0].content").value("도시는 기억으로 만들어진다는 문장에서 오래 멈췄다."))
                .andExpect(jsonPath("$.reviews[0].createdAt").value("2026-08-18T14:00:00Z"))
                .andExpect(jsonPath("$.reviews[0].replyCount").value(12))
                .andExpect(jsonPath("$.reviews[0].bookId").value(42))
                .andExpect(jsonPath("$.reviews[0].isbn13").value("9788936433598"))
                .andDo(document("home-latest-reviews",
                        responseFields(latestReviewResponseFields()),
                        resource(ResourceSnippetParameters.builder()
                                .summary("최신 감상 목록 조회")
                                .description("삭제되지 않은 최신 감상을 최대 10개 조회한다. 조회할 감상이 없으면 빈 배열을 반환한다")
                                .tag("홈")
                                .responseFields(latestReviewResponseFields())
                                .build())));
    }

    @Test
    @DisplayName("최신 감상이 없으면 빈 목록을 반환한다")
    void should_ReturnEmptyLatestReviews_When_NoReviewExists() throws Exception {
        // given
        when(homeService.getLatestReviews()).thenReturn(new LatestReviewListResponse(List.of()));

        // when & then
        mockMvc.perform(get("/api/v1/home/latest-reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviews").isEmpty());
    }

    private static FieldDescriptor[] popularBookResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("books").type(JsonFieldType.ARRAY).description("인기 책 목록"),
                fieldWithPath("books[].bookId").type(JsonFieldType.NUMBER).description("도서 ID"),
                fieldWithPath("books[].isbn13").type(JsonFieldType.STRING).description("ISBN-13"),
                fieldWithPath("books[].title").type(JsonFieldType.STRING).description("도서 제목"),
                fieldWithPath("books[].coverImageUrl").type(JsonFieldType.STRING).description("표지 이미지 URL"),
                fieldWithPath("books[].authors").type(JsonFieldType.ARRAY).description("저자 목록")
                        .attributes(key("itemsType").value(JsonFieldType.STRING)),
                fieldWithPath("books[].reviewCount").type(JsonFieldType.NUMBER).description("삭제되지 않은 감상 수"),
                fieldWithPath("books[].replyCount").type(JsonFieldType.NUMBER).description("삭제되지 않은 답글 수")
        };
    }

    private static FieldDescriptor[] latestReviewResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("reviews").type(JsonFieldType.ARRAY).description("최신 감상 목록"),
                fieldWithPath("reviews[].content").type(JsonFieldType.STRING).description("감상 내용"),
                fieldWithPath("reviews[].createdAt").type(JsonFieldType.STRING).description("감상 작성 시각(UTC)"),
                fieldWithPath("reviews[].replyCount").type(JsonFieldType.NUMBER).description("삭제되지 않은 답글 수"),
                fieldWithPath("reviews[].bookId").type(JsonFieldType.NUMBER).description("도서 ID"),
                fieldWithPath("reviews[].isbn13").type(JsonFieldType.STRING).description("ISBN-13"),
                fieldWithPath("reviews[].bookTitle").type(JsonFieldType.STRING).description("도서 제목"),
                fieldWithPath("reviews[].bookCoverImageUrl").type(JsonFieldType.STRING).description("도서 표지 이미지 URL")
        };
    }
}
