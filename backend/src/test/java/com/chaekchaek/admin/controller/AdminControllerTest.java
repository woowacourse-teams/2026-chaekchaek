package com.chaekchaek.admin.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.admin.dto.RecommendedBookListResponse;
import com.chaekchaek.admin.dto.RecommendedBookResponse;
import com.chaekchaek.admin.service.AdminService;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.epages.restdocs.apispec.ParameterDescriptorWithType;
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
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(value = AdminController.class, excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class)
@AutoConfigureRestDocs
class AdminControllerTest {

    private static final long BOOK_ID = 42L;
    private static final String ISBN13 = "9788925568683";
    private static final String ADMIN_TAG = "관리자";
    private static final String LIST_SUMMARY = "추천 도서 목록 조회";
    private static final String LIST_DESCRIPTION = "관리자가 지정한 추천 도서를 최근에 추천한 순서로 조회한다. 추천한 책이 없으면 빈 배열을 반환한다";
    private static final String ADD_SUMMARY = "추천 도서 추가";
    private static final String ADD_DESCRIPTION = "관리자가 ISBN-13으로 도서를 조회해 추천 도서로 등록한다. 추천 도서는 최대 10권까지 등록할 수 있다";
    private static final String DELETE_SUMMARY = "추천 도서 삭제";
    private static final String DELETE_DESCRIPTION = "관리자가 추천 도서에서 도서를 삭제한다";
    private static final String BOOK_ID_DESCRIPTION = "추천 도서에서 삭제할 도서 ID";
    private static final FieldDescriptor[] PROBLEM_DETAIL_FIELDS = {
            fieldWithPath("type").type(JsonFieldType.STRING).description("문제 유형 URI"),
            fieldWithPath("title").type(JsonFieldType.STRING).description("HTTP 상태 설명"),
            fieldWithPath("status").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
            fieldWithPath("detail").type(JsonFieldType.STRING).description("오류 상세 메시지"),
            fieldWithPath("instance").type(JsonFieldType.STRING).description("오류가 발생한 요청 경로"),
            fieldWithPath("code").type(JsonFieldType.STRING).description("애플리케이션 오류 코드")
    };

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @Test
    @DisplayName("추천 도서 목록을 조회하고 문서화한다")
    void should_ReturnRecommendedBooks_When_FindingRecommendedBooks() throws Exception {
        // given
        when(adminService.getRecommendedBooks())
                .thenReturn(new RecommendedBookListResponse(List.of(recommendedBookResponse())));

        // when & then
        mockMvc.perform(get("/api/v1/admin/recommended-books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books[0].bookId").value(BOOK_ID))
                .andExpect(jsonPath("$.books[0].isbn13").value(ISBN13))
                .andExpect(jsonPath("$.books[0].title").value("마션"))
                .andExpect(jsonPath("$.books[0].createdAt").value("2026-08-28T00:00:00Z"))
                .andDo(document("admin-recommended-books",
                        responseFields(recommendedBookListResponseFields()),
                        resource(ResourceSnippetParameters.builder()
                                .summary(LIST_SUMMARY)
                                .description(LIST_DESCRIPTION)
                                .tag(ADMIN_TAG)
                                .responseFields(recommendedBookListResponseFields())
                                .build())));
    }

    @Test
    @DisplayName("추천한 책이 없으면 빈 목록을 반환한다")
    void should_ReturnEmptyRecommendedBooks_When_NoBookIsRecommended() throws Exception {
        // given
        when(adminService.getRecommendedBooks()).thenReturn(new RecommendedBookListResponse(List.of()));

        // when & then
        mockMvc.perform(get("/api/v1/admin/recommended-books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books").isEmpty());
    }

    @Test
    @DisplayName("관리자가 아니라면 문제 응답을 반환한다")
    void should_ReturnProblemDetail_When_ActorIsNotAdmin() throws Exception {
        // given
        when(adminService.getRecommendedBooks()).thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

        // when & then
        expectProblemDetail(mockMvc.perform(get("/api/v1/admin/recommended-books")), HttpStatus.FORBIDDEN,
                ErrorCode.FORBIDDEN, "/api/v1/admin/recommended-books")
                .andDo(problemDetailDocument("admin-recommended-books-forbidden", LIST_SUMMARY, LIST_DESCRIPTION));
    }

    @Test
    @DisplayName("유효한 요청이라면 추천 도서를 추가한다")
    void should_AddRecommendedBook_When_RequestIsValid() throws Exception {
        // given
        when(adminService.addRecommendedBookByIsbn13(new Isbn13(ISBN13))).thenReturn(recommendedBookResponse());

        // when & then
        postRecommendedBook(ISBN13)
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/admin/recommended-books/" + BOOK_ID))
                .andExpect(jsonPath("$.bookId").value(BOOK_ID))
                .andExpect(jsonPath("$.isbn13").value(ISBN13))
                .andDo(document("admin-recommended-book-add",
                        requestFields(addRecommendedBookRequestFields()),
                        responseFields(recommendedBookResponseFields()),
                        responseHeaders(headerWithName(HttpHeaders.LOCATION).description("등록된 추천 도서 URI")),
                        resource(ResourceSnippetParameters.builder()
                                .summary(ADD_SUMMARY)
                                .description(ADD_DESCRIPTION)
                                .tag(ADMIN_TAG)
                                .requestSchema(Schema.schema("AddRecommendedBookRequest"))
                                .requestFields(addRecommendedBookRequestFields())
                                .responseFields(recommendedBookResponseFields())
                                .responseHeaders(ResourceDocumentation.headerWithName(HttpHeaders.LOCATION)
                                        .type(SimpleType.STRING).description("등록된 추천 도서 URI"))
                                .build())));

        verify(adminService).addRecommendedBookByIsbn13(new Isbn13(ISBN13));
    }

    @Test
    @DisplayName("ISBN13이 유효하지 않다면 문제 응답을 반환한다")
    void should_ReturnProblemDetail_When_Isbn13IsInvalid() throws Exception {
        // when & then
        expectProblemDetail(postRecommendedBook("9788925568680"), HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_REQUEST, "/api/v1/admin/recommended-books")
                .andDo(problemDetailDocument("admin-recommended-book-add-invalid-request", ADD_SUMMARY,
                        ADD_DESCRIPTION));
    }

    @Test
    @DisplayName("추천 도서가 10권이라면 문제 응답을 반환한다")
    void should_ReturnProblemDetail_When_RecommendedBookLimitIsReached() throws Exception {
        // given
        when(adminService.addRecommendedBookByIsbn13(new Isbn13(ISBN13)))
                .thenThrow(new BusinessException(ErrorCode.RECOMMENDED_BOOK_LIMIT_EXCEEDED));

        // when & then
        expectProblemDetail(postRecommendedBook(ISBN13), HttpStatus.CONFLICT,
                ErrorCode.RECOMMENDED_BOOK_LIMIT_EXCEEDED, "/api/v1/admin/recommended-books")
                .andDo(problemDetailDocument("admin-recommended-book-add-limit-exceeded", ADD_SUMMARY,
                        ADD_DESCRIPTION));
    }

    @Test
    @DisplayName("유효한 요청이라면 추천 도서를 삭제한다")
    void should_DeleteRecommendedBook_When_RequestIsValid() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/admin/recommended-books/{bookId}", BOOK_ID))
                .andExpect(status().isNoContent())
                .andDo(document("admin-recommended-book-delete",
                        pathParameters(parameterWithName("bookId").description(BOOK_ID_DESCRIPTION)),
                        resource(ResourceSnippetParameters.builder()
                                .summary(DELETE_SUMMARY)
                                .description(DELETE_DESCRIPTION)
                                .tag(ADMIN_TAG)
                                .pathParameters(bookIdResourcePathParameter())
                                .build())));

        verify(adminService).deleteRecommendedBook(BOOK_ID);
    }

    @Test
    @DisplayName("추천 중인 책이 아니라면 문제 응답을 반환한다")
    void should_ReturnProblemDetail_When_DeletingBookThatIsNotRecommended() throws Exception {
        // given
        doThrow(new BusinessException(ErrorCode.RECOMMENDED_BOOK_NOT_FOUND))
                .when(adminService).deleteRecommendedBook(BOOK_ID);

        // when & then
        expectProblemDetail(mockMvc.perform(delete("/api/v1/admin/recommended-books/{bookId}", BOOK_ID)),
                HttpStatus.NOT_FOUND, ErrorCode.RECOMMENDED_BOOK_NOT_FOUND,
                "/api/v1/admin/recommended-books/" + BOOK_ID)
                .andDo(problemDetailDocument("admin-recommended-book-delete-not-found", DELETE_SUMMARY,
                        DELETE_DESCRIPTION, bookIdResourcePathParameter()));
    }

    private RestDocumentationResultHandler problemDetailDocument(
            String identifier, String summary, String description, ParameterDescriptorWithType... pathParameters
    ) {
        return document(identifier,
                responseFields(PROBLEM_DETAIL_FIELDS),
                resource(ResourceSnippetParameters.builder()
                        .summary(summary)
                        .description(description)
                        .tag(ADMIN_TAG)
                        .pathParameters(pathParameters)
                        .responseSchema(Schema.schema("ProblemDetail"))
                        .responseFields(PROBLEM_DETAIL_FIELDS)
                        .build()));
    }

    private static ParameterDescriptorWithType bookIdResourcePathParameter() {
        return ResourceDocumentation.parameterWithName("bookId")
                .type(SimpleType.INTEGER).description(BOOK_ID_DESCRIPTION);
    }

    private ResultActions postRecommendedBook(String isbn13) throws Exception {
        return mockMvc.perform(post("/api/v1/admin/recommended-books")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isbn13\":\"" + isbn13 + "\"}"));
    }

    private ResultActions expectProblemDetail(ResultActions result, HttpStatus status, ErrorCode errorCode,
                                              String instance) throws Exception {
        return result
                .andExpect(status().is(status.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value(status.getReasonPhrase()))
                .andExpect(jsonPath("$.status").value(status.value()))
                .andExpect(jsonPath("$.code").value(errorCode.getCode()))
                .andExpect(jsonPath("$.instance").value(instance));
    }

    private static RecommendedBookResponse recommendedBookResponse() {
        return new RecommendedBookResponse(BOOK_ID, ISBN13, "마션", "https://example.com/martian.jpg",
                List.of("앤디 위어"), Instant.parse("2026-08-28T00:00:00Z"));
    }

    private static FieldDescriptor[] addRecommendedBookRequestFields() {
        return new FieldDescriptor[]{
                fieldWithPath("isbn13").type(JsonFieldType.STRING).description("추천할 도서의 ISBN-13")
        };
    }

    private static FieldDescriptor[] recommendedBookResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("bookId").type(JsonFieldType.NUMBER).description("도서 ID"),
                fieldWithPath("isbn13").type(JsonFieldType.STRING).description("ISBN-13"),
                fieldWithPath("title").type(JsonFieldType.STRING).description("도서 제목"),
                fieldWithPath("coverImageUrl").type(JsonFieldType.STRING).description("표지 이미지 URL"),
                fieldWithPath("authors").type(JsonFieldType.ARRAY).description("저자 목록")
                        .attributes(key("itemsType").value(JsonFieldType.STRING)),
                fieldWithPath("createdAt").type(JsonFieldType.STRING).description("추천 등록 시각(UTC)")
        };
    }

    private static FieldDescriptor[] recommendedBookListResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("books").type(JsonFieldType.ARRAY).description("추천 도서 목록"),
                fieldWithPath("books[].bookId").type(JsonFieldType.NUMBER).description("도서 ID"),
                fieldWithPath("books[].isbn13").type(JsonFieldType.STRING).description("ISBN-13"),
                fieldWithPath("books[].title").type(JsonFieldType.STRING).description("도서 제목"),
                fieldWithPath("books[].coverImageUrl").type(JsonFieldType.STRING).description("표지 이미지 URL"),
                fieldWithPath("books[].authors").type(JsonFieldType.ARRAY).description("저자 목록")
                        .attributes(key("itemsType").value(JsonFieldType.STRING)),
                fieldWithPath("books[].createdAt").type(JsonFieldType.STRING).description("추천 등록 시각(UTC)")
        };
    }
}
