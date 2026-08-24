package com.chaekchaek.book.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.book.client.AladinClientException;
import com.chaekchaek.book.domain.BookSearchSort;
import com.chaekchaek.book.dto.BookItem;
import com.chaekchaek.book.dto.BookDetailResponse;
import com.chaekchaek.book.dto.BookMyRecordResponse;
import com.chaekchaek.book.dto.BookSearchResponse;
import com.chaekchaek.book.exception.BookNotFoundException;
import com.chaekchaek.book.service.BookSearchService;
import com.chaekchaek.book.service.BookService;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.epages.restdocs.apispec.ResourceDocumentation;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.epages.restdocs.apispec.SimpleType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(
        value = BookController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class
)
@AutoConfigureRestDocs
class BookControllerTest {

    private static final String BOOK_SEARCH_SUMMARY = "도서 검색";
    private static final String BOOK_SEARCH_DESCRIPTION = "도서명과 페이지 번호 및 정렬 기준으로 도서를 검색한다";
    private static final String BOOK_DETAIL_SUMMARY = "도서 상세 조회";

    private static final FieldDescriptor[] BOOK_SEARCH_RESPONSE_FIELDS = {
            fieldWithPath("totalCount").type(JsonFieldType.NUMBER)
                    .description("검색 결과의 전체 도서 수"),
            fieldWithPath("nextPage").type(JsonFieldType.NUMBER)
                    .description("다음 페이지 번호. 마지막 페이지라면 null")
                    .optional(),
            fieldWithPath("items").type(JsonFieldType.ARRAY)
                    .description("검색된 도서 목록. 한 페이지당 최대 10개"),
            fieldWithPath("items[].bookId").type(JsonFieldType.NUMBER)
                    .description("등록된 도서 ID. 미등록 도서라면 null")
                    .optional(),
            fieldWithPath("items[].isRegisteredInMyLibrary").type(JsonFieldType.BOOLEAN)
                    .description("로그인 회원의 내 서재 등록 여부. 등록되어 있으면 true, 미등록이면 false, 비로그인 요청이면 null")
                    .optional(),
            fieldWithPath("items[].title").type(JsonFieldType.STRING)
                    .description("도서 제목"),
            fieldWithPath("items[].coverImageUrl").type(JsonFieldType.STRING)
                    .description("표지 이미지 URL. 알라딘 Big 규격(너비 200px)을 따름"),
            fieldWithPath("items[].authors").type(JsonFieldType.ARRAY)
                    .description("저자 이름 목록")
                    .attributes(key("itemsType").value(JsonFieldType.STRING)),
            fieldWithPath("items[].translators").type(JsonFieldType.ARRAY)
                    .description("옮긴이 이름 목록")
                    .attributes(key("itemsType").value(JsonFieldType.STRING)),
            fieldWithPath("items[].publishedDate").type(JsonFieldType.STRING)
                    .description("출판일"),
            fieldWithPath("items[].isbn13").type(JsonFieldType.STRING)
                    .description("ISBN-13"),
            fieldWithPath("items[].category").type(JsonFieldType.STRING)
                    .description("도서 카테고리"),
            fieldWithPath("items[].publisher").type(JsonFieldType.STRING)
                    .description("출판사"),
            fieldWithPath("items[].reviewCount").type(JsonFieldType.NUMBER)
                    .description("감상 수. 미등록 도서라면 null")
                    .optional(),
            fieldWithPath("items[].replyCount").type(JsonFieldType.NUMBER)
                    .description("답글 수. 미등록 도서라면 null")
                    .optional()
    };

    private static final FieldDescriptor[] PROBLEM_DETAIL_FIELDS = {
            fieldWithPath("type").type(JsonFieldType.STRING).description("문제 유형 URI"),
            fieldWithPath("title").type(JsonFieldType.STRING).description("HTTP 상태 설명"),
            fieldWithPath("status").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
            fieldWithPath("detail").type(JsonFieldType.STRING).description("오류 상세 메시지"),
            fieldWithPath("instance").type(JsonFieldType.STRING).description("오류가 발생한 요청 경로"),
            fieldWithPath("code").type(JsonFieldType.STRING).description("애플리케이션 오류 코드")
    };

    private static final FieldDescriptor[] BOOK_DETAIL_RESPONSE_FIELDS = {
            fieldWithPath("bookId").type(JsonFieldType.NUMBER).description("도서 ID"),
            fieldWithPath("isbn13").type(JsonFieldType.STRING).description("ISBN13"),
            fieldWithPath("title").type(JsonFieldType.STRING).description("도서 제목"),
            fieldWithPath("coverImageUrl").type(JsonFieldType.STRING).description("표지 이미지 URL"),
            fieldWithPath("description").type(JsonFieldType.STRING).description("도서 상세 설명").optional(),
            fieldWithPath("authors").type(JsonFieldType.ARRAY).description("저자 목록"),
            fieldWithPath("translators").type(JsonFieldType.ARRAY).description("옮긴이 목록"),
            fieldWithPath("publisher").type(JsonFieldType.STRING).description("출판사"),
            fieldWithPath("category").type(JsonFieldType.STRING).description("카테고리"),
            fieldWithPath("publishedDate").type(JsonFieldType.STRING).description("출간일").optional(),
            fieldWithPath("totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수").optional(),
            fieldWithPath("reviewCount").type(JsonFieldType.NUMBER).description("감상 수"),
            fieldWithPath("replyCount").type(JsonFieldType.NUMBER).description("답글 수"),
            fieldWithPath("averageRating").type(JsonFieldType.NUMBER).description("평균 별점").optional(),
            fieldWithPath("ratingCount").type(JsonFieldType.NUMBER).description("별점 수"),
            fieldWithPath("myRatingCount").type(JsonFieldType.NUMBER).description("내가 별점을 등록한 도서 수").optional(),
            fieldWithPath("myRecord").type(JsonFieldType.OBJECT).description("내 서재 기록").optional(),
            fieldWithPath("myRecord.status").type(JsonFieldType.STRING).description("읽기 상태").optional(),
            fieldWithPath("myRecord.currentPage").type(JsonFieldType.NUMBER).description("현재 페이지").optional(),
            fieldWithPath("myRecord.myRating").type(JsonFieldType.NUMBER).description("내 별점").optional()
    };

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookSearchService bookSearchService;

    @MockitoBean
    private BookService bookService;

    @Test
    @DisplayName("유효한 요청이라면 도서 검색 결과를 반환한다")
    void should_ReturnBookSearchResponse_When_RequestIsValid() throws Exception {
        // given
        BookItem item = new BookItem(
                null,
                "마션",
                "https://image.aladin.co.kr/martian.jpg",
                List.of("앤디 위어"),
                List.of("박아람"),
                "2026-07-01",
                "9788925568683",
                "국내도서>소설>과학소설",
                "알에이치코리아(RHK)",
                null,
                null,
                null
        );
        BookSearchResponse response = new BookSearchResponse(1, null, List.of(item));
        when(bookSearchService.search("마션", 1, BookSearchSort.LATEST)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/books")
                        .param("query", "마션")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.nextPage").value(nullValue()))
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
                .andExpect(jsonPath("$.items[0].publisher").value("알에이치코리아(RHK)"))
                .andExpect(jsonPath("$.items[0].reviewCount").value(nullValue()))
                .andExpect(jsonPath("$.items[0].replyCount").value(nullValue()))
                .andExpect(jsonPath("$.items[0].isRegisteredInMyLibrary").value(nullValue()))
                .andDo(document(
                        "book-search",
                        queryParameters(
                                parameterWithName("query").description("검색할 도서명"),
                                parameterWithName("page").description("1부터 시작하는 페이지 번호"),
                                parameterWithName("sort")
                                        .description("정렬 기준. LATEST는 최신순, COMMENT는 댓글순이며 생략하면 LATEST를 사용한다")
                                        .optional()
                        ),
                        responseFields(BOOK_SEARCH_RESPONSE_FIELDS),
                        resource(ResourceSnippetParameters.builder()
                                .summary(BOOK_SEARCH_SUMMARY)
                                .description(BOOK_SEARCH_DESCRIPTION)
                                .tag("도서")
                                .queryParameters(
                                        ResourceDocumentation.parameterWithName("query")
                                                .type(SimpleType.STRING)
                                                .description("검색할 도서명"),
                                        ResourceDocumentation.parameterWithName("page")
                                                .type(SimpleType.INTEGER)
                                                .description("1부터 시작하는 페이지 번호"),
                                        ResourceDocumentation.parameterWithName("sort")
                                                .type(SimpleType.STRING)
                                                .description("정렬 기준. LATEST는 최신순, COMMENT는 댓글순이며 생략하면 LATEST를 사용한다")
                                                .optional()
                                )
                                .responseFields(BOOK_SEARCH_RESPONSE_FIELDS)
                                .build())
                ));

        verify(bookSearchService).search("마션", 1, BookSearchSort.LATEST);
    }

    @Test
    @DisplayName("정렬 기준을 지정하면 해당 기준으로 도서 검색을 요청한다")
    void should_RequestBookSearchWithSort_When_SortIsProvided() throws Exception {
        // given
        when(bookSearchService.search("마션", 1, BookSearchSort.COMMENT))
                .thenReturn(new BookSearchResponse(0, null, List.of()));

        // when & then
        mockMvc.perform(get("/api/v1/books")
                        .param("query", "마션")
                        .param("page", "1")
                        .param("sort", "COMMENT"))
                .andExpect(status().isOk());

        verify(bookSearchService).search("마션", 1, BookSearchSort.COMMENT);
    }

    @Test
    @DisplayName("ISBN13으로 상세 정보를 조회하면 상세 정보를 반환한다")
    void should_ReturnBookDetailResponse_When_BookExists() throws Exception {
        // given
        when(bookService.getDetail("9788925568683")).thenReturn(bookDetailResponse());

        // when & then
        mockMvc.perform(get("/api/v1/books/by-isbn/{isbn13}", "9788925568683"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookId").value(42))
                .andExpect(jsonPath("$.description").value("책 설명"))
                .andExpect(jsonPath("$.reviewCount").value(22))
                .andExpect(jsonPath("$.replyCount").value(24))
                .andDo(document(
                        "book-detail",
                        responseFields(BOOK_DETAIL_RESPONSE_FIELDS),
                        resource(ResourceSnippetParameters.builder()
                                .summary(BOOK_DETAIL_SUMMARY)
                                .description("도서의 메타데이터, 댓글 수, 별점과 로그인 사용자의 서재 기록을 조회한다")
                                .tag("도서")
                                .pathParameters(ResourceDocumentation.parameterWithName("isbn13")
                                        .type(SimpleType.STRING).description("ISBN-13"))
                                .responseFields(BOOK_DETAIL_RESPONSE_FIELDS)
                                .build())
                ));

        verify(bookService).getDetail("9788925568683");
    }

    @Test
    @DisplayName("상세 조회 중 책을 찾지 못하면 404 응답을 반환한다")
    void should_ReturnNotFound_When_BookDoesNotExist() throws Exception {
        // given
        when(bookService.getDetail("9788925568683")).thenThrow(new BookNotFoundException());

        // when & then
        expectProblemDetail(
                mockMvc.perform(get("/api/v1/books/by-isbn/{isbn13}", "9788925568683")),
                HttpStatus.NOT_FOUND,
                "BOOK_NOT_FOUND",
                "책을 찾을 수 없습니다.",
                "/api/v1/books/by-isbn/9788925568683"
        ).andDo(problemDetailDocument("book-detail-not-found", BOOK_DETAIL_SUMMARY,
                "요청한 도서가 존재하지 않는다"));
    }

    @Test
    @DisplayName("검색어가 누락되었다면 400 응답을 반환한다")
    void should_ReturnBadRequest_When_QueryIsMissing() throws Exception {
        // when & then
        expectProblemDetail(
                mockMvc.perform(get("/api/v1/books")
                        .param("page", "1")),
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "요청값이 올바르지 않습니다."
        );

        verifyNoInteractions(bookSearchService);
    }

    @Test
    @DisplayName("페이지가 누락되었다면 400 응답을 반환한다")
    void should_ReturnBadRequest_When_PageIsMissing() throws Exception {
        // when & then
        expectProblemDetail(
                mockMvc.perform(get("/api/v1/books")
                        .param("query", "마션")),
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "요청값이 올바르지 않습니다."
        );

        verifyNoInteractions(bookSearchService);
    }

    @Test
    @DisplayName("페이지가 숫자가 아니라면 400 응답을 반환한다")
    void should_ReturnBadRequest_When_PageIsNotNumeric() throws Exception {
        // when & then
        expectProblemDetail(
                mockMvc.perform(get("/api/v1/books")
                        .param("query", "마션")
                        .param("page", "first")),
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "요청값이 올바르지 않습니다."
        ).andDo(problemDetailDocument(
                "book-search-invalid-request"
        ));

        verifyNoInteractions(bookSearchService);
    }

    @Test
    @DisplayName("정렬 기준이 유효하지 않다면 400 응답을 반환한다")
    void should_ReturnBadRequest_When_SortIsInvalid() throws Exception {
        // when & then
        expectProblemDetail(
                mockMvc.perform(get("/api/v1/books")
                        .param("query", "마션")
                        .param("page", "1")
                        .param("sort", "POPULAR")),
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "요청값이 올바르지 않습니다."
        );

        verifyNoInteractions(bookSearchService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    @DisplayName("검색어가 비어 있다면 400 응답을 반환한다")
    void should_ReturnBadRequest_When_QueryIsBlank(String query) throws Exception {
        // when & then
        expectProblemDetail(
                mockMvc.perform(get("/api/v1/books")
                        .param("query", query)
                        .param("page", "1")),
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "요청값이 올바르지 않습니다."
        );

        verifyNoInteractions(bookSearchService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    @DisplayName("페이지가 양수가 아니라면 400 응답을 반환한다")
    void should_ReturnBadRequest_When_PageIsNotPositive(String page) throws Exception {
        // when & then
        expectProblemDetail(
                mockMvc.perform(get("/api/v1/books")
                        .param("query", "마션")
                        .param("page", page)),
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "요청값이 올바르지 않습니다."
        );

        verifyNoInteractions(bookSearchService);
    }

    @Test
    @DisplayName("예상하지 못한 오류가 발생하면 내부 정보를 숨긴 500 응답을 반환한다")
    void should_ReturnInternalServerError_When_UnexpectedExceptionOccurs() throws Exception {
        when(bookSearchService.search("마션", 1, BookSearchSort.LATEST))
                .thenThrow(new RuntimeException("database password leaked"));

        expectProblemDetail(
                mockMvc.perform(get("/api/v1/books")
                        .param("query", "마션")
                        .param("page", "1")),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "서버 내부 오류가 발생했습니다."
        ).andExpect(content().string(not(containsString("database password leaked"))))
                .andDo(problemDetailDocument(
                        "book-search-internal-server-error"
                ));
    }

    @Test
    @DisplayName("알라딘 API 오류가 발생하면 진단 정보를 숨긴 502 응답을 반환한다")
    void should_ReturnBadGateway_When_AladinClientExceptionOccurs() throws Exception {
        // given
        when(bookSearchService.search("마션", 1, BookSearchSort.LATEST))
                .thenThrow(new AladinClientException(1, "invalid secret key"));

        // when & then
        expectProblemDetail(
                mockMvc.perform(get("/api/v1/books")
                        .param("query", "마션")
                        .param("page", "1")),
                HttpStatus.BAD_GATEWAY,
                "EXTERNAL_API_ERROR",
                "외부 서비스 호출에 실패했습니다."
        ).andExpect(content().string(not(containsString("invalid secret key"))))
                .andDo(problemDetailDocument(
                        "book-search-external-api-error"
                ));
    }

    @Test
    @DisplayName("읽기 상태가 유효하지 않으면 422 응답을 반환한다")
    void should_ReturnUnprocessableEntity_When_ReadingStateIsInvalid() throws Exception {
        // given
        when(bookSearchService.search("마션", 1, BookSearchSort.LATEST))
                .thenThrow(new BusinessException(ErrorCode.INVALID_READING_STATE));

        // when & then
        expectProblemDetail(
                mockMvc.perform(get("/api/v1/books")
                        .param("query", "마션")
                        .param("page", "1")),
                HttpStatus.UNPROCESSABLE_CONTENT,
                "INVALID_READING_STATE",
                "현재 읽기 상태에서는 요청을 처리할 수 없습니다."
        );
    }

    private BookDetailResponse bookDetailResponse() {
        return new BookDetailResponse(
                42L,
                "9788925568683",
                "마션",
                "https://image.aladin.co.kr/martian.jpg",
                "책 설명",
                List.of("앤디 위어"),
                List.of("박아람"),
                "알에이치코리아(RHK)",
                "SF",
                "2026-01-01",
                308,
                22,
                24,
                new java.math.BigDecimal("4.3"),
                21,
                12,
                new BookMyRecordResponse("READING", 120, new java.math.BigDecimal("4.2"))
        );
    }

    private RestDocumentationResultHandler problemDetailDocument(String identifier) {
        return problemDetailDocument(identifier, BOOK_SEARCH_SUMMARY, BOOK_SEARCH_DESCRIPTION);
    }

    private RestDocumentationResultHandler problemDetailDocument(
            String identifier,
            String summary,
            String description
    ) {
        return document(
                identifier,
                responseFields(PROBLEM_DETAIL_FIELDS),
                resource(ResourceSnippetParameters.builder()
                        .summary(summary)
                        .description(description)
                        .tag("도서")
                        .responseSchema(Schema.schema("ProblemDetail"))
                        .responseFields(PROBLEM_DETAIL_FIELDS)
                        .build())
        );
    }

    private ResultActions expectProblemDetail(
            ResultActions result,
            HttpStatus status,
            String code,
            String detail
    ) throws Exception {
        return expectProblemDetail(result, status, code, detail, "/api/v1/books");
    }

    private ResultActions expectProblemDetail(
            ResultActions result,
            HttpStatus status,
            String code,
            String detail,
            String instance
    ) throws Exception {
        return result
                .andExpect(status().is(status.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value(status.getReasonPhrase()))
                .andExpect(jsonPath("$.status").value(status.value()))
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(jsonPath("$.instance").value(instance))
                .andExpect(jsonPath("$.code").value(code));
    }
}
