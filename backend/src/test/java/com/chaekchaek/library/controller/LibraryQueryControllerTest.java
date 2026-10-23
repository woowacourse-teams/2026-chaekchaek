package com.chaekchaek.library.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.library.domain.ReadingStatus;
import com.chaekchaek.library.dto.LibraryBookDetailResponse;
import com.chaekchaek.library.dto.LibraryBookSummaryResponse;
import com.chaekchaek.library.dto.LibraryCollectionResponse;
import com.chaekchaek.library.dto.LibraryDetailResponse;
import com.chaekchaek.library.dto.LibraryShelfResponse;
import com.chaekchaek.library.service.LibraryQueryService;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.epages.restdocs.apispec.SimpleType;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = LibraryQueryController.class, excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class)
@AutoConfigureRestDocs
class LibraryQueryControllerTest {

    private static final String COLLECTION_DESCRIPTION = """
            인증 없이 ACTIVE 회원의 모든 책장과 각 책장 전체 도서 요약을 저장된 표시 순서로 조회한다.
            회원당 책장 수와 응답 도서 수를 제한하지 않으며 이전된 30권 초과 책장도 모두 반환한다.
            같은 도서가 여러 책장에 있으면 책장마다 포함한다. 빈 책장도 포함하고 책장이 없으면 libraries는 빈 배열이다.
            기존 책장 이름은 10자 초과, 공백 등 새 입력 규칙을 벗어나도 그대로 반환한다.
            없는 회원과 비활성 회원은 MEMBER_NOT_FOUND다.
            """;
    private static final String DETAIL_DESCRIPTION = """
            인증 없이 ACTIVE 회원 소유 책장 도서를 저장된 표시 순서로 조회한다.
            page는 생략하면 1이며 페이지 크기는 10이다. 빈 값, 소수, 0 이하, int 범위 초과는 INVALID_REQUEST다.
            totalCount는 책장 전체 권수이며 마지막 페이지의 nextPage는 null이다.
            빈 책장과 범위 밖 페이지도 200과 빈 items, nextPage: null을 반환한다.
            memberId와 독서 기록은 조회자가 아닌 책장 소유자 기준이다. 배치된 도서의 독서 기록은 null이 아니다.
            이전된 30권 초과 책장도 조회하며 기존 책장 이름을 자르거나 보정하지 않는다.
            없는 책장과 비활성 회원의 책장은 LIBRARY_NOT_FOUND다.
            """;
    private static final FieldDescriptor[] COLLECTION_FIELDS = {
            fieldWithPath("memberId").type(JsonFieldType.NUMBER).description("서재 소유 회원 ID"),
            fieldWithPath("libraries").type(JsonFieldType.ARRAY).description("저장된 표시 순서의 전체 책장"),
            fieldWithPath("libraries[].libraryId").type(JsonFieldType.NUMBER).description("책장 ID"),
            fieldWithPath("libraries[].name").type(JsonFieldType.STRING).description("기존 값을 보존한 책장 이름"),
            fieldWithPath("libraries[].isDefault").type(JsonFieldType.BOOLEAN).description("기본 책장 여부"),
            fieldWithPath("libraries[].totalCount").type(JsonFieldType.NUMBER).description("책장 전체 권수, items 길이"),
            fieldWithPath("libraries[].items").type(JsonFieldType.ARRAY).description("표시 순서의 전체 도서 요약"),
            fieldWithPath("libraries[].items[].bookId").type(JsonFieldType.NUMBER).description("도서 ID"),
            fieldWithPath("libraries[].items[].title").type(JsonFieldType.STRING).description("도서 제목"),
            fieldWithPath("libraries[].items[].spineImageUrl").type(JsonFieldType.STRING).description("책등 이미지 URL, 없으면 null").optional(),
            fieldWithPath("libraries[].items[].isbn13").type(JsonFieldType.STRING).description("상세 조회에 사용하는 ISBN-13")
    };
    private static final FieldDescriptor[] DETAIL_FIELDS = {
            fieldWithPath("libraryId").type(JsonFieldType.NUMBER).description("책장 ID"),
            fieldWithPath("memberId").type(JsonFieldType.NUMBER).description("책장 소유 회원 ID"),
            fieldWithPath("name").type(JsonFieldType.STRING).description("기존 값을 보존한 책장 이름"),
            fieldWithPath("isDefault").type(JsonFieldType.BOOLEAN).description("기본 책장 여부"),
            fieldWithPath("totalCount").type(JsonFieldType.NUMBER).description("책장 전체 권수"),
            fieldWithPath("nextPage").type(JsonFieldType.NUMBER).description("다음 페이지, 마지막이면 null").optional(),
            fieldWithPath("items").type(JsonFieldType.ARRAY).description("표시 순서의 도서, 페이지당 최대 10권"),
            fieldWithPath("items[].bookId").type(JsonFieldType.NUMBER).description("도서 ID"),
            fieldWithPath("items[].title").type(JsonFieldType.STRING).description("도서 제목"),
            fieldWithPath("items[].spineImageUrl").type(JsonFieldType.STRING).description("책등 이미지 URL, 없으면 null").optional(),
            fieldWithPath("items[].isbn13").type(JsonFieldType.STRING).description("ISBN-13"),
            fieldWithPath("items[].totalPages").type(JsonFieldType.NUMBER).description("전체 페이지, 모르면 null").optional(),
            fieldWithPath("items[].status").type(JsonFieldType.STRING).description("소유자의 공유 독서 상태"),
            fieldWithPath("items[].currentPage").type(JsonFieldType.NUMBER).description("소유자의 공유 현재 페이지"),
            fieldWithPath("items[].readingUpdatedAt").type(JsonFieldType.STRING).description("소유자의 독서 갱신 시각")
    };
    private static final FieldDescriptor[] PROBLEM_FIELDS = {
            fieldWithPath("type").type(JsonFieldType.STRING).description("문제 유형 URI"),
            fieldWithPath("title").type(JsonFieldType.STRING).description("HTTP 상태 설명"),
            fieldWithPath("status").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
            fieldWithPath("detail").type(JsonFieldType.STRING).description("오류 상세 메시지"),
            fieldWithPath("instance").type(JsonFieldType.STRING).description("요청 경로"),
            fieldWithPath("code").type(JsonFieldType.STRING).description("애플리케이션 오류 코드")
    };

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LibraryQueryService service;

    @Test
    @DisplayName("전체 서재는 기존 이름과 nullable 책등을 포함한 전체 도서 요약만 반환한다")
    void should_ReturnCollection_When_MemberIsActive() throws Exception {
        // given
        LibraryBookSummaryResponse book = new LibraryBookSummaryResponse(
                42L,
                "책 제목",
                null,
                "9788925568683"
        );
        LibraryShelfResponse shelf = new LibraryShelfResponse(
                7L,
                "기존의 긴 책장 이름 !",
                true,
                1L,
                List.of(book)
        );
        when(service.findLibraries(1L)).thenReturn(new LibraryCollectionResponse(
                1L,
                List.of(shelf)
        ));

        // when & then
        mockMvc.perform(get("/api/v1/libraries").param("memberId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.libraries[0].name").value("기존의 긴 책장 이름 !"))
                .andExpect(jsonPath("$.libraries[0].isDefault").value(true))
                .andExpect(jsonPath("$.libraries[0].items[0].isbn13").value("9788925568683"))
                .andExpect(jsonPath("$.libraries[0].items[0].spineImageUrl").value(nullValue()))
                .andExpect(jsonPath("$.libraries[0].items[0].status").doesNotExist())
                .andDo(document("libraries-public-collection", resource(ResourceSnippetParameters.builder()
                        .tag("공개 서재")
                        .summary("회원의 전체 책장과 도서 요약 조회")
                        .description(COLLECTION_DESCRIPTION)
                        .queryParameters(parameterWithName("memberId").type(SimpleType.INTEGER).description("조회할 양의 회원 ID"))
                        .responseSchema(Schema.schema("LibraryCollectionResponse"))
                        .responseFields(COLLECTION_FIELDS)
                        .build())));
    }

    @Test
    @DisplayName("책장 상세는 소유자와 공유 독서 기록을 포함하고 nullable 값을 명시한다")
    void should_ReturnOwnerRecords_When_FindingLibraryDetail() throws Exception {
        // given
        LibraryBookDetailResponse book = new LibraryBookDetailResponse(
                42L,
                "책 제목",
                null,
                "9788925568683",
                null,
                ReadingStatus.READING,
                23,
                Instant.parse("2026-10-23T03:00:00Z")
        );
        when(service.findLibrary(7L, 1)).thenReturn(new LibraryDetailResponse(
                7L,
                1L,
                "기존의 긴 책장 이름 !",
                true,
                1L,
                null,
                List.of(book)
        ));

        // when & then
        mockMvc.perform(get("/api/v1/libraries/{libraryId}", 7L).param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(1L))
                .andExpect(jsonPath("$.nextPage").value(nullValue()))
                .andExpect(jsonPath("$.items[0].totalPages").value(nullValue()))
                .andExpect(jsonPath("$.items[0].currentPage").value(23))
                .andDo(document("libraries-public-detail", resource(ResourceSnippetParameters.builder()
                        .tag("공개 서재")
                        .summary("책장 도서 페이지 조회")
                        .description(DETAIL_DESCRIPTION)
                        .pathParameters(parameterWithName("libraryId").type(SimpleType.INTEGER).description("양의 책장 ID"))
                        .queryParameters(parameterWithName("page").type(SimpleType.INTEGER).description("생략하면 1, 페이지당 10권").optional())
                        .responseSchema(Schema.schema("LibraryDetailResponse"))
                        .responseFields(DETAIL_FIELDS)
                        .build())));
    }

    @Test
    @DisplayName("페이지를 생략하면 첫 페이지로 조회한다")
    void should_DefaultToFirstPage_When_PageIsOmitted() throws Exception {
        // given
        when(service.findLibrary(7L, 1)).thenReturn(new LibraryDetailResponse(
                7L,
                1L,
                "빈 책장",
                false,
                0L,
                null,
                List.of()
        ));

        // when & then
        mockMvc.perform(get("/api/v1/libraries/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
        verify(service).findLibrary(7L, 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "0", "-1", "1.5", "2147483648", "abc", " "})
    @DisplayName("빈 값과 유효하지 않은 페이지는 조회 전에 거절한다")
    void should_RejectInvalidPage_When_PageIsSupplied(String page) throws Exception {
        // given

        // when & then
        mockMvc.perform(get("/api/v1/libraries/7").param("page", page))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "0", "-1", "1.5", "9223372036854775808", "abc"})
    @DisplayName("유효하지 않은 회원 ID는 조회 전에 거절한다")
    void should_RejectInvalidMemberId_When_FindingCollection(String memberId) throws Exception {
        // given

        // when & then
        mockMvc.perform(get("/api/v1/libraries").param("memberId", memberId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("회원 ID가 누락되면 INVALID_REQUEST 문서를 생성한다")
    void should_ReturnBadRequest_When_MemberIdIsMissing() throws Exception {
        // given

        // when & then
        mockMvc.perform(get("/api/v1/libraries"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andDo(document("libraries-public-collection-invalid-request", resource(ResourceSnippetParameters.builder()
                        .tag("공개 서재")
                        .summary("회원의 전체 책장과 도서 요약 조회")
                        .description(COLLECTION_DESCRIPTION)
                        .responseSchema(Schema.schema("ProblemDetail"))
                        .responseFields(PROBLEM_FIELDS)
                        .build())));
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("유효하지 않은 페이지에 INVALID_REQUEST 문서를 생성한다")
    void should_DocumentBadRequest_When_PageIsEmpty() throws Exception {
        // given

        // when & then
        mockMvc.perform(get("/api/v1/libraries/{libraryId}", 7L).param("page", ""))
                .andExpect(status().isBadRequest())
                .andDo(document("libraries-public-detail-invalid-request", resource(ResourceSnippetParameters.builder()
                        .tag("공개 서재")
                        .summary("책장 도서 페이지 조회")
                        .description(DETAIL_DESCRIPTION)
                        .pathParameters(parameterWithName("libraryId").type(SimpleType.INTEGER).description("양의 책장 ID"))
                        .queryParameters(parameterWithName("page").type(SimpleType.INTEGER).description("생략하면 1, 페이지당 10권").optional())
                        .responseSchema(Schema.schema("ProblemDetail"))
                        .responseFields(PROBLEM_FIELDS)
                        .build())));
    }

    @Test
    @DisplayName("없는 회원과 비활성 회원은 MEMBER_NOT_FOUND로 응답한다")
    void should_ReturnMemberNotFound_When_CollectionIsUnavailable() throws Exception {
        // given
        when(service.findLibraries(1L)).thenThrow(new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/libraries").param("memberId", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"))
                .andDo(document("libraries-public-collection-not-found", resource(ResourceSnippetParameters.builder()
                        .tag("공개 서재")
                        .summary("회원의 전체 책장과 도서 요약 조회")
                        .description(COLLECTION_DESCRIPTION)
                        .queryParameters(parameterWithName("memberId").type(SimpleType.INTEGER).description("조회할 양의 회원 ID"))
                        .responseSchema(Schema.schema("ProblemDetail"))
                        .responseFields(PROBLEM_FIELDS)
                        .build())));
    }

    @Test
    @DisplayName("없는 책장과 비활성 회원의 책장은 LIBRARY_NOT_FOUND로 응답한다")
    void should_ReturnLibraryNotFound_When_ShelfIsUnavailable() throws Exception {
        // given
        when(service.findLibrary(7L, 1)).thenThrow(new BusinessException(ErrorCode.LIBRARY_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/libraries/{libraryId}", 7L).param("page", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LIBRARY_NOT_FOUND"))
                .andDo(document("libraries-public-detail-not-found", resource(ResourceSnippetParameters.builder()
                        .tag("공개 서재")
                        .summary("책장 도서 페이지 조회")
                        .description(DETAIL_DESCRIPTION)
                        .pathParameters(parameterWithName("libraryId").type(SimpleType.INTEGER).description("양의 책장 ID"))
                        .queryParameters(parameterWithName("page").type(SimpleType.INTEGER).description("생략하면 1, 페이지당 10권").optional())
                        .responseSchema(Schema.schema("ProblemDetail"))
                        .responseFields(PROBLEM_FIELDS)
                        .build())));
    }
}
