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
                new PopularBookResponse(42L, "마션", "https://example.com/martian.jpg", List.of("앤디 위어"), 12, 30)
        )));

        // when & then
        mockMvc.perform(get("/api/v1/home/popular-books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books[0].bookId").value(42))
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

    private static FieldDescriptor[] popularBookResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("books").type(JsonFieldType.ARRAY).description("인기 책 목록"),
                fieldWithPath("books[].bookId").type(JsonFieldType.NUMBER).description("도서 ID"),
                fieldWithPath("books[].title").type(JsonFieldType.STRING).description("도서 제목"),
                fieldWithPath("books[].coverImageUrl").type(JsonFieldType.STRING).description("표지 이미지 URL"),
                fieldWithPath("books[].authors").type(JsonFieldType.ARRAY).description("저자 목록")
                        .attributes(key("itemsType").value(JsonFieldType.STRING)),
                fieldWithPath("books[].reviewCount").type(JsonFieldType.NUMBER).description("삭제되지 않은 감상 수"),
                fieldWithPath("books[].replyCount").type(JsonFieldType.NUMBER).description("삭제되지 않은 답글 수")
        };
    }
}
