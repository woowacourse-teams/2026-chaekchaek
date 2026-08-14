package com.chaekchaek.review.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.review.dto.AuthorResponse;
import com.chaekchaek.review.dto.PageResponse;
import com.chaekchaek.review.dto.ReactionResponse;
import com.chaekchaek.review.dto.ReplyCreateRequest;
import com.chaekchaek.review.dto.ReplyResponse;
import com.chaekchaek.review.dto.ReplyUpdateRequest;
import com.chaekchaek.review.dto.ReviewCreateRequest;
import com.chaekchaek.review.dto.ReviewResponse;
import com.chaekchaek.review.dto.ReviewUpdateRequest;
import com.chaekchaek.review.service.ReviewService;
import com.chaekchaek.review.service.ReviewService.Feed;
import com.chaekchaek.review.service.ReviewService.ReviewSort;
import com.epages.restdocs.apispec.ResourceDocumentation;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.epages.restdocs.apispec.SimpleType;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.headers.HeaderDescriptor;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(ReviewController.class)
@AutoConfigureRestDocs
class ReviewControllerTest {

    private static final String REVIEW_TAG = "감상";
    private static final AuthorResponse AUTHOR = new AuthorResponse("닉네임", "https://example.com/profile.jpg", false,
            true);
    private static final HeaderDescriptor LOCATION_HEADER = headerWithName("Location")
            .description("생성된 리소스의 상대 경로");

    private static final FieldDescriptor[] PROBLEM_DETAIL_FIELDS = {
            fieldWithPath("type").type(JsonFieldType.STRING).description("문제 유형 URI"),
            fieldWithPath("title").type(JsonFieldType.STRING).description("HTTP 상태 설명"),
            fieldWithPath("status").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
            fieldWithPath("detail").type(JsonFieldType.STRING).description("오류 상세 메시지"),
            fieldWithPath("instance").type(JsonFieldType.STRING).description("오류가 발생한 요청 경로"),
            fieldWithPath("code").type(JsonFieldType.STRING).description("애플리케이션 오류 코드")
    };

    private static final FieldDescriptor[] REVIEW_CREATE_REQUEST_FIELDS = {
            fieldWithPath("content").type(JsonFieldType.STRING).description("감상 내용. 공백 불가, 최대 1,000자"),
            fieldWithPath("quote").type(JsonFieldType.STRING).description("인용문. 공백 불가, 최대 500자").optional(),
            fieldWithPath("chapter").type(JsonFieldType.STRING).description("챕터. 공백 불가, 최대 255자").optional(),
            fieldWithPath("currentPage").type(JsonFieldType.NUMBER).description("감상을 남긴 현재 페이지").optional(),
            fieldWithPath("totalPages").type(JsonFieldType.NUMBER).description("페이지 검증에 사용할 전체 페이지 수").optional(),
            fieldWithPath("isSpoiler").type(JsonFieldType.BOOLEAN).description("스포일러 여부. 기본값 false").optional()
    };

    private static final FieldDescriptor[] REVIEW_UPDATE_REQUEST_FIELDS = {
            fieldWithPath("content").type(JsonFieldType.STRING).description("변경할 감상 내용").optional(),
            fieldWithPath("quote").type(JsonFieldType.STRING).description("변경할 인용문. null이면 삭제").optional(),
            fieldWithPath("chapter").type(JsonFieldType.STRING).description("변경할 챕터. null이면 삭제").optional(),
            fieldWithPath("currentPage").type(JsonFieldType.NUMBER).description("변경할 현재 페이지. null이면 삭제").optional(),
            fieldWithPath("totalPages").type(JsonFieldType.NUMBER).description("페이지 검증에 사용할 전체 페이지 수").optional(),
            fieldWithPath("isSpoiler").type(JsonFieldType.BOOLEAN).description("변경할 스포일러 여부").optional()
    };

    private static final FieldDescriptor[] REPLY_REQUEST_FIELDS = {
            fieldWithPath("content").type(JsonFieldType.STRING).description("답글 내용. 공백 불가, 최대 200자")
    };

    private static final FieldDescriptor[] REACTION_RESPONSE_FIELDS = {
            fieldWithPath("likeCount").type(JsonFieldType.NUMBER).description("좋아요 수"),
            fieldWithPath("likedByMe").type(JsonFieldType.BOOLEAN).description("내가 좋아요를 눌렀는지 여부")
    };

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    @Test
    @DisplayName("감상 목록을 조회하고 문서화한다")
    void should_ReturnReviewPage_When_FindingReviews() throws Exception {
        // given
        PageResponse<ReviewResponse> response = new PageResponse<>(1, null, List.of(reviewResponse()));
        when(reviewService.findReviews(42L, 1, Feed.ALL, ReviewSort.PAGE)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/books/{bookId}/reviews", 42L)
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].reviewId").value(101))
                .andDo(document("review-list",
                        pathParameters(parameterWithName("bookId").description("도서 ID")),
                        queryParameters(parameterWithName("page").description("1부터 시작하는 페이지 번호"),
                                parameterWithName("feed").optional().description("피드 범위: ALL 또는 MINE. MINE은 인증 필요, 기본값 ALL"),
                                parameterWithName("sort").optional().description("정렬: PAGE, LATEST, OLDEST, POPULAR. 기본값 PAGE")),
                        responseFields(pageReviewResponseFields()),
                        resource(ResourceSnippetParameters.builder()
                                .summary("감상 목록 조회")
                                .description("도서의 감상을 페이지와 피드·정렬 조건으로 조회한다")
                                .tag(REVIEW_TAG)
                                .pathParameters(pathParameter("bookId", "도서 ID"))
                                .queryParameters(queryParameter("page", SimpleType.INTEGER, "1부터 시작하는 페이지 번호"),
                                        queryParameter("feed", SimpleType.STRING,
                                                "피드 범위: ALL 또는 MINE. MINE은 인증 필요, 기본값 ALL", true),
                                        queryParameter("sort", SimpleType.STRING, "정렬 기준. 기본값 PAGE", true))
                                .responseFields(pageReviewResponseFields())
                                .build())));

        verify(reviewService).findReviews(42L, 1, Feed.ALL, ReviewSort.PAGE);
    }

    @Test
    @DisplayName("감상을 작성하고 Location을 반환한다")
    void should_CreateReviewWithLocation_When_RequestIsValid() throws Exception {
        // given
        when(reviewService.createReview(org.mockito.ArgumentMatchers.eq(42L), org.mockito.ArgumentMatchers.any(ReviewCreateRequest.class)))
                .thenReturn(reviewResponse());

        // when & then
        mockMvc.perform(post("/api/v1/books/{bookId}/reviews", 42L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"인상 깊었다.","quote":"화성에서 살아남아야 한다.","chapter":"3장",
                                "currentPage":120,"totalPages":308,"isSpoiler":false}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/reviews/101"))
                .andExpect(jsonPath("$.reviewId").value(101))
                .andDo(document("review-create",
                        pathParameters(parameterWithName("bookId").description("도서 ID")),
                        requestFields(REVIEW_CREATE_REQUEST_FIELDS),
                        responseHeaders(LOCATION_HEADER),
                        responseFields(reviewResponseFields("")),
                        resource(ResourceSnippetParameters.builder()
                                .summary("감상 작성")
                                .description("도서에 감상을 작성하고 필요한 경우 서재 진도를 갱신한다")
                                .tag(REVIEW_TAG)
                                .pathParameters(pathParameter("bookId", "도서 ID"))
                                .requestFields(REVIEW_CREATE_REQUEST_FIELDS)
                                .responseHeaders(LOCATION_HEADER)
                                .responseFields(reviewResponseFields(""))
                                .build())));

        verify(reviewService).createReview(org.mockito.ArgumentMatchers.eq(42L),
                org.mockito.ArgumentMatchers.any(ReviewCreateRequest.class));
    }

    @Test
    @DisplayName("감상을 수정한다")
    void should_ReturnUpdatedReview_When_UpdatingReview() throws Exception {
        // given
        when(reviewService.updateReview(org.mockito.ArgumentMatchers.eq(101L), org.mockito.ArgumentMatchers.any(ReviewUpdateRequest.class)))
                .thenReturn(reviewResponse());

        // when & then
        mockMvc.perform(patch("/api/v1/reviews/{reviewId}", 101L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정한 감상\",\"isSpoiler\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(101))
                .andDo(document("review-update",
                        pathParameters(parameterWithName("reviewId").description("감상 ID")),
                        requestFields(REVIEW_UPDATE_REQUEST_FIELDS),
                        responseFields(reviewResponseFields("")),
                        resource(ResourceSnippetParameters.builder()
                                .summary("감상 수정")
                                .description("작성자가 감상의 지정된 필드만 수정한다")
                                .tag(REVIEW_TAG)
                                .pathParameters(pathParameter("reviewId", "감상 ID"))
                                .requestFields(REVIEW_UPDATE_REQUEST_FIELDS)
                                .responseFields(reviewResponseFields(""))
                                .build())));
    }

    @Test
    @DisplayName("감상을 soft delete한다")
    void should_ReturnNoContent_When_DeletingReview() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/reviews/{reviewId}", 101L))
                .andExpect(status().isNoContent())
                .andDo(document("review-delete",
                        pathParameters(parameterWithName("reviewId").description("감상 ID")),
                        resource(ResourceSnippetParameters.builder()
                                .summary("감상 삭제")
                                .description("작성자가 감상을 soft delete한다")
                                .tag(REVIEW_TAG)
                                .pathParameters(pathParameter("reviewId", "감상 ID"))
                                .build())));

        verify(reviewService).deleteReview(101L);
    }

    @Test
    @DisplayName("답글 목록을 조회한다")
    void should_ReturnReplyPage_When_FindingReplies() throws Exception {
        // given
        when(reviewService.findReplies(101L, 1)).thenReturn(new PageResponse<>(1, null, List.of(replyResponse())));

        // when & then
        mockMvc.perform(get("/api/v1/reviews/{reviewId}/replies", 101L).param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].replyId").value(201))
                .andDo(document("reply-list",
                        pathParameters(parameterWithName("reviewId").description("감상 ID")),
                        queryParameters(parameterWithName("page").description("1부터 시작하는 페이지 번호")),
                        responseFields(pageReplyResponseFields()),
                        resource(ResourceSnippetParameters.builder()
                                .summary("답글 목록 조회")
                                .description("감상의 답글을 작성일 오름차순으로 조회한다")
                                .tag(REVIEW_TAG)
                                .pathParameters(pathParameter("reviewId", "감상 ID"))
                                .queryParameters(queryParameter("page", SimpleType.INTEGER, "1부터 시작하는 페이지 번호"))
                                .responseFields(pageReplyResponseFields())
                                .build())));
    }

    @Test
    @DisplayName("답글을 작성하고 Location을 반환한다")
    void should_CreateReplyWithLocation_When_RequestIsValid() throws Exception {
        // given
        when(reviewService.createReply(org.mockito.ArgumentMatchers.eq(101L), org.mockito.ArgumentMatchers.any(ReplyCreateRequest.class)))
                .thenReturn(replyResponse());

        // when & then
        mockMvc.perform(post("/api/v1/reviews/{reviewId}/replies", 101L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"저도 그 부분이 좋았어요.\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/replies/201"))
                .andExpect(jsonPath("$.replyId").value(201))
                .andDo(document("reply-create",
                        pathParameters(parameterWithName("reviewId").description("감상 ID")),
                        requestFields(REPLY_REQUEST_FIELDS),
                        responseHeaders(LOCATION_HEADER),
                        responseFields(replyResponseFields("")),
                        resource(ResourceSnippetParameters.builder()
                                .summary("답글 작성")
                                .description("감상에 답글을 작성한다")
                                .tag(REVIEW_TAG)
                                .pathParameters(pathParameter("reviewId", "감상 ID"))
                                .requestFields(REPLY_REQUEST_FIELDS)
                                .responseHeaders(LOCATION_HEADER)
                                .responseFields(replyResponseFields(""))
                                .build())));
    }

    @Test
    @DisplayName("답글을 수정한다")
    void should_ReturnUpdatedReply_When_UpdatingReply() throws Exception {
        // given
        when(reviewService.updateReply(org.mockito.ArgumentMatchers.eq(201L), org.mockito.ArgumentMatchers.any(ReplyUpdateRequest.class)))
                .thenReturn(replyResponse());

        // when & then
        mockMvc.perform(patch("/api/v1/replies/{replyId}", 201L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정한 답글\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replyId").value(201))
                .andDo(document("reply-update",
                        pathParameters(parameterWithName("replyId").description("답글 ID")),
                        requestFields(REPLY_REQUEST_FIELDS),
                        responseFields(replyResponseFields("")),
                        resource(ResourceSnippetParameters.builder()
                                .summary("답글 수정")
                                .description("작성자가 답글 내용을 수정한다")
                                .tag(REVIEW_TAG)
                                .pathParameters(pathParameter("replyId", "답글 ID"))
                                .requestFields(REPLY_REQUEST_FIELDS)
                                .responseFields(replyResponseFields(""))
                                .build())));
    }

    @Test
    @DisplayName("답글을 soft delete한다")
    void should_ReturnNoContent_When_DeletingReply() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/replies/{replyId}", 201L))
                .andExpect(status().isNoContent())
                .andDo(document("reply-delete",
                        pathParameters(parameterWithName("replyId").description("답글 ID")),
                        resource(ResourceSnippetParameters.builder()
                                .summary("답글 삭제")
                                .description("작성자가 답글을 soft delete한다")
                                .tag(REVIEW_TAG)
                                .pathParameters(pathParameter("replyId", "답글 ID"))
                                .build())));

        verify(reviewService).deleteReply(201L);
    }

    @Test
    @DisplayName("감상 좋아요를 생성한다")
    void should_CreateReviewReaction_When_ReactionDoesNotExist() throws Exception {
        // given
        when(reviewService.createReviewReaction(101L)).thenReturn(new ReactionResponse(13, true));

        // when & then
        mockMvc.perform(post("/api/v1/reviews/{reviewId}/reactions", 101L))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.likedByMe").value(true))
                .andDo(reactionDocument("review-reaction-create", "감상 좋아요", "감상에 좋아요를 남긴다", "reviewId", "감상 ID"));
    }

    @Test
    @DisplayName("감상 좋아요를 취소한다")
    void should_ReturnNoContent_When_DeletingReviewReaction() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/reviews/{reviewId}/reactions", 101L))
                .andExpect(status().isNoContent())
                .andDo(noContentDocument("review-reaction-delete", "감상 좋아요 취소", "감상의 좋아요를 취소한다", "reviewId", "감상 ID"));

        verify(reviewService).deleteReviewReaction(101L);
    }

    @Test
    @DisplayName("답글 좋아요를 생성한다")
    void should_CreateReplyReaction_When_ReactionDoesNotExist() throws Exception {
        // given
        when(reviewService.createReplyReaction(201L)).thenReturn(new ReactionResponse(3, true));

        // when & then
        mockMvc.perform(post("/api/v1/replies/{replyId}/reactions", 201L))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.likeCount").value(3))
                .andDo(reactionDocument("reply-reaction-create", "답글 좋아요", "답글에 좋아요를 남긴다", "replyId", "답글 ID"));
    }

    @Test
    @DisplayName("답글 좋아요를 취소한다")
    void should_ReturnNoContent_When_DeletingReplyReaction() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/replies/{replyId}/reactions", 201L))
                .andExpect(status().isNoContent())
                .andDo(noContentDocument("reply-reaction-delete", "답글 좋아요 취소", "답글의 좋아요를 취소한다", "replyId", "답글 ID"));

        verify(reviewService).deleteReplyReaction(201L);
    }

    @Test
    @DisplayName("감상 목록 페이지가 유효하지 않으면 ProblemDetail을 반환한다")
    void should_ReturnBadRequest_When_ReviewListPageIsInvalid() throws Exception {
        // when & then
        expectProblemDetail(mockMvc.perform(get("/api/v1/books/{bookId}/reviews", 42L).param("page", "0")),
                HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "/api/v1/books/42/reviews")
                .andDo(problemDetailDocument("review-list-invalid-request", "감상 목록 조회",
                        "도서의 감상을 페이지와 피드·정렬 조건으로 조회한다", "bookId", "도서 ID"));

        verifyNoInteractions(reviewService);
    }

    @Test
    @DisplayName("인증 없이 감상을 작성하면 ProblemDetail을 반환한다")
    void should_ReturnUnauthorized_When_CreatingReviewWithoutAuthentication() throws Exception {
        // given
        when(reviewService.createReview(org.mockito.ArgumentMatchers.eq(42L), org.mockito.ArgumentMatchers.any(ReviewCreateRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED));

        // when & then
        expectProblemDetail(mockMvc.perform(post("/api/v1/books/{bookId}/reviews", 42L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"감상\"}")),
                HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "/api/v1/books/42/reviews")
                .andDo(problemDetailDocument("review-create-unauthorized", "감상 작성",
                        "도서에 감상을 작성하고 필요한 경우 서재 진도를 갱신한다", "bookId", "도서 ID"));
    }

    @Test
    @DisplayName("다른 회원의 감상을 수정하면 ProblemDetail을 반환한다")
    void should_ReturnForbidden_When_UpdatingAnotherMembersReview() throws Exception {
        // given
        when(reviewService.updateReview(org.mockito.ArgumentMatchers.eq(101L), org.mockito.ArgumentMatchers.any(ReviewUpdateRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

        // when & then
        expectProblemDetail(mockMvc.perform(patch("/api/v1/reviews/{reviewId}", 101L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"수정\"}")),
                HttpStatus.FORBIDDEN, "FORBIDDEN", "/api/v1/reviews/101")
                .andDo(problemDetailDocument("review-update-forbidden", "감상 수정",
                        "작성자가 감상의 지정된 필드만 수정한다", "reviewId", "감상 ID"));
    }

    @Test
    @DisplayName("없는 감상의 답글을 조회하면 ProblemDetail을 반환한다")
    void should_ReturnNotFound_When_FindingRepliesForMissingReview() throws Exception {
        // given
        when(reviewService.findReplies(101L, 1)).thenThrow(new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        // when & then
        expectProblemDetail(mockMvc.perform(get("/api/v1/reviews/{reviewId}/replies", 101L).param("page", "1")),
                HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND", "/api/v1/reviews/101/replies")
                .andDo(problemDetailDocument("reply-list-review-not-found", "답글 목록 조회",
                        "감상의 답글을 작성일 오름차순으로 조회한다", "reviewId", "감상 ID"));
    }

    @Test
    @DisplayName("중복 감상 좋아요는 ProblemDetail을 반환한다")
    void should_ReturnConflict_When_CreatingDuplicateReviewReaction() throws Exception {
        // given
        when(reviewService.createReviewReaction(101L)).thenThrow(new BusinessException(ErrorCode.REACTION_ALREADY_EXISTS));

        // when & then
        expectProblemDetail(mockMvc.perform(post("/api/v1/reviews/{reviewId}/reactions", 101L)),
                HttpStatus.CONFLICT, "REACTION_ALREADY_EXISTS", "/api/v1/reviews/101/reactions")
                .andDo(problemDetailDocument("review-reaction-create-conflict", "감상 좋아요", "감상에 좋아요를 남긴다",
                        "reviewId", "감상 ID"));
    }

    @Test
    @DisplayName("내 감상 피드는 인증이 필요함을 문서화한다")
    void should_ReturnUnauthorized_When_FindingMyReviewsWithoutAuthentication() throws Exception {
        // given
        when(reviewService.findReviews(42L, 1, Feed.MINE, ReviewSort.PAGE))
                .thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED));

        // when & then
        documentProblemDetail(mockMvc.perform(get("/api/v1/books/{bookId}/reviews", 42L)
                        .param("page", "1").param("feed", "MINE")),
                HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "/api/v1/books/42/reviews",
                "review-list-mine-unauthorized", "감상 목록 조회", "도서의 감상을 페이지와 피드·정렬 조건으로 조회한다",
                "bookId", "도서 ID");
    }

    @Test
    @DisplayName("감상 작성의 입력과 도서 및 페이지 오류를 문서화한다")
    void should_DocumentReviewCreateErrors_When_RequestCannotBeProcessed() throws Exception {
        // given
        when(reviewService.createReview(org.mockito.ArgumentMatchers.eq(404L), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BusinessException(ErrorCode.BOOK_NOT_FOUND));
        when(reviewService.createReview(org.mockito.ArgumentMatchers.eq(409L), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BusinessException(ErrorCode.TOTAL_PAGES_CONFLICT));
        when(reviewService.createReview(org.mockito.ArgumentMatchers.eq(422L), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BusinessException(ErrorCode.INVALID_READING_STATE));

        // when & then
        documentProblemDetail(mockMvc.perform(post("/api/v1/books/{bookId}/reviews", 42L)
                        .contentType(MediaType.APPLICATION_JSON).content("{}")),
                HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "/api/v1/books/42/reviews",
                "review-create-invalid-request", "감상 작성", "도서에 감상을 작성하고 필요한 경우 서재 진도를 갱신한다",
                "bookId", "도서 ID");
        documentProblemDetail(mockMvc.perform(post("/api/v1/books/{bookId}/reviews", 404L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"감상\"}")),
                HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", "/api/v1/books/404/reviews",
                "review-create-book-not-found", "감상 작성", "도서에 감상을 작성하고 필요한 경우 서재 진도를 갱신한다",
                "bookId", "도서 ID");
        documentProblemDetail(mockMvc.perform(post("/api/v1/books/{bookId}/reviews", 409L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"감상\"}")),
                HttpStatus.CONFLICT, "TOTAL_PAGES_CONFLICT", "/api/v1/books/409/reviews",
                "review-create-total-pages-conflict", "감상 작성", "도서에 감상을 작성하고 필요한 경우 서재 진도를 갱신한다",
                "bookId", "도서 ID");
        documentProblemDetail(mockMvc.perform(post("/api/v1/books/{bookId}/reviews", 422L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"감상\"}")),
                HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_READING_STATE", "/api/v1/books/422/reviews",
                "review-create-invalid-reading-state", "감상 작성", "도서에 감상을 작성하고 필요한 경우 서재 진도를 갱신한다",
                "bookId", "도서 ID");
    }

    @Test
    @DisplayName("감상 수정의 입력과 리소스 및 페이지 오류를 문서화한다")
    void should_DocumentReviewUpdateErrors_When_RequestCannotBeProcessed() throws Exception {
        // given
        when(reviewService.updateReview(org.mockito.ArgumentMatchers.eq(400L), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BusinessException(ErrorCode.INVALID_REQUEST));
        when(reviewService.updateReview(org.mockito.ArgumentMatchers.eq(404L), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
        when(reviewService.updateReview(org.mockito.ArgumentMatchers.eq(409L), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BusinessException(ErrorCode.TOTAL_PAGES_CONFLICT));
        when(reviewService.updateReview(org.mockito.ArgumentMatchers.eq(410L), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BusinessException(ErrorCode.DELETED_RESOURCE));
        when(reviewService.updateReview(org.mockito.ArgumentMatchers.eq(422L), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BusinessException(ErrorCode.INVALID_READING_STATE));

        // when & then
        documentProblemDetail(mockMvc.perform(patch("/api/v1/reviews/{reviewId}", 400L)
                        .contentType(MediaType.APPLICATION_JSON).content("{}")),
                HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "/api/v1/reviews/400",
                "review-update-invalid-request", "감상 수정", "작성자가 감상의 지정된 필드만 수정한다", "reviewId", "감상 ID");
        documentProblemDetail(mockMvc.perform(patch("/api/v1/reviews/{reviewId}", 404L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"감상\"}")),
                HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND", "/api/v1/reviews/404",
                "review-update-not-found", "감상 수정", "작성자가 감상의 지정된 필드만 수정한다", "reviewId", "감상 ID");
        documentProblemDetail(mockMvc.perform(patch("/api/v1/reviews/{reviewId}", 409L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"currentPage\":100}")),
                HttpStatus.CONFLICT, "TOTAL_PAGES_CONFLICT", "/api/v1/reviews/409",
                "review-update-total-pages-conflict", "감상 수정", "작성자가 감상의 지정된 필드만 수정한다", "reviewId", "감상 ID");
        documentProblemDetail(mockMvc.perform(patch("/api/v1/reviews/{reviewId}", 410L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"감상\"}")),
                HttpStatus.CONFLICT, "DELETED_RESOURCE", "/api/v1/reviews/410",
                "review-update-deleted-resource", "감상 수정", "작성자가 감상의 지정된 필드만 수정한다", "reviewId", "감상 ID");
        documentProblemDetail(mockMvc.perform(patch("/api/v1/reviews/{reviewId}", 422L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"currentPage\":100}")),
                HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_READING_STATE", "/api/v1/reviews/422",
                "review-update-invalid-reading-state", "감상 수정", "작성자가 감상의 지정된 필드만 수정한다", "reviewId", "감상 ID");
    }

    @Test
    @DisplayName("감상 삭제의 권한과 리소스 오류를 문서화한다")
    void should_DocumentReviewDeleteErrors_When_RequestCannotBeProcessed() throws Exception {
        // given
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.FORBIDDEN)).when(reviewService).deleteReview(403L);
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.REVIEW_NOT_FOUND)).when(reviewService).deleteReview(404L);
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.DELETED_RESOURCE)).when(reviewService).deleteReview(409L);

        // when & then
        documentProblemDetail(mockMvc.perform(delete("/api/v1/reviews/{reviewId}", 403L)), HttpStatus.FORBIDDEN,
                "FORBIDDEN", "/api/v1/reviews/403", "review-delete-forbidden", "감상 삭제",
                "작성자가 감상을 soft delete한다", "reviewId", "감상 ID");
        documentProblemDetail(mockMvc.perform(delete("/api/v1/reviews/{reviewId}", 404L)), HttpStatus.NOT_FOUND,
                "REVIEW_NOT_FOUND", "/api/v1/reviews/404", "review-delete-not-found", "감상 삭제",
                "작성자가 감상을 soft delete한다", "reviewId", "감상 ID");
        documentProblemDetail(mockMvc.perform(delete("/api/v1/reviews/{reviewId}", 409L)), HttpStatus.CONFLICT,
                "DELETED_RESOURCE", "/api/v1/reviews/409", "review-delete-deleted-resource", "감상 삭제",
                "작성자가 감상을 soft delete한다", "reviewId", "감상 ID");
    }

    @Test
    @DisplayName("답글 작성의 입력과 부모 감상 오류를 문서화한다")
    void should_DocumentReplyCreateErrors_When_RequestCannotBeProcessed() throws Exception {
        // given
        when(reviewService.createReply(org.mockito.ArgumentMatchers.eq(404L), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
        when(reviewService.createReply(org.mockito.ArgumentMatchers.eq(409L), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BusinessException(ErrorCode.DELETED_RESOURCE));

        // when & then
        documentProblemDetail(mockMvc.perform(post("/api/v1/reviews/{reviewId}/replies", 101L)
                        .contentType(MediaType.APPLICATION_JSON).content("{}")), HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST", "/api/v1/reviews/101/replies", "reply-create-invalid-request", "답글 작성",
                "감상에 답글을 작성한다", "reviewId", "감상 ID");
        documentProblemDetail(mockMvc.perform(post("/api/v1/reviews/{reviewId}/replies", 404L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"답글\"}")), HttpStatus.NOT_FOUND,
                "REVIEW_NOT_FOUND", "/api/v1/reviews/404/replies", "reply-create-review-not-found", "답글 작성",
                "감상에 답글을 작성한다", "reviewId", "감상 ID");
        documentProblemDetail(mockMvc.perform(post("/api/v1/reviews/{reviewId}/replies", 409L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"답글\"}")), HttpStatus.CONFLICT,
                "DELETED_RESOURCE", "/api/v1/reviews/409/replies", "reply-create-deleted-resource", "답글 작성",
                "감상에 답글을 작성한다", "reviewId", "감상 ID");
    }

    @Test
    @DisplayName("답글 수정과 삭제의 리소스 오류를 문서화한다")
    void should_DocumentReplyMutationErrors_When_RequestCannotBeProcessed() throws Exception {
        // given
        when(reviewService.updateReply(org.mockito.ArgumentMatchers.eq(400L), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BusinessException(ErrorCode.INVALID_REQUEST));
        when(reviewService.updateReply(org.mockito.ArgumentMatchers.eq(403L), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN));
        when(reviewService.updateReply(org.mockito.ArgumentMatchers.eq(404L), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BusinessException(ErrorCode.REPLY_NOT_FOUND));
        when(reviewService.updateReply(org.mockito.ArgumentMatchers.eq(409L), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BusinessException(ErrorCode.DELETED_RESOURCE));
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.FORBIDDEN)).when(reviewService).deleteReply(403L);
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.REPLY_NOT_FOUND)).when(reviewService).deleteReply(404L);
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.DELETED_RESOURCE)).when(reviewService).deleteReply(409L);

        // when & then
        documentProblemDetail(mockMvc.perform(patch("/api/v1/replies/{replyId}", 400L)
                        .contentType(MediaType.APPLICATION_JSON).content("{}")), HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST", "/api/v1/replies/400", "reply-update-invalid-request", "답글 수정",
                "작성자가 답글 내용을 수정한다", "replyId", "답글 ID");
        documentProblemDetail(mockMvc.perform(patch("/api/v1/replies/{replyId}", 403L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"답글\"}")), HttpStatus.FORBIDDEN,
                "FORBIDDEN", "/api/v1/replies/403", "reply-update-forbidden", "답글 수정",
                "작성자가 답글 내용을 수정한다", "replyId", "답글 ID");
        documentProblemDetail(mockMvc.perform(patch("/api/v1/replies/{replyId}", 404L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"답글\"}")), HttpStatus.NOT_FOUND,
                "REPLY_NOT_FOUND", "/api/v1/replies/404", "reply-update-not-found", "답글 수정",
                "작성자가 답글 내용을 수정한다", "replyId", "답글 ID");
        documentProblemDetail(mockMvc.perform(patch("/api/v1/replies/{replyId}", 409L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"답글\"}")), HttpStatus.CONFLICT,
                "DELETED_RESOURCE", "/api/v1/replies/409", "reply-update-deleted-resource", "답글 수정",
                "작성자가 답글 내용을 수정한다", "replyId", "답글 ID");
        documentProblemDetail(mockMvc.perform(delete("/api/v1/replies/{replyId}", 403L)), HttpStatus.FORBIDDEN,
                "FORBIDDEN", "/api/v1/replies/403", "reply-delete-forbidden", "답글 삭제",
                "작성자가 답글을 soft delete한다", "replyId", "답글 ID");
        documentProblemDetail(mockMvc.perform(delete("/api/v1/replies/{replyId}", 404L)), HttpStatus.NOT_FOUND,
                "REPLY_NOT_FOUND", "/api/v1/replies/404", "reply-delete-not-found", "답글 삭제",
                "작성자가 답글을 soft delete한다", "replyId", "답글 ID");
        documentProblemDetail(mockMvc.perform(delete("/api/v1/replies/{replyId}", 409L)), HttpStatus.CONFLICT,
                "DELETED_RESOURCE", "/api/v1/replies/409", "reply-delete-deleted-resource", "답글 삭제",
                "작성자가 답글을 soft delete한다", "replyId", "답글 ID");
    }

    @Test
    @DisplayName("좋아요 생성과 취소의 대상 오류를 문서화한다")
    void should_DocumentReactionErrors_When_TargetCannotBeProcessed() throws Exception {
        // given
        when(reviewService.createReviewReaction(404L)).thenThrow(new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
        when(reviewService.createReviewReaction(409L)).thenThrow(new BusinessException(ErrorCode.DELETED_RESOURCE));
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.REVIEW_NOT_FOUND)).when(reviewService)
                .deleteReviewReaction(404L);
        when(reviewService.createReplyReaction(404L)).thenThrow(new BusinessException(ErrorCode.REPLY_NOT_FOUND));
        when(reviewService.createReplyReaction(409L)).thenThrow(new BusinessException(ErrorCode.DELETED_RESOURCE));
        when(reviewService.createReplyReaction(410L)).thenThrow(new BusinessException(ErrorCode.REACTION_ALREADY_EXISTS));
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.REPLY_NOT_FOUND)).when(reviewService)
                .deleteReplyReaction(404L);

        // when & then
        documentProblemDetail(mockMvc.perform(post("/api/v1/reviews/{reviewId}/reactions", 404L)), HttpStatus.NOT_FOUND,
                "REVIEW_NOT_FOUND", "/api/v1/reviews/404/reactions", "review-reaction-create-not-found", "감상 좋아요",
                "감상에 좋아요를 남긴다", "reviewId", "감상 ID");
        documentProblemDetail(mockMvc.perform(post("/api/v1/reviews/{reviewId}/reactions", 409L)), HttpStatus.CONFLICT,
                "DELETED_RESOURCE", "/api/v1/reviews/409/reactions", "review-reaction-create-deleted-resource", "감상 좋아요",
                "감상에 좋아요를 남긴다", "reviewId", "감상 ID");
        documentProblemDetail(mockMvc.perform(delete("/api/v1/reviews/{reviewId}/reactions", 404L)), HttpStatus.NOT_FOUND,
                "REVIEW_NOT_FOUND", "/api/v1/reviews/404/reactions", "review-reaction-delete-not-found", "감상 좋아요 취소",
                "감상의 좋아요를 취소한다", "reviewId", "감상 ID");
        documentProblemDetail(mockMvc.perform(post("/api/v1/replies/{replyId}/reactions", 404L)), HttpStatus.NOT_FOUND,
                "REPLY_NOT_FOUND", "/api/v1/replies/404/reactions", "reply-reaction-create-not-found", "답글 좋아요",
                "답글에 좋아요를 남긴다", "replyId", "답글 ID");
        documentProblemDetail(mockMvc.perform(post("/api/v1/replies/{replyId}/reactions", 409L)), HttpStatus.CONFLICT,
                "DELETED_RESOURCE", "/api/v1/replies/409/reactions", "reply-reaction-create-deleted-resource", "답글 좋아요",
                "답글에 좋아요를 남긴다", "replyId", "답글 ID");
        documentProblemDetail(mockMvc.perform(post("/api/v1/replies/{replyId}/reactions", 410L)), HttpStatus.CONFLICT,
                "REACTION_ALREADY_EXISTS", "/api/v1/replies/410/reactions", "reply-reaction-create-conflict", "답글 좋아요",
                "답글에 좋아요를 남긴다", "replyId", "답글 ID");
        documentProblemDetail(mockMvc.perform(delete("/api/v1/replies/{replyId}/reactions", 404L)), HttpStatus.NOT_FOUND,
                "REPLY_NOT_FOUND", "/api/v1/replies/404/reactions", "reply-reaction-delete-not-found", "답글 좋아요 취소",
                "답글의 좋아요를 취소한다", "replyId", "답글 ID");
    }

    @Test
    @DisplayName("목록 대상과 페이지 오류를 문서화한다")
    void should_DocumentListErrors_When_TargetOrPageIsInvalid() throws Exception {
        // given
        when(reviewService.findReviews(404L, 1, Feed.ALL, ReviewSort.PAGE))
                .thenThrow(new BusinessException(ErrorCode.BOOK_NOT_FOUND));

        // when & then
        documentProblemDetail(mockMvc.perform(get("/api/v1/books/{bookId}/reviews", 404L)
                        .param("page", "1")), HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", "/api/v1/books/404/reviews",
                "review-list-book-not-found", "감상 목록 조회", "도서의 감상을 페이지와 피드·정렬 조건으로 조회한다",
                "bookId", "도서 ID");
        documentProblemDetail(mockMvc.perform(get("/api/v1/reviews/{reviewId}/replies", 101L)
                        .param("page", "0")), HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "/api/v1/reviews/101/replies",
                "reply-list-invalid-request", "답글 목록 조회", "감상의 답글을 작성일 오름차순으로 조회한다",
                "reviewId", "감상 ID");
    }

    @Test
    @DisplayName("인증 필수 감상과 답글 작업의 401 응답을 문서화한다")
    void should_DocumentUnauthorizedForAuthenticatedOperations_When_NotAuthenticated() throws Exception {
        // given
        when(reviewService.updateReview(org.mockito.ArgumentMatchers.eq(101L), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED));
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.UNAUTHORIZED)).when(reviewService).deleteReview(101L);
        when(reviewService.createReply(org.mockito.ArgumentMatchers.eq(101L), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED));
        when(reviewService.updateReply(org.mockito.ArgumentMatchers.eq(201L), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED));
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.UNAUTHORIZED)).when(reviewService).deleteReply(201L);
        when(reviewService.createReviewReaction(101L)).thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED));
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.UNAUTHORIZED)).when(reviewService)
                .deleteReviewReaction(101L);
        when(reviewService.createReplyReaction(201L)).thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED));
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.UNAUTHORIZED)).when(reviewService)
                .deleteReplyReaction(201L);

        // when & then
        documentProblemDetail(mockMvc.perform(patch("/api/v1/reviews/{reviewId}", 101L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"감상\"}")),
                HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "/api/v1/reviews/101", "review-update-unauthorized", "감상 수정",
                "작성자가 감상의 지정된 필드만 수정한다", "reviewId", "감상 ID");
        documentProblemDetail(mockMvc.perform(delete("/api/v1/reviews/{reviewId}", 101L)),
                HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "/api/v1/reviews/101", "review-delete-unauthorized", "감상 삭제",
                "작성자가 감상을 soft delete한다", "reviewId", "감상 ID");
        documentProblemDetail(mockMvc.perform(post("/api/v1/reviews/{reviewId}/replies", 101L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"답글\"}")),
                HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "/api/v1/reviews/101/replies", "reply-create-unauthorized", "답글 작성",
                "감상에 답글을 작성한다", "reviewId", "감상 ID");
        documentProblemDetail(mockMvc.perform(patch("/api/v1/replies/{replyId}", 201L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"답글\"}")),
                HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "/api/v1/replies/201", "reply-update-unauthorized", "답글 수정",
                "작성자가 답글 내용을 수정한다", "replyId", "답글 ID");
        documentProblemDetail(mockMvc.perform(delete("/api/v1/replies/{replyId}", 201L)),
                HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "/api/v1/replies/201", "reply-delete-unauthorized", "답글 삭제",
                "작성자가 답글을 soft delete한다", "replyId", "답글 ID");
        documentProblemDetail(mockMvc.perform(post("/api/v1/reviews/{reviewId}/reactions", 101L)),
                HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "/api/v1/reviews/101/reactions",
                "review-reaction-create-unauthorized", "감상 좋아요", "감상에 좋아요를 남긴다", "reviewId", "감상 ID");
        documentProblemDetail(mockMvc.perform(delete("/api/v1/reviews/{reviewId}/reactions", 101L)),
                HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "/api/v1/reviews/101/reactions",
                "review-reaction-delete-unauthorized", "감상 좋아요 취소", "감상의 좋아요를 취소한다", "reviewId", "감상 ID");
        documentProblemDetail(mockMvc.perform(post("/api/v1/replies/{replyId}/reactions", 201L)),
                HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "/api/v1/replies/201/reactions",
                "reply-reaction-create-unauthorized", "답글 좋아요", "답글에 좋아요를 남긴다", "replyId", "답글 ID");
        documentProblemDetail(mockMvc.perform(delete("/api/v1/replies/{replyId}/reactions", 201L)),
                HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "/api/v1/replies/201/reactions",
                "reply-reaction-delete-unauthorized", "답글 좋아요 취소", "답글의 좋아요를 취소한다", "replyId", "답글 ID");
    }

    private RestDocumentationResultHandler reactionDocument(
            String identifier,
            String summary,
            String description,
            String pathName,
            String pathDescription
    ) {
        return document(identifier,
                pathParameters(parameterWithName(pathName).description(pathDescription)),
                responseFields(REACTION_RESPONSE_FIELDS),
                resource(ResourceSnippetParameters.builder()
                        .summary(summary)
                        .description(description)
                        .tag(REVIEW_TAG)
                        .pathParameters(pathParameter(pathName, pathDescription))
                        .responseFields(REACTION_RESPONSE_FIELDS)
                        .build()));
    }

    private RestDocumentationResultHandler noContentDocument(
            String identifier,
            String summary,
            String description,
            String pathName,
            String pathDescription
    ) {
        return document(identifier,
                pathParameters(parameterWithName(pathName).description(pathDescription)),
                resource(ResourceSnippetParameters.builder()
                        .summary(summary)
                        .description(description)
                        .tag(REVIEW_TAG)
                        .pathParameters(pathParameter(pathName, pathDescription))
                        .build()));
    }

    private RestDocumentationResultHandler problemDetailDocument(
            String identifier,
            String summary,
            String description,
            String pathName,
            String pathDescription
    ) {
        return document(identifier,
                pathParameters(parameterWithName(pathName).description(pathDescription)),
                responseFields(PROBLEM_DETAIL_FIELDS),
                resource(ResourceSnippetParameters.builder()
                        .summary(summary)
                        .description(description)
                        .tag(REVIEW_TAG)
                        .pathParameters(pathParameter(pathName, pathDescription))
                        .responseSchema(Schema.schema("ProblemDetail"))
                        .responseFields(PROBLEM_DETAIL_FIELDS)
                        .build()));
    }

    private ResultActions expectProblemDetail(ResultActions result, HttpStatus expectedStatus, String code, String instance)
            throws Exception {
        return result
                .andExpect(status().is(expectedStatus.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value(expectedStatus.getReasonPhrase()))
                .andExpect(jsonPath("$.status").value(expectedStatus.value()))
                .andExpect(jsonPath("$.instance").value(instance))
                .andExpect(jsonPath("$.code").value(code));
    }

    private void documentProblemDetail(
            ResultActions result,
            HttpStatus expectedStatus,
            String code,
            String instance,
            String identifier,
            String summary,
            String description,
            String pathName,
            String pathDescription
    ) throws Exception {
        expectProblemDetail(result, expectedStatus, code, instance)
                .andDo(problemDetailDocument(identifier, summary, description, pathName, pathDescription));
    }

    private static FieldDescriptor[] pageReviewResponseFields() {
        return prependPageFields(reviewResponseFields("items[]"));
    }

    private static FieldDescriptor[] pageReplyResponseFields() {
        return prependPageFields(replyResponseFields("items[]"));
    }

    private static FieldDescriptor[] prependPageFields(FieldDescriptor[] itemFields) {
        FieldDescriptor[] fields = new FieldDescriptor[itemFields.length + 3];
        fields[0] = fieldWithPath("totalCount").type(JsonFieldType.NUMBER).description("전체 항목 수");
        fields[1] = fieldWithPath("nextPage").type(JsonFieldType.NUMBER).description("다음 페이지 번호. 마지막 페이지면 null").optional();
        fields[2] = fieldWithPath("items").type(JsonFieldType.ARRAY).description("페이지 항목 목록");
        System.arraycopy(itemFields, 0, fields, 3, itemFields.length);
        return fields;
    }

    private static FieldDescriptor[] reviewResponseFields(String prefix) {
        FieldDescriptor[] reviewFields = {
                field(prefix, "reviewId", JsonFieldType.NUMBER, "감상 ID"),
                field(prefix, "content", JsonFieldType.STRING, "감상 내용"),
                field(prefix, "quote", JsonFieldType.STRING, "인용문").optional(),
                field(prefix, "chapter", JsonFieldType.STRING, "챕터").optional(),
                field(prefix, "currentPage", JsonFieldType.NUMBER, "감상을 남긴 현재 페이지").optional(),
                field(prefix, "isSpoiler", JsonFieldType.BOOLEAN, "스포일러 여부"),
                field(prefix, "deleted", JsonFieldType.BOOLEAN, "soft delete 여부"),
                field(prefix, "createdAt", JsonFieldType.STRING, "작성 시각(UTC)"),
                field(prefix, "author", JsonFieldType.OBJECT, "작성자 정보"),
                field(prefix, "author.displayName", JsonFieldType.STRING, "작성자 표시 이름"),
                field(prefix, "author.profileImageUrl", JsonFieldType.STRING, "작성자 프로필 이미지 URL").optional(),
                field(prefix, "author.anonymous", JsonFieldType.BOOLEAN, "익명 작성 여부"),
                field(prefix, "author.mine", JsonFieldType.BOOLEAN, "내가 작성한 감상인지 여부"),
                field(prefix, "likeCount", JsonFieldType.NUMBER, "좋아요 수"),
                field(prefix, "likedByMe", JsonFieldType.BOOLEAN, "내가 좋아요를 눌렀는지 여부"),
                field(prefix, "replyCount", JsonFieldType.NUMBER, "답글 수"),
                field(prefix, "recentReplies", JsonFieldType.ARRAY, "최근 답글 최대 3개")
        };
        FieldDescriptor[] recentReplyFields = replyResponseFields(path(prefix, "recentReplies[]"));
        FieldDescriptor[] fields = new FieldDescriptor[reviewFields.length + recentReplyFields.length];
        System.arraycopy(reviewFields, 0, fields, 0, reviewFields.length);
        System.arraycopy(recentReplyFields, 0, fields, reviewFields.length, recentReplyFields.length);
        return fields;
    }

    private static FieldDescriptor[] replyResponseFields(String prefix) {
        return new FieldDescriptor[]{
                field(prefix, "replyId", JsonFieldType.NUMBER, "답글 ID"),
                field(prefix, "content", JsonFieldType.STRING, "답글 내용"),
                field(prefix, "deleted", JsonFieldType.BOOLEAN, "soft delete 여부"),
                field(prefix, "createdAt", JsonFieldType.STRING, "작성 시각(UTC)"),
                field(prefix, "author", JsonFieldType.OBJECT, "작성자 정보"),
                field(prefix, "author.displayName", JsonFieldType.STRING, "작성자 표시 이름"),
                field(prefix, "author.profileImageUrl", JsonFieldType.STRING, "작성자 프로필 이미지 URL").optional(),
                field(prefix, "author.anonymous", JsonFieldType.BOOLEAN, "익명 작성 여부"),
                field(prefix, "author.mine", JsonFieldType.BOOLEAN, "내가 작성한 답글인지 여부"),
                field(prefix, "likeCount", JsonFieldType.NUMBER, "좋아요 수"),
                field(prefix, "likedByMe", JsonFieldType.BOOLEAN, "내가 좋아요를 눌렀는지 여부")
        };
    }

    private static FieldDescriptor field(String prefix, String name, JsonFieldType type, String description) {
        return fieldWithPath(path(prefix, name)).type(type).description(description);
    }

    private static String path(String prefix, String name) {
        return prefix.isEmpty() ? name : prefix + "." + name;
    }

    private static com.epages.restdocs.apispec.ParameterDescriptorWithType pathParameter(String name, String description) {
        return ResourceDocumentation.parameterWithName(name).type(SimpleType.INTEGER).description(description);
    }

    private static com.epages.restdocs.apispec.ParameterDescriptorWithType queryParameter(
            String name,
            SimpleType type,
            String description
    ) {
        return queryParameter(name, type, description, false);
    }

    private static com.epages.restdocs.apispec.ParameterDescriptorWithType queryParameter(
            String name,
            SimpleType type,
            String description,
            boolean optional
    ) {
        com.epages.restdocs.apispec.ParameterDescriptorWithType parameter = ResourceDocumentation.parameterWithName(name)
                .type(type).description(description);
        return optional ? parameter.optional() : parameter;
    }

    private static ReviewResponse reviewResponse() {
        return new ReviewResponse(101, "인상 깊었다.", "화성에서 살아남아야 한다.", "3장", 120,
                false, false, Instant.parse("2026-08-13T15:00:00Z"), AUTHOR, 12, false, 1,
                List.of(replyResponse()));
    }

    private static ReplyResponse replyResponse() {
        return new ReplyResponse(201, "저도 그 부분이 좋았어요.", false, Instant.parse("2026-08-13T16:00:00Z"),
                AUTHOR, 3, true);
    }
}
