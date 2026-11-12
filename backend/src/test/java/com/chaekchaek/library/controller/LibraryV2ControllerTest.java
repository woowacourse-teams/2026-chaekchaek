package com.chaekchaek.library.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.common.auth.CurrentMemberIdProvider;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.library.domain.LibrarySort;
import com.chaekchaek.library.dto.LibraryBookDetailResponse;
import com.chaekchaek.library.dto.LibraryBookSummaryResponse;
import com.chaekchaek.library.dto.LibraryCollectionResponse;
import com.chaekchaek.library.dto.LibraryDetailResponse;
import com.chaekchaek.library.dto.LibraryItemResponse;
import com.chaekchaek.library.dto.LibraryListResponse;
import com.chaekchaek.library.dto.LibraryShelfResponse;
import com.chaekchaek.library.dto.PublicLibraryItemResponse;
import com.chaekchaek.library.dto.PublicLibraryListResponse;
import com.chaekchaek.library.dto.PublicMemberResponse;
import com.chaekchaek.library.service.LibraryQueryService;
import com.chaekchaek.library.service.LibraryService;
import com.epages.restdocs.apispec.ResourceSnippetParametersBuilder;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.epages.restdocs.apispec.SimpleType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(value = LibraryV2Controller.class, excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class)
@AutoConfigureRestDocs
class LibraryV2ControllerTest {

    private static final String TAG = "서재";
    private static final String SPINE_NOTICE = "책등 이미지가 없거나 YES24 준비중 이미지면 `spineImageUrl`은 `null`이다.";
    private static final String LIBRARY_LIST_SUMMARY = "내 서재 조회 v2";
    private static final String PUBLIC_LIST_SUMMARY = "사용자 공개 서재 조회 v2";
    private static final String COLLECTION_SUMMARY = "회원의 전체 책장과 도서 요약 조회 v2";
    private static final String DETAIL_SUMMARY = "책장 상세 조회 v2";
    private static final String SPINE_URL = "https://image.yes24.com/goods/2/SIDE/L";
    private static final long MEMBER_ID = 1L;
    private static final int MAX_SIZE = 30;
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
    private LibraryService libraryService;

    @MockitoBean
    private LibraryQueryService libraryQueryService;

    @MockitoBean
    private CurrentMemberIdProvider currentMemberIdProvider;

    @BeforeEach
    void setUp() {
        when(currentMemberIdProvider.getCurrentMemberId()).thenReturn(MEMBER_ID);
    }

    @Test
    @DisplayName("v2 내 서재는 책등이 없는 도서의 spineImageUrl을 null로 반환한다")
    void should_ReturnNullSpine_When_GettingLibrary() throws Exception {
        // given
        when(libraryService.getLibrary(MEMBER_ID, 1, LibrarySort.RECENT))
                .thenReturn(new LibraryListResponse(
                        2,
                        MAX_SIZE,
                        2,
                        null,
                        List.of(libraryItem(1L, null), libraryItem(2L, SPINE_URL))
                ));

        // when & then
        mockMvc.perform(get("/api/v2/library")
                        .param("page", "1")
                        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].spineImageUrl").value(nullValue()))
                .andExpect(jsonPath("$.items[1].spineImageUrl").value(SPINE_URL))
                .andDo(document("library-list-v2", resource(libraryListResource()
                        .responseSchema(Schema.schema("LibraryListV2Response"))
                        .responseFields(libraryListFields())
                        .build())));
    }

    @Test
    @DisplayName("v2 내 서재의 잘못된 페이지는 INVALID_REQUEST로 응답한다")
    void should_ReturnBadRequest_When_LibraryPageIsInvalid() throws Exception {
        // given

        // when
        ResultActions result = mockMvc.perform(get("/api/v2/library").param("page", "0"));

        // then
        result.andExpect(status().isBadRequest())
                .andDo(document("library-list-v2-invalid-request", resource(libraryListResource()
                        .responseSchema(Schema.schema("ProblemDetail"))
                        .responseFields(PROBLEM_FIELDS)
                        .build())));
    }

    @Test
    @DisplayName("v2 내 서재는 인증되지 않으면 UNAUTHORIZED로 응답한다")
    void should_ReturnUnauthorized_When_LibraryRequesterIsAnonymous() throws Exception {
        // given
        when(currentMemberIdProvider.getCurrentMemberId())
                .thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED));

        // when
        ResultActions result = mockMvc.perform(get("/api/v2/library").param("page", "1"));

        // then
        result.andExpect(status().isUnauthorized())
                .andDo(document("library-list-v2-unauthorized", resource(libraryListResource()
                        .responseSchema(Schema.schema("ProblemDetail"))
                        .responseFields(PROBLEM_FIELDS)
                        .build())));
    }

    @Test
    @DisplayName("v2 공개 서재는 책등이 없는 도서의 spineImageUrl을 null로 반환한다")
    void should_ReturnNullSpine_When_GettingPublicLibrary() throws Exception {
        // given
        when(libraryService.getPublicLibrary(MEMBER_ID, 1, LibrarySort.RECENT))
                .thenReturn(new PublicLibraryListResponse(
                        new PublicMemberResponse(
                                MEMBER_ID,
                                "책책이",
                                null
                        ),
                        2,
                        MAX_SIZE,
                        2,
                        null,
                        List.of(
                                PublicLibraryItemResponse.from(libraryItem(1L, null)),
                                PublicLibraryItemResponse.from(libraryItem(2L, SPINE_URL))
                        )
                ));

        // when & then
        mockMvc.perform(get("/api/v2/members/{memberId}/library", MEMBER_ID).param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].spineImageUrl").value(nullValue()))
                .andExpect(jsonPath("$.items[1].spineImageUrl").value(SPINE_URL))
                .andDo(document("public-library-list-v2", resource(publicLibraryResource()
                        .responseSchema(Schema.schema("PublicLibraryListV2Response"))
                        .responseFields(publicLibraryFields())
                        .build())));
    }

    @Test
    @DisplayName("v2 공개 서재의 대상 회원이 없으면 LIBRARY_NOT_FOUND로 응답한다")
    void should_ReturnNotFound_When_PublicLibraryIsUnavailable() throws Exception {
        // given
        when(libraryService.getPublicLibrary(MEMBER_ID, 1, LibrarySort.RECENT))
                .thenThrow(new BusinessException(ErrorCode.LIBRARY_NOT_FOUND));

        // when
        ResultActions result = mockMvc.perform(get("/api/v2/members/{memberId}/library", MEMBER_ID)
                .param("page", "1"));

        // then
        result.andExpect(status().isNotFound())
                .andDo(document("public-library-list-v2-not-found", resource(publicLibraryResource()
                        .responseSchema(Schema.schema("ProblemDetail"))
                        .responseFields(PROBLEM_FIELDS)
                        .build())));
    }

    @Test
    @DisplayName("v2 전체 책장은 책등이 없는 도서의 spineImageUrl을 null로 반환한다")
    void should_ReturnNullSpine_When_FindingLibraries() throws Exception {
        // given
        when(libraryQueryService.findLibraries(MEMBER_ID)).thenReturn(new LibraryCollectionResponse(
                MEMBER_ID,
                List.of(
                        shelf(7L, summary(1L, null)),
                        shelf(8L, summary(2L, SPINE_URL))
                )
        ));

        // when & then
        mockMvc.perform(get("/api/v2/libraries").param("memberId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.libraries[0].items[0].spineImageUrl").value(nullValue()))
                .andExpect(jsonPath("$.libraries[1].items[0].spineImageUrl").value(SPINE_URL))
                .andDo(document("libraries-public-collection-v2", resource(collectionResource()
                        .responseSchema(Schema.schema("LibraryCollectionV2Response"))
                        .responseFields(collectionFields())
                        .build())));
    }

    @Test
    @DisplayName("v2 전체 책장의 대상 회원이 없으면 MEMBER_NOT_FOUND로 응답한다")
    void should_ReturnNotFound_When_CollectionIsUnavailable() throws Exception {
        // given
        when(libraryQueryService.findLibraries(MEMBER_ID))
                .thenThrow(new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // when
        ResultActions result = mockMvc.perform(get("/api/v2/libraries").param("memberId", "1"));

        // then
        result.andExpect(status().isNotFound())
                .andDo(document("libraries-public-collection-v2-not-found", resource(collectionResource()
                        .responseSchema(Schema.schema("ProblemDetail"))
                        .responseFields(PROBLEM_FIELDS)
                        .build())));
    }

    @Test
    @DisplayName("v2 책장 상세는 책등이 없는 도서의 spineImageUrl을 null로 반환한다")
    void should_ReturnNullSpine_When_FindingLibrary() throws Exception {
        // given
        when(libraryQueryService.findLibrary(7L)).thenReturn(new LibraryDetailResponse(
                7L,
                MEMBER_ID,
                "책장",
                true,
                2L,
                MAX_SIZE,
                List.of(detail(1L, null), detail(2L, SPINE_URL))
        ));

        // when & then
        mockMvc.perform(get("/api/v2/libraries/{libraryId}", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].spineImageUrl").value(nullValue()))
                .andExpect(jsonPath("$.items[1].spineImageUrl").value(SPINE_URL))
                .andDo(document("libraries-public-detail-v2", resource(detailResource()
                        .responseSchema(Schema.schema("LibraryDetailV2Response"))
                        .responseFields(detailFields())
                        .build())));
    }

    @Test
    @DisplayName("v2 책장 상세의 책장이 없으면 LIBRARY_NOT_FOUND로 응답한다")
    void should_ReturnNotFound_When_LibraryDetailIsUnavailable() throws Exception {
        // given
        when(libraryQueryService.findLibrary(7L))
                .thenThrow(new BusinessException(ErrorCode.LIBRARY_NOT_FOUND));

        // when
        ResultActions result = mockMvc.perform(get("/api/v2/libraries/{libraryId}", 7L));

        // then
        result.andExpect(status().isNotFound())
                .andDo(document("libraries-public-detail-v2-not-found", resource(detailResource()
                        .responseSchema(Schema.schema("ProblemDetail"))
                        .responseFields(PROBLEM_FIELDS)
                        .build())));
    }

    private ResourceSnippetParametersBuilder libraryListResource() {
        return ResourceSnippetParameters.builder()
                .tag(TAG)
                .summary(LIBRARY_LIST_SUMMARY)
                .description("인증된 사용자의 기본 책장 도서를 조회한다. " + SPINE_NOTICE)
                .queryParameters(
                        parameterWithName("page").type(SimpleType.INTEGER).description("1부터 시작하는 페이지 번호"),
                        parameterWithName("sort").type(SimpleType.STRING).description("정렬 기준").optional()
                );
    }

    private ResourceSnippetParametersBuilder publicLibraryResource() {
        return ResourceSnippetParameters.builder()
                .tag(TAG)
                .summary(PUBLIC_LIST_SUMMARY)
                .description("활성 회원의 기본 책장 도서를 로그인 없이 조회한다. " + SPINE_NOTICE)
                .pathParameters(parameterWithName("memberId").type(SimpleType.INTEGER).description("조회할 회원 ID"))
                .queryParameters(
                        parameterWithName("page").type(SimpleType.INTEGER).description("1부터 시작하는 페이지 번호"),
                        parameterWithName("sort").type(SimpleType.STRING).description("정렬 기준").optional()
                );
    }

    private ResourceSnippetParametersBuilder collectionResource() {
        return ResourceSnippetParameters.builder()
                .tag(TAG)
                .summary(COLLECTION_SUMMARY)
                .description("회원의 모든 책장과 도서 요약을 로그인 없이 조회한다. " + SPINE_NOTICE)
                .queryParameters(parameterWithName("memberId").type(SimpleType.INTEGER).description("조회할 회원 ID"));
    }

    private ResourceSnippetParametersBuilder detailResource() {
        return ResourceSnippetParameters.builder()
                .tag(TAG)
                .summary(DETAIL_SUMMARY)
                .description("책장에 담긴 모든 도서를 로그인 없이 조회한다. " + SPINE_NOTICE)
                .pathParameters(parameterWithName("libraryId").type(SimpleType.INTEGER).description("조회할 책장 ID"));
    }

    private FieldDescriptor[] libraryListFields() {
        return new FieldDescriptor[]{
                fieldWithPath("totalCount").type(JsonFieldType.NUMBER).description("내 서재 전체 도서 수"),
                fieldWithPath("maxSize").type(JsonFieldType.NUMBER).description("책장에 등록 가능한 최대 도서 수"),
                fieldWithPath("filteredCount").type(JsonFieldType.NUMBER).description("필터 적용 후 도서 수"),
                fieldWithPath("nextPage").type(JsonFieldType.NUMBER).description("다음 페이지 번호, 마지막 페이지면 null").optional(),
                fieldWithPath("items").type(JsonFieldType.ARRAY).description("도서 목록"),
                fieldWithPath("items[].bookId").type(JsonFieldType.NUMBER).description("도서 ID"),
                fieldWithPath("items[].isbn13").type(JsonFieldType.STRING).description("ISBN-13"),
                fieldWithPath("items[].title").type(JsonFieldType.STRING).description("도서 제목"),
                fieldWithPath("items[].coverImageUrl").type(JsonFieldType.STRING).description("표지 이미지 URL"),
                fieldWithPath("items[].spineImageUrl").type(JsonFieldType.STRING).description("책등 이미지 URL, 없으면 null").optional(),
                fieldWithPath("items[].backImageUrl").type(JsonFieldType.STRING).description("책뒷면 이미지 URL").optional(),
                fieldWithPath("items[].authors").type(JsonFieldType.ARRAY).description("저자 이름 목록")
                        .attributes(key("itemsType").value(JsonFieldType.STRING)),
                fieldWithPath("items[].translators").type(JsonFieldType.ARRAY).description("옮긴이 이름 목록")
                        .attributes(key("itemsType").value(JsonFieldType.STRING)),
                fieldWithPath("items[].publisher").type(JsonFieldType.STRING).description("출판사"),
                fieldWithPath("items[].category").type(JsonFieldType.STRING).description("도서 카테고리"),
                fieldWithPath("items[].publishedDate").type(JsonFieldType.STRING).description("출판일"),
                fieldWithPath("items[].commentCount").type(JsonFieldType.NUMBER).description("감상과 답글 수"),
                fieldWithPath("items[].rating").type(JsonFieldType.NUMBER).description("내 별점").optional(),
                fieldWithPath("items[].addedAt").type(JsonFieldType.STRING).description("서재 추가 시각")
        };
    }

    private FieldDescriptor[] publicLibraryFields() {
        return new FieldDescriptor[]{
                fieldWithPath("member").type(JsonFieldType.OBJECT).description("공개 서재 회원 정보"),
                fieldWithPath("member.memberId").type(JsonFieldType.NUMBER).description("회원 ID"),
                fieldWithPath("member.displayName").type(JsonFieldType.STRING).description("회원 표시 이름"),
                fieldWithPath("member.profileImageUrl").type(JsonFieldType.STRING).description("회원 프로필 이미지 URL").optional(),
                fieldWithPath("totalCount").type(JsonFieldType.NUMBER).description("공개 서재 전체 도서 수"),
                fieldWithPath("maxSize").type(JsonFieldType.NUMBER).description("책장에 등록 가능한 최대 도서 수"),
                fieldWithPath("filteredCount").type(JsonFieldType.NUMBER).description("필터 적용 후 도서 수"),
                fieldWithPath("nextPage").type(JsonFieldType.NUMBER).description("다음 페이지 번호, 마지막 페이지면 null").optional(),
                fieldWithPath("items").type(JsonFieldType.ARRAY).description("도서 목록"),
                fieldWithPath("items[].bookId").type(JsonFieldType.NUMBER).description("도서 ID"),
                fieldWithPath("items[].isbn13").type(JsonFieldType.STRING).description("ISBN-13"),
                fieldWithPath("items[].title").type(JsonFieldType.STRING).description("도서 제목"),
                fieldWithPath("items[].coverImageUrl").type(JsonFieldType.STRING).description("표지 이미지 URL"),
                fieldWithPath("items[].spineImageUrl").type(JsonFieldType.STRING).description("책등 이미지 URL, 없으면 null").optional(),
                fieldWithPath("items[].backImageUrl").type(JsonFieldType.STRING).description("책뒷면 이미지 URL").optional(),
                fieldWithPath("items[].authors").type(JsonFieldType.ARRAY).description("저자 이름 목록")
                        .attributes(key("itemsType").value(JsonFieldType.STRING)),
                fieldWithPath("items[].translators").type(JsonFieldType.ARRAY).description("옮긴이 이름 목록")
                        .attributes(key("itemsType").value(JsonFieldType.STRING)),
                fieldWithPath("items[].publisher").type(JsonFieldType.STRING).description("출판사"),
                fieldWithPath("items[].category").type(JsonFieldType.STRING).description("도서 카테고리"),
                fieldWithPath("items[].publishedDate").type(JsonFieldType.STRING).description("출판일"),
                fieldWithPath("items[].commentCount").type(JsonFieldType.NUMBER).description("감상과 답글 수"),
                fieldWithPath("items[].rating").type(JsonFieldType.NUMBER).description("별점").optional()
        };
    }

    private FieldDescriptor[] collectionFields() {
        return new FieldDescriptor[]{
                fieldWithPath("memberId").type(JsonFieldType.NUMBER).description("서재 소유 회원 ID"),
                fieldWithPath("libraries").type(JsonFieldType.ARRAY).description("설정한 순서의 책장 목록"),
                fieldWithPath("libraries[].libraryId").type(JsonFieldType.NUMBER).description("책장 ID"),
                fieldWithPath("libraries[].name").type(JsonFieldType.STRING).description("책장 이름"),
                fieldWithPath("libraries[].isDefault").type(JsonFieldType.BOOLEAN).description("기본 책장 여부"),
                fieldWithPath("libraries[].totalCount").type(JsonFieldType.NUMBER).description("책장에 담긴 도서 수"),
                fieldWithPath("libraries[].maxSize").type(JsonFieldType.NUMBER).description("책장에 등록 가능한 최대 도서 수"),
                fieldWithPath("libraries[].items").type(JsonFieldType.ARRAY).description("설정한 순서의 도서 목록"),
                fieldWithPath("libraries[].items[].bookId").type(JsonFieldType.NUMBER).description("도서 ID"),
                fieldWithPath("libraries[].items[].title").type(JsonFieldType.STRING).description("도서 제목"),
                fieldWithPath("libraries[].items[].coverImageUrl").type(JsonFieldType.STRING).description("표지 이미지 URL"),
                fieldWithPath("libraries[].items[].spineImageUrl").type(JsonFieldType.STRING).description("책등 이미지 URL, 없으면 null").optional(),
                fieldWithPath("libraries[].items[].isbn13").type(JsonFieldType.STRING).description("ISBN-13"),
                fieldWithPath("libraries[].items[].authors").type(JsonFieldType.ARRAY).description("저자 이름 목록")
                        .attributes(key("itemsType").value(JsonFieldType.STRING))
        };
    }

    private FieldDescriptor[] detailFields() {
        return new FieldDescriptor[]{
                fieldWithPath("libraryId").type(JsonFieldType.NUMBER).description("책장 ID"),
                fieldWithPath("memberId").type(JsonFieldType.NUMBER).description("책장 소유 회원 ID"),
                fieldWithPath("name").type(JsonFieldType.STRING).description("책장 이름"),
                fieldWithPath("isDefault").type(JsonFieldType.BOOLEAN).description("기본 책장 여부"),
                fieldWithPath("totalCount").type(JsonFieldType.NUMBER).description("책장 전체 권수"),
                fieldWithPath("maxSize").type(JsonFieldType.NUMBER).description("책장에 등록 가능한 최대 도서 수"),
                fieldWithPath("items").type(JsonFieldType.ARRAY).description("설정한 순서의 도서 목록"),
                fieldWithPath("items[].bookId").type(JsonFieldType.NUMBER).description("도서 ID"),
                fieldWithPath("items[].title").type(JsonFieldType.STRING).description("도서 제목"),
                fieldWithPath("items[].coverImageUrl").type(JsonFieldType.STRING).description("표지 이미지 URL"),
                fieldWithPath("items[].spineImageUrl").type(JsonFieldType.STRING).description("책등 이미지 URL, 없으면 null").optional(),
                fieldWithPath("items[].isbn13").type(JsonFieldType.STRING).description("ISBN-13"),
                fieldWithPath("items[].authors").type(JsonFieldType.ARRAY).description("저자 이름 목록")
                        .attributes(key("itemsType").value(JsonFieldType.STRING)),
        };
    }

    private LibraryItemResponse libraryItem(long bookId, String spineImageUrl) {
        return new LibraryItemResponse(
                bookId,
                "9788925568683",
                "책 제목",
                "https://example.com/cover.jpg",
                spineImageUrl,
                "https://example.com/back.jpg",
                List.of("저자"),
                List.of("옮긴이"),
                "출판사",
                "국내도서>소설",
                LocalDate.parse("2007-10-30"),
                2L,
                new BigDecimal("4.5"),
                Instant.parse("2026-08-14T03:00:00Z")
        );
    }

    private LibraryShelfResponse shelf(long libraryId, LibraryBookSummaryResponse book) {
        return new LibraryShelfResponse(
                libraryId,
                "책장",
                libraryId == 7L,
                1L,
                MAX_SIZE,
                List.of(book)
        );
    }

    private LibraryBookSummaryResponse summary(long bookId, String spineImageUrl) {
        return new LibraryBookSummaryResponse(
                bookId,
                "책 제목",
                "https://example.com/cover.jpg",
                spineImageUrl,
                "9788925568683",
                List.of("저자")
        );
    }

    private LibraryBookDetailResponse detail(long bookId, String spineImageUrl) {
        return new LibraryBookDetailResponse(
                bookId,
                "책 제목",
                "https://example.com/cover.jpg",
                spineImageUrl,
                "9788925568683",
                List.of("저자"));
    }
}
