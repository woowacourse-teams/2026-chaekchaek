package com.chaekchaek.feed.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.common.auth.ActorType;
import com.chaekchaek.feed.dto.FeedReviewListResponse;
import com.chaekchaek.feed.dto.FeedReviewResponse;
import com.chaekchaek.feed.service.FeedService;
import com.chaekchaek.review.dto.AuthorProfileStatus;
import com.chaekchaek.review.dto.AuthorResponse;
import com.epages.restdocs.apispec.ResourceDocumentation;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.SimpleType;
import java.time.Instant;
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

@WebMvcTest(value = FeedController.class, excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class)
@AutoConfigureRestDocs
class FeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedService feedService;

    @Test
    @DisplayName("전체 감상 피드의 페이지 요청과 응답을 문서화한다")
    void should_ReturnAndDocumentReviewPage_When_PageIsValid() throws Exception {
        // given
        FeedReviewResponse review = new FeedReviewResponse(123L, "감상 내용", true,
                Instant.parse("2026-09-28T10:00:00Z"),
                new AuthorResponse(1L, "독자", null, false, false, ActorType.MEMBER,
                        AuthorProfileStatus.AVAILABLE),
                3L, 42L, "9788936433598", "도서 제목", "https://example.com/cover.jpg");
        when(feedService.getReviews(1)).thenReturn(new FeedReviewListResponse(41, 2, List.of(review)));

        // when & then
        mockMvc.perform(get("/api/v1/feed/reviews").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(41))
                .andExpect(jsonPath("$.nextPage").value(2))
                .andExpect(jsonPath("$.reviews[0].reviewId").value(123))
                .andExpect(jsonPath("$.reviews[0].content").value("감상 내용"))
                .andExpect(jsonPath("$.reviews[0].isSpoiler").value(true))
                .andExpect(jsonPath("$.reviews[0].author.displayName").value("독자"))
                .andExpect(jsonPath("$.reviews[0].replyCount").value(3))
                .andExpect(jsonPath("$.reviews[0].bookId").value(42))
                .andDo(document("feed-reviews",
                        queryParameters(parameterWithName("page").description("1부터 시작하는 필수 페이지 번호")),
                        responseFields(feedResponseFields()),
                        resource(ResourceSnippetParameters.builder()
                                .summary("전체 감상 피드 조회")
                                .description("삭제되지 않은 감상을 스포일러 포함, 작성 시각과 감상 ID 내림차순으로 20개씩 조회한다")
                                .tag("피드")
                                .queryParameters(ResourceDocumentation.parameterWithName("page")
                                        .type(SimpleType.INTEGER).description("1부터 시작하는 필수 페이지 번호"))
                                .responseFields(feedResponseFields())
                                .build())));
        verify(feedService).getReviews(1);
    }

    @Test
    @DisplayName("조회 대상이 없으면 빈 목록과 다음 페이지 없음으로 응답한다")
    void should_ReturnEmptyPage_When_NoReviewsExist() throws Exception {
        // given
        when(feedService.getReviews(1)).thenReturn(new FeedReviewListResponse(0, null, List.of()));

        // when & then
        mockMvc.perform(get("/api/v1/feed/reviews").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.nextPage").value((Object) null))
                .andExpect(jsonPath("$.reviews").isEmpty());
    }

    @Test
    @DisplayName("페이지 번호가 없거나 양수가 아니면 잘못된 요청으로 응답한다")
    void should_RejectInvalidPage_When_PageIsMissingOrNotPositive() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/feed/reviews")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/feed/reviews").param("page", "0")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/feed/reviews").param("page", "-1")).andExpect(status().isBadRequest());
    }

    private static FieldDescriptor[] feedResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("totalCount").type(JsonFieldType.NUMBER).description("삭제되지 않고 책이 있는 감상 전체 개수"),
                fieldWithPath("nextPage").type(JsonFieldType.NUMBER).description("다음 페이지 번호. 마지막 페이지면 null").optional(),
                fieldWithPath("reviews").type(JsonFieldType.ARRAY).description("감상 목록"),
                fieldWithPath("reviews[].reviewId").type(JsonFieldType.NUMBER).description("감상 ID"),
                fieldWithPath("reviews[].content").type(JsonFieldType.STRING).description("감상 내용. 스포일러도 포함"),
                fieldWithPath("reviews[].isSpoiler").type(JsonFieldType.BOOLEAN).description("스포일러 여부"),
                fieldWithPath("reviews[].createdAt").type(JsonFieldType.STRING).description("감상 작성 시각(UTC)"),
                fieldWithPath("reviews[].author").type(JsonFieldType.OBJECT).description("작성자 정보"),
                fieldWithPath("reviews[].author.memberId").type(JsonFieldType.NUMBER)
                        .description("공개 서재 조회용 회원 ID").optional(),
                fieldWithPath("reviews[].author.displayName").type(JsonFieldType.STRING).description("작성자 표시 이름"),
                fieldWithPath("reviews[].author.profileImageUrl").type(JsonFieldType.STRING)
                        .description("작성자 프로필 이미지 URL").optional(),
                fieldWithPath("reviews[].author.anonymous").type(JsonFieldType.BOOLEAN).description("익명 작성 여부"),
                fieldWithPath("reviews[].author.mine").type(JsonFieldType.BOOLEAN).description("내가 작성한 감상인지 여부"),
                fieldWithPath("reviews[].author.actorType").type(JsonFieldType.STRING)
                        .description("작성자 유형(MEMBER, GUEST)"),
                fieldWithPath("reviews[].author.profileStatus").type(JsonFieldType.STRING)
                        .description("프로필 접근 상태. 일반 회원은 AVAILABLE, 익명·비회원·탈퇴 회원은 UNAVAILABLE"),
                fieldWithPath("reviews[].replyCount").type(JsonFieldType.NUMBER).description("삭제되지 않은 답글 수"),
                fieldWithPath("reviews[].bookId").type(JsonFieldType.NUMBER).description("도서 ID"),
                fieldWithPath("reviews[].isbn13").type(JsonFieldType.STRING).description("ISBN-13"),
                fieldWithPath("reviews[].bookTitle").type(JsonFieldType.STRING).description("도서 제목"),
                fieldWithPath("reviews[].bookCoverImageUrl").type(JsonFieldType.STRING).description("도서 표지 이미지 URL")
        };
    }
}
