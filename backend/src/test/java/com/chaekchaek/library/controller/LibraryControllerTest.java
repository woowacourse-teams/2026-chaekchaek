package com.chaekchaek.library.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.common.auth.CurrentMemberIdProvider;
import com.chaekchaek.library.domain.LibrarySort;
import com.chaekchaek.library.domain.ReadingStatus;
import com.chaekchaek.library.dto.LibraryItemResponse;
import com.chaekchaek.library.dto.LibraryListResponse;
import com.chaekchaek.library.dto.RatingComparisonBookResponse;
import com.chaekchaek.library.dto.RatingComparisonResponse;
import com.chaekchaek.library.service.LibraryService;
import com.epages.restdocs.apispec.ResourceDocumentation;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.epages.restdocs.apispec.SimpleType;
import com.epages.restdocs.apispec.ParameterDescriptorWithType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(LibraryController.class)
@AutoConfigureRestDocs
class LibraryControllerTest {

    private static final long MEMBER_ID = 1L;
    private static final long BOOK_ID = 10L;
    private static final String ISBN13 = "9788936433598";
    private static final String LIBRARY_TAG = "내 서재";
    private static final FieldDescriptor[] LIBRARY_ITEM_RESPONSE_FIELDS = {
            fieldWithPath("bookId").type(JsonFieldType.NUMBER).description("도서 ID"),
            fieldWithPath("isbn13").type(JsonFieldType.STRING).description("ISBN-13"),
            fieldWithPath("title").type(JsonFieldType.STRING).description("도서 제목"),
            fieldWithPath("coverImageUrl").type(JsonFieldType.STRING).description("표지 이미지 URL"),
            fieldWithPath("authors").type(JsonFieldType.ARRAY).description("저자 이름 목록"),
            fieldWithPath("translators").type(JsonFieldType.ARRAY).description("옮긴이 이름 목록"),
            fieldWithPath("publisher").type(JsonFieldType.STRING).description("출판사"),
            fieldWithPath("category").type(JsonFieldType.STRING).description("도서 카테고리"),
            fieldWithPath("publishedDate").type(JsonFieldType.STRING).description("출판일"),
            fieldWithPath("totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수").optional(),
            fieldWithPath("commentCount").type(JsonFieldType.NUMBER).description("감상과 답글 수"),
            fieldWithPath("status").type(JsonFieldType.STRING).description("읽기 상태"),
            fieldWithPath("currentPage").type(JsonFieldType.NUMBER).description("현재 읽은 페이지"),
            fieldWithPath("rating").type(JsonFieldType.NUMBER).description("내 별점").optional(),
            fieldWithPath("addedAt").type(JsonFieldType.STRING).description("서재 추가 시각"),
            fieldWithPath("readingUpdatedAt").type(JsonFieldType.STRING).description("읽기 정보 갱신 시각")
    };
    private static final FieldDescriptor[] PROBLEM_DETAIL_FIELDS = {
            fieldWithPath("type").type(JsonFieldType.STRING).description("문제 유형 URI"),
            fieldWithPath("title").type(JsonFieldType.STRING).description("HTTP 상태 설명"),
            fieldWithPath("status").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
            fieldWithPath("detail").type(JsonFieldType.STRING).description("오류 상세 메시지"),
            fieldWithPath("instance").type(JsonFieldType.STRING).description("오류가 발생한 요청 경로"),
            fieldWithPath("code").type(JsonFieldType.STRING).description("애플리케이션 오류 코드")
    };

    @MockitoBean
    private LibraryService libraryService;

    @MockitoBean
    private CurrentMemberIdProvider currentMemberIdProvider;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(currentMemberIdProvider.getCurrentMemberId()).thenReturn(MEMBER_ID);
    }

    @Test
    @DisplayName("유효한 요청이라면 내 서재 목록을 반환한다")
    void should_ReturnLibraryList_When_RequestIsValid() throws Exception {
        // given
        LibraryListResponse response = new LibraryListResponse(3, 2, 2, List.of(libraryItemResponse()));
        when(libraryService.getLibrary(MEMBER_ID, 1, ReadingStatus.READING, LibrarySort.RATING))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/library")
                        .param("page", "1")
                        .param("status", "READING")
                        .param("sort", "RATING"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.filteredCount").value(2))
                .andExpect(jsonPath("$.items[0].bookId").value(BOOK_ID))
                .andDo(document(
                        "library-list",
                        queryParameters(libraryListQueryParameters()),
                        responseFields(libraryListResponseFields()),
                        resource(ResourceSnippetParameters.builder()
                                .summary("내 서재 조회")
                                .description("읽기 상태와 정렬 기준으로 내 서재를 조회한다")
                                .tag(LIBRARY_TAG)
                                .queryParameters(libraryListQueryParameters())
                                .responseFields(libraryListResponseFields())
                                .build())
                ));

        verify(libraryService).getLibrary(MEMBER_ID, 1, ReadingStatus.READING, LibrarySort.RATING);
    }

    @Test
    @DisplayName("서재 조회 페이지가 유효하지 않다면 문제 응답을 반환한다")
    void should_ReturnProblemDetail_When_LibraryListPageIsInvalid() throws Exception {
        // when & then
        expectProblemDetail(mockMvc.perform(get("/api/v1/library").param("page", "0")),
                HttpStatus.BAD_REQUEST, "/api/v1/library")
                .andDo(problemDetailDocument("library-list-invalid-request", "내 서재 조회",
                        "내 서재 조회 요청이 유효하지 않다"));
    }

    @Test
    @DisplayName("유효한 요청이라면 도서를 내 서재에 추가한다")
    void should_AddLibraryItem_When_RequestIsValid() throws Exception {
        // given
        LibraryItemResponse response = libraryItemResponse();
        when(libraryService.addByIsbn13(MEMBER_ID, ISBN13, ReadingStatus.READING, 368))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/library")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isbn13":"9788936433598","status":"READING","totalPages":368}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookId").value(BOOK_ID))
                .andDo(document(
                        "library-add",
                        requestFields(addLibraryItemRequestFields()),
                        responseFields(LIBRARY_ITEM_RESPONSE_FIELDS),
                        resource(ResourceSnippetParameters.builder()
                                .summary("내 서재에 도서 추가")
                                .description("ISBN-13으로 도서를 조회해 내 서재에 추가한다")
                                .tag(LIBRARY_TAG)
                                .requestSchema(Schema.schema("AddLibraryItemRequest"))
                                .requestFields(addLibraryItemRequestFields())
                                .responseFields(LIBRARY_ITEM_RESPONSE_FIELDS)
                                .build())
                ));

        verify(libraryService).addByIsbn13(MEMBER_ID, ISBN13, ReadingStatus.READING, 368);
    }

    @Test
    @DisplayName("유효한 요청이라면 내 서재 항목을 수정한다")
    void should_UpdateLibraryItem_When_RequestIsValid() throws Exception {
        // given
        LibraryItemResponse response = libraryItemResponse();
        when(libraryService.update(MEMBER_ID, BOOK_ID, null, 150, 368)).thenReturn(response);

        // when & then
        mockMvc.perform(patch("/api/v1/library/{bookId}", BOOK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPage":150,"totalPages":368}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(100))
                .andDo(document(
                        "library-update",
                        pathParameters(bookIdPathParameter()),
                        requestFields(updateLibraryItemRequestFields()),
                        responseFields(LIBRARY_ITEM_RESPONSE_FIELDS),
                        resource(ResourceSnippetParameters.builder()
                                .summary("내 서재 항목 수정")
                                .description("읽기 상태 또는 현재 읽은 페이지 중 하나를 수정한다")
                                .tag(LIBRARY_TAG)
                                .pathParameters(bookIdResourcePathParameter())
                                .requestSchema(Schema.schema("UpdateLibraryItemRequest"))
                                .requestFields(updateLibraryItemRequestFields())
                                .responseFields(LIBRARY_ITEM_RESPONSE_FIELDS)
                                .build())
                ));

        verify(libraryService).update(MEMBER_ID, BOOK_ID, null, 150, 368);
    }

    @Test
    @DisplayName("유효한 요청이라면 내 서재 항목을 삭제한다")
    void should_DeleteLibraryItem_When_RequestIsValid() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/library/{bookId}", BOOK_ID))
                .andExpect(status().isNoContent())
                .andDo(document(
                        "library-delete",
                        pathParameters(bookIdPathParameter()),
                        resource(ResourceSnippetParameters.builder()
                                .summary("내 서재 항목 삭제")
                                .description("내 서재에서 도서를 삭제한다")
                                .tag(LIBRARY_TAG)
                                .pathParameters(bookIdResourcePathParameter())
                                .build())
                ));

        verify(libraryService).delete(MEMBER_ID, BOOK_ID);
    }

    @Test
    @DisplayName("유효한 요청이라면 내 서재 항목을 일괄 삭제한다")
    void should_BulkDeleteLibraryItems_When_RequestIsValid() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/library/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookIds":[10,20]}
                                """))
                .andExpect(status().isNoContent())
                .andDo(document(
                        "library-bulk-delete",
                        requestFields(bulkDeleteRequestFields()),
                        resource(ResourceSnippetParameters.builder()
                                .summary("내 서재 항목 일괄 삭제")
                                .description("최대 10개의 내 서재 항목을 한 번에 삭제한다")
                                .tag(LIBRARY_TAG)
                                .requestSchema(Schema.schema("BulkDeleteLibraryItemsRequest"))
                                .requestFields(bulkDeleteRequestFields())
                                .build())
                ));

        verify(libraryService).bulkDelete(MEMBER_ID, List.of(10L, 20L));
    }

    @Test
    @DisplayName("유효한 요청이라면 내 서재 항목의 읽기 상태를 일괄 변경한다")
    void should_BulkChangeLibraryStatus_When_RequestIsValid() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/v1/library/bulk-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookIds":[10,20],"status":"WANT_TO_READ"}
                                """))
                .andExpect(status().isNoContent())
                .andDo(document(
                        "library-bulk-status",
                        requestFields(bulkStatusRequestFields()),
                        resource(ResourceSnippetParameters.builder()
                                .summary("내 서재 읽기 상태 일괄 변경")
                                .description("최대 10개의 내 서재 항목의 읽기 상태를 한 번에 변경한다")
                                .tag(LIBRARY_TAG)
                                .requestSchema(Schema.schema("BulkUpdateLibraryStatusRequest"))
                                .requestFields(bulkStatusRequestFields())
                                .build())
                ));

        verify(libraryService).bulkChangeStatus(MEMBER_ID, List.of(10L, 20L),
                ReadingStatus.WANT_TO_READ);
    }

    @Test
    @DisplayName("유효한 요청이라면 도서에 별점을 등록한다")
    void should_RateBook_When_RequestIsValid() throws Exception {
        // given
        LibraryItemResponse response = libraryItemResponse();
        when(libraryService.rate(MEMBER_ID, BOOK_ID, new BigDecimal("4.5"))).thenReturn(response);

        // when & then
        mockMvc.perform(put("/api/v1/library/{bookId}/rating", BOOK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":4.5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4.5))
                .andDo(document(
                        "library-rate",
                        pathParameters(bookIdPathParameter()),
                        requestFields(ratingRequestFields()),
                        responseFields(LIBRARY_ITEM_RESPONSE_FIELDS),
                        resource(ResourceSnippetParameters.builder()
                                .summary("도서 별점 등록")
                                .description("내 서재 도서에 0.1부터 5.0까지의 별점을 등록한다")
                                .tag(LIBRARY_TAG)
                                .pathParameters(bookIdResourcePathParameter())
                                .requestSchema(Schema.schema("RateBookRequest"))
                                .requestFields(ratingRequestFields())
                                .responseFields(LIBRARY_ITEM_RESPONSE_FIELDS)
                                .build())
                ));

        verify(libraryService).rate(MEMBER_ID, BOOK_ID, new BigDecimal("4.5"));
    }

    @Test
    @DisplayName("유효한 요청이라면 도서 별점을 삭제한다")
    void should_RemoveRating_When_RequestIsValid() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/library/{bookId}/rating", BOOK_ID))
                .andExpect(status().isNoContent())
                .andDo(document(
                        "library-remove-rating",
                        pathParameters(bookIdPathParameter()),
                        resource(ResourceSnippetParameters.builder()
                                .summary("도서 별점 삭제")
                                .description("내 서재 도서에 등록한 별점을 삭제한다")
                                .tag(LIBRARY_TAG)
                                .pathParameters(bookIdResourcePathParameter())
                                .build())
                ));

        verify(libraryService).removeRating(MEMBER_ID, BOOK_ID);
    }

    @Test
    @DisplayName("유효한 요청이라면 기준 별점과 가까운 도서를 비교한다")
    void should_ReturnRatingComparison_When_RequestIsValid() throws Exception {
        // given
        RatingComparisonResponse response = new RatingComparisonResponse(
                comparisonBook(9L, "4.0"), comparisonBook(BOOK_ID, "4.5"), comparisonBook(11L, "4.8"));
        when(libraryService.compareRatingsByIsbn13(MEMBER_ID, ISBN13, new BigDecimal("4.5")))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/members/me/ratings/comparison")
                        .param("isbn13", ISBN13)
                        .param("criterion", "4.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current.bookId").value(BOOK_ID))
                .andDo(document(
                        "library-rating-comparison",
                        queryParameters(ratingComparisonQueryParameters()),
                        responseFields(ratingComparisonResponseFields()),
                        resource(ResourceSnippetParameters.builder()
                                .summary("별점 비교 도서 조회")
                                .description("기준 별점보다 낮고 높은 내 별점 도서를 각각 한 권씩 조회한다")
                                .tag(LIBRARY_TAG)
                                .queryParameters(ratingComparisonQueryParameters())
                                .responseFields(ratingComparisonResponseFields())
                                .build())
                ));

        verify(libraryService).compareRatingsByIsbn13(MEMBER_ID, ISBN13, new BigDecimal("4.5"));
    }

    private ResultActions expectProblemDetail(ResultActions result, HttpStatus status, String instance)
            throws Exception {
        return result
                .andExpect(status().is(status.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value(status.getReasonPhrase()))
                .andExpect(jsonPath("$.status").value(status.value()))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.instance").value(instance));
    }

    private org.springframework.restdocs.mockmvc.RestDocumentationResultHandler problemDetailDocument(
            String identifier, String summary, String description
    ) {
        return document(identifier,
                responseFields(PROBLEM_DETAIL_FIELDS),
                resource(ResourceSnippetParameters.builder()
                        .summary(summary)
                        .description(description)
                        .tag(LIBRARY_TAG)
                        .responseSchema(Schema.schema("ProblemDetail"))
                        .responseFields(PROBLEM_DETAIL_FIELDS)
                        .build()));
    }

    private ParameterDescriptor[] libraryListQueryParameters() {
        return new ParameterDescriptor[]{
                parameterWithName("page").description("1부터 시작하는 페이지 번호"),
                parameterWithName("status").description("필터할 읽기 상태. 생략하면 전체 상태를 조회한다").optional(),
                parameterWithName("sort").description("정렬 기준. 생략하면 RECENT를 사용한다").optional()
        };
    }

    private FieldDescriptor[] libraryListResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("totalCount").type(JsonFieldType.NUMBER).description("내 서재 전체 도서 수"),
                fieldWithPath("filteredCount").type(JsonFieldType.NUMBER).description("필터 적용 후 도서 수"),
                fieldWithPath("nextPage").type(JsonFieldType.NUMBER).description("다음 페이지 번호. 마지막 페이지면 null").optional(),
                fieldWithPath("items").type(JsonFieldType.ARRAY).description("내 서재 도서 목록"),
                fieldWithPath("items[].bookId").type(JsonFieldType.NUMBER).description("도서 ID"),
                fieldWithPath("items[].isbn13").type(JsonFieldType.STRING).description("ISBN-13"),
                fieldWithPath("items[].title").type(JsonFieldType.STRING).description("도서 제목"),
                fieldWithPath("items[].coverImageUrl").type(JsonFieldType.STRING).description("표지 이미지 URL"),
                fieldWithPath("items[].authors").type(JsonFieldType.ARRAY).description("저자 이름 목록"),
                fieldWithPath("items[].translators").type(JsonFieldType.ARRAY).description("옮긴이 이름 목록"),
                fieldWithPath("items[].publisher").type(JsonFieldType.STRING).description("출판사"),
                fieldWithPath("items[].category").type(JsonFieldType.STRING).description("도서 카테고리"),
                fieldWithPath("items[].publishedDate").type(JsonFieldType.STRING).description("출판일"),
                fieldWithPath("items[].totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수").optional(),
                fieldWithPath("items[].commentCount").type(JsonFieldType.NUMBER).description("감상과 답글 수"),
                fieldWithPath("items[].status").type(JsonFieldType.STRING).description("읽기 상태"),
                fieldWithPath("items[].currentPage").type(JsonFieldType.NUMBER).description("현재 읽은 페이지"),
                fieldWithPath("items[].rating").type(JsonFieldType.NUMBER).description("내 별점").optional(),
                fieldWithPath("items[].addedAt").type(JsonFieldType.STRING).description("서재 추가 시각"),
                fieldWithPath("items[].readingUpdatedAt").type(JsonFieldType.STRING).description("읽기 정보 갱신 시각")
        };
    }

    private FieldDescriptor[] addLibraryItemRequestFields() {
        return new FieldDescriptor[]{
                fieldWithPath("isbn13").type(JsonFieldType.STRING).description("추가할 도서의 ISBN-13"),
                fieldWithPath("status").type(JsonFieldType.STRING).description("초기 읽기 상태"),
                fieldWithPath("totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수")
        };
    }

    private FieldDescriptor[] updateLibraryItemRequestFields() {
        return new FieldDescriptor[]{
                fieldWithPath("status").type(JsonFieldType.STRING).description("변경할 읽기 상태").optional(),
                fieldWithPath("currentPage").type(JsonFieldType.NUMBER).description("변경할 현재 읽은 페이지").optional(),
                fieldWithPath("totalPages").type(JsonFieldType.NUMBER).description("변경할 전체 페이지 수").optional()
        };
    }

    private FieldDescriptor[] bulkDeleteRequestFields() {
        return new FieldDescriptor[]{
                fieldWithPath("bookIds").type(JsonFieldType.ARRAY).description("삭제할 도서 ID 목록. 최대 10개")
        };
    }

    private FieldDescriptor[] bulkStatusRequestFields() {
        return new FieldDescriptor[]{
                fieldWithPath("bookIds").type(JsonFieldType.ARRAY).description("상태를 변경할 도서 ID 목록. 최대 10개"),
                fieldWithPath("status").type(JsonFieldType.STRING).description("변경할 읽기 상태")
        };
    }

    private FieldDescriptor[] ratingRequestFields() {
        return new FieldDescriptor[]{
                fieldWithPath("rating").type(JsonFieldType.NUMBER).description("0.1부터 5.0까지 0.1 단위의 별점")
        };
    }

    private ParameterDescriptor[] ratingComparisonQueryParameters() {
        return new ParameterDescriptor[]{
                parameterWithName("isbn13").description("비교 기준 도서의 ISBN-13"),
                parameterWithName("criterion").description("0.1부터 5.0까지 0.1 단위의 비교 기준 별점")
        };
    }

    private FieldDescriptor[] ratingComparisonResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("lower").type(JsonFieldType.OBJECT).description("기준보다 낮은 별점 중 가장 가까운 도서").optional(),
                fieldWithPath("lower.bookId").type(JsonFieldType.NUMBER).description("도서 ID").optional(),
                fieldWithPath("lower.isbn13").type(JsonFieldType.STRING).description("ISBN-13").optional(),
                fieldWithPath("lower.title").type(JsonFieldType.STRING).description("도서 제목").optional(),
                fieldWithPath("lower.coverImageUrl").type(JsonFieldType.STRING).description("표지 이미지 URL").optional(),
                fieldWithPath("lower.authors").type(JsonFieldType.ARRAY).description("저자 이름 목록").optional(),
                fieldWithPath("lower.myRating").type(JsonFieldType.NUMBER).description("내 별점").optional(),
                fieldWithPath("current").type(JsonFieldType.OBJECT).description("비교 기준 도서"),
                fieldWithPath("current.bookId").type(JsonFieldType.NUMBER).description("도서 ID"),
                fieldWithPath("current.isbn13").type(JsonFieldType.STRING).description("ISBN-13"),
                fieldWithPath("current.title").type(JsonFieldType.STRING).description("도서 제목"),
                fieldWithPath("current.coverImageUrl").type(JsonFieldType.STRING).description("표지 이미지 URL"),
                fieldWithPath("current.authors").type(JsonFieldType.ARRAY).description("저자 이름 목록"),
                fieldWithPath("current.myRating").type(JsonFieldType.NUMBER).description("내 별점"),
                fieldWithPath("higher").type(JsonFieldType.OBJECT).description("기준보다 높은 별점 중 가장 가까운 도서").optional(),
                fieldWithPath("higher.bookId").type(JsonFieldType.NUMBER).description("도서 ID").optional(),
                fieldWithPath("higher.isbn13").type(JsonFieldType.STRING).description("ISBN-13").optional(),
                fieldWithPath("higher.title").type(JsonFieldType.STRING).description("도서 제목").optional(),
                fieldWithPath("higher.coverImageUrl").type(JsonFieldType.STRING).description("표지 이미지 URL").optional(),
                fieldWithPath("higher.authors").type(JsonFieldType.ARRAY).description("저자 이름 목록").optional(),
                fieldWithPath("higher.myRating").type(JsonFieldType.NUMBER).description("내 별점").optional()
        };
    }

    private ParameterDescriptor bookIdPathParameter() {
        return org.springframework.restdocs.request.RequestDocumentation.parameterWithName("bookId")
                .description("도서 ID");
    }

    private ParameterDescriptorWithType bookIdResourcePathParameter() {
        return ResourceDocumentation.parameterWithName("bookId")
                .type(SimpleType.INTEGER)
                .description("도서 ID");
    }

    private LibraryItemResponse libraryItemResponse() {
        return new LibraryItemResponse(
                BOOK_ID,
                ISBN13,
                "채식주의자",
                "https://image.aladin.co.kr/cover.jpg",
                List.of("한강"),
                List.of(),
                "창비",
                "국내도서>소설",
                LocalDate.of(2007, 10, 30),
                368,
                2,
                ReadingStatus.READING,
                100,
                new BigDecimal("4.5"),
                Instant.parse("2026-08-14T03:00:00Z"),
                Instant.parse("2026-08-14T03:30:00Z")
        );
    }

    private RatingComparisonBookResponse comparisonBook(long bookId, String rating) {
        return new RatingComparisonBookResponse(bookId, ISBN13, "채식주의자",
                "https://image.aladin.co.kr/cover.jpg", List.of("한강"), new BigDecimal(rating));
    }

}
