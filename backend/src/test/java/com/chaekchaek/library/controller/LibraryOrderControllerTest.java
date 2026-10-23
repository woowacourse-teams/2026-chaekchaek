package com.chaekchaek.library.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.common.auth.CurrentMemberIdProvider;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.library.service.LibraryOrderService;
import com.epages.restdocs.apispec.ResourceDocumentation;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.ResourceSnippetParametersBuilder;
import com.epages.restdocs.apispec.Schema;
import com.epages.restdocs.apispec.SimpleType;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(value = LibraryOrderController.class, excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class)
@AutoConfigureRestDocs
class LibraryOrderControllerTest {

    private static final String COMMON_ORDER_DESCRIPTION = """
            빈 목록은 빈 배열을 허용한다.
            ID는 양의 int64 정수이며 null, 문자열, 소수와 중복은 `400`, `INVALID_REQUEST`다.
            현재 전체 ID 집합과 다르면 `409`, `ORDER_CONFLICT`이며 목록을 다시 조회한 뒤 요청한다.
            실패하면 기존 순서를 전부 유지하고, 같은 집합의 동시 요청은 마지막 반영 순서를 적용한다.
            다른 책장, 독서 기록과 별점은 변경하지 않는다.
            """;
    private static final String LIBRARY_ORDER_DESCRIPTION =
            "인증 회원의 모든 책장 ID를 기본 책장까지 포함해 원하는 순서대로 전달한다. "
                    + COMMON_ORDER_DESCRIPTION;
    private static final String BOOK_ORDER_DESCRIPTION =
            "인증 회원이 소유한 특정 책장의 전체 도서 ID를 원하는 순서대로 전달한다. "
                    + "이전된 30권 초과 책장에도 도서 개수 제한을 적용하지 않는다. "
                    + COMMON_ORDER_DESCRIPTION;
    private static final FieldDescriptor[] PROBLEM_FIELDS = {
            fieldWithPath("type").description("문제 유형 URI"),
            fieldWithPath("title").description("HTTP 상태 설명"),
            fieldWithPath("status").description("HTTP 상태 코드"),
            fieldWithPath("detail").description("오류 상세 메시지"),
            fieldWithPath("instance").description("요청 경로").optional(),
            fieldWithPath("code").description("애플리케이션 오류 코드")
    };

    @MockitoBean
    private LibraryOrderService libraryOrderService;
    @MockitoBean
    private CurrentMemberIdProvider currentMemberIdProvider;
    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(currentMemberIdProvider.getCurrentMemberIdForWrite()).thenReturn(1L);
    }

    @Test
    @DisplayName("전체 책장 순서를 변경하면 본문 없는 응답을 반환한다")
    void should_ReturnNoContent_When_ReorderingLibraries() throws Exception {
        // when & then
        request(false, "[2,1]")
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andDo(orderDocument("library-order", false, false));
        verify(libraryOrderService).reorderLibraries(1L, List.of(2L, 1L));
    }

    @Test
    @DisplayName("전체 도서 순서를 변경하면 본문 없는 응답을 반환한다")
    void should_ReturnNoContent_When_ReorderingBooks() throws Exception {
        // when & then
        request(true, "[20,10]")
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andDo(orderDocument("library-book-order", true, false));
        verify(libraryOrderService).reorderBooks(1L, 7L, List.of(20L, 10L));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("빈 목록의 순서 변경에 빈 배열을 전달할 수 있다")
    void should_AcceptEmptyArray_When_ListIsEmpty(boolean books) throws Exception {
        // when & then
        request(books, "[]")
                .andExpect(status().isNoContent())
                .andDo(orderDocument(books ? "library-book-order-empty" : "library-order-empty", books, false));
    }

    @Test
    @DisplayName("이전된 35권 전체 ID를 전달할 수 있다")
    void should_AcceptThirtyFiveIds_When_ReorderingMigratedLibrary() throws Exception {
        // given
        List<Long> ids = LongStream.rangeClosed(1L, 35L)
                .boxed()
                .toList()
                .reversed();

        // when & then
        request(true, ids.toString())
                .andExpect(status().isNoContent())
                .andDo(orderDocument("library-book-order-migrated-35", true, false));
        verify(libraryOrderService).reorderBooks(1L, 7L, ids);
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[null]", "[0]", "[-1]", "[1.0]", "[1.5]", "[\"1\"]", "[true]", "[9223372036854775808]"})
    @DisplayName("양의 BIGINT 정수 배열이 아니면 요청을 거절한다")
    void should_RejectInvalidIds_When_RequestBodyIsMalformed(String ids) throws Exception {
        // when & then
        request(false, ids).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        request(true, ids).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(libraryOrderService);
    }

    @Test
    @DisplayName("필수 배열이 누락되면 요청을 거절한다")
    void should_RejectMissingArray_When_BodyIsEmptyObject() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/v1/libraries/order").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andDo(orderDocument("library-order-invalid", false, true));
        mockMvc.perform(patch("/api/v1/libraries/{libraryId}/books/order", 7L)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andDo(orderDocument("library-book-order-invalid", true, true));
        verifyNoInteractions(libraryOrderService);
    }

    @Test
    @DisplayName("양의 BIGINT 최댓값을 손실 없이 전달한다")
    void should_PreserveLongMaxValue_When_IdFitsBigint() throws Exception {
        // when & then
        request(false, "[9223372036854775807]").andExpect(status().isNoContent());
        verify(libraryOrderService).reorderLibraries(1L, List.of(Long.MAX_VALUE));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("현재 전체 ID 집합이 달라지면 충돌 응답을 반환한다")
    void should_ReturnConflict_When_CurrentIdsDiffer(boolean books) throws Exception {
        // given
        fail(books, ErrorCode.ORDER_CONFLICT);

        // when & then
        request(books, "[20,10]")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_CONFLICT"))
                .andDo(orderDocument(books ? "library-book-order-conflict" : "library-order-conflict", books, true));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("중복 ID는 잘못된 요청 응답을 반환한다")
    void should_ReturnBadRequest_When_DuplicateIdsAreSubmitted(boolean books) throws Exception {
        // given
        if (books) {
            doThrow(new BusinessException(ErrorCode.INVALID_REQUEST))
                    .when(libraryOrderService).reorderBooks(1L, 7L, List.of(10L, 10L));
        } else {
            doThrow(new BusinessException(ErrorCode.INVALID_REQUEST))
                    .when(libraryOrderService).reorderLibraries(1L, List.of(10L, 10L));
        }

        // when & then
        request(books, "[10,10]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("인증이 없으면 인증 오류 응답을 반환한다")
    void should_ReturnUnauthorized_When_MemberIsNotAuthenticated(boolean books) throws Exception {
        // given
        when(currentMemberIdProvider.getCurrentMemberIdForWrite()).thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED));

        // when & then
        request(books, "[20,10]")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andDo(orderDocument(books ? "library-book-order-unauthorized" : "library-order-unauthorized", books, true));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("허용되지 않은 회원이나 타인 책장은 권한 오류 응답을 반환한다")
    void should_ReturnForbidden_When_MutationIsNotAllowed(boolean books) throws Exception {
        // given
        fail(books, ErrorCode.FORBIDDEN);

        // when & then
        request(books, "[20,10]")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andDo(orderDocument(books ? "library-book-order-forbidden" : "library-order-forbidden", books, true));
    }

    @Test
    @DisplayName("없는 책장은 책장 없음 응답을 반환한다")
    void should_ReturnNotFound_When_LibraryDoesNotExist() throws Exception {
        // given
        fail(true, ErrorCode.LIBRARY_NOT_FOUND);

        // when & then
        request(true, "[20,10]")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LIBRARY_NOT_FOUND"))
                .andDo(orderDocument("library-book-order-not-found", true, true));
    }

    @Test
    @DisplayName("책장 경로의 비양수 ID를 거절한다")
    void should_RejectNonPositiveLibraryId_When_ReorderingBooks() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/v1/libraries/{libraryId}/books/order", 0L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"bookIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(libraryOrderService);
    }

    private void fail(boolean books, ErrorCode code) {
        if (books) {
            doThrow(new BusinessException(code)).when(libraryOrderService).reorderBooks(1L, 7L, List.of(20L, 10L));
        } else {
            doThrow(new BusinessException(code)).when(libraryOrderService).reorderLibraries(1L, List.of(20L, 10L));
        }
    }

    private ResultActions request(boolean books, String ids) throws Exception {
        String path = "/api/v1/libraries/order";
        String field = "libraryIds";
        if (books) {
            path = "/api/v1/libraries/{libraryId}/books/order";
            field = "bookIds";
        }
        return mockMvc.perform(patch(path, 7L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"" + field + "\":" + ids + "}"));
    }

    private RestDocumentationResultHandler orderDocument(String id, boolean books, boolean problem) {
        String field = books ? "bookIds" : "libraryIds";
        ResourceSnippetParametersBuilder builder = ResourceSnippetParameters.builder()
                .tag(books ? "책장 도서" : "책장")
                .summary(books ? "책장 내 도서 순서 변경" : "서재 내 책장 순서 변경")
                .description(books ? BOOK_ORDER_DESCRIPTION : LIBRARY_ORDER_DESCRIPTION);
        if (books) {
            builder.pathParameters(ResourceDocumentation.parameterWithName("libraryId").type(SimpleType.INTEGER)
                    .description("소유한 책장 ID, 양의 BIGINT 정수"));
        }
        if (problem) {
            builder.responseSchema(Schema.schema("ProblemDetail")).responseFields(PROBLEM_FIELDS);
        } else {
            builder.requestSchema(Schema.schema(books ? "BookOrderRequest" : "LibraryOrderRequest"))
                    .requestFields(fieldWithPath(field).type(JsonFieldType.ARRAY)
                            .description("원하는 순서로 나열한 현재 전체 ID, 빈 배열 허용")
                            .attributes(key("itemsType").value(JsonFieldType.NUMBER)));
        }
        return document(id, resource(builder.build()));
    }
}
