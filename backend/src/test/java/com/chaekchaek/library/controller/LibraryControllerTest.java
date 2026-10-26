package com.chaekchaek.library.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.common.auth.CurrentMemberIdProvider;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.library.domain.LibrarySort;
import com.chaekchaek.library.domain.ReadingStatus;
import com.chaekchaek.library.dto.LibraryItemResponse;
import com.chaekchaek.library.dto.LibraryListResponse;
import com.chaekchaek.library.dto.LibraryResponse;
import com.chaekchaek.library.dto.PublicLibraryItemResponse;
import com.chaekchaek.library.dto.PublicLibraryListResponse;
import com.chaekchaek.library.dto.PublicMemberResponse;
import com.chaekchaek.library.dto.RatingComparisonBookResponse;
import com.chaekchaek.library.dto.RatingComparisonResponse;
import com.chaekchaek.library.service.LibraryBookService;
import com.chaekchaek.library.service.LibraryService;
import com.epages.restdocs.apispec.ParameterDescriptorWithType;
import com.epages.restdocs.apispec.ResourceDocumentation;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.epages.restdocs.apispec.SimpleType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(
        value = LibraryController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class
)
@AutoConfigureRestDocs
class LibraryControllerTest {

    private static final long MEMBER_ID = 1L;
    private static final long BOOK_ID = 10L;
    private static final String ISBN13 = "9788936433598";
    private static final String LIBRARY_TAG = "기존 서재 API";
    private static final String LIBRARY_CREATE_SUMMARY = "내 서재에 책장 추가";
    private static final String LIBRARY_CREATE_DESCRIPTION = """
            인증된 회원의 내 서재 맨 뒤에 일반 책장을 추가한다.

            - `name`: 닉네임과 같은 규칙으로 공백뿐이 아닌 1~10자를 허용한다.
            - 같은 회원의 다른 책장과 이름이 중복되면 대소문자를 구분하지 않고 `409`, `LIBRARY_NAME_ALREADY_EXISTS`를 반환한다.
            - 닉네임을 설정하지 않은 회원은 `422`, `NICKNAME_REQUIRED`를 반환한다.
            - `SUSPENDED`, `WITHDRAWN` 회원은 `403`, `FORBIDDEN`을 반환한다.
            - 회원당 책장 수 제한은 없으며 새 책장은 맨 뒤에 추가한다.
            - 성공 응답에는 `isDefault: false`, `totalCount: 0`, `items: []`가 포함된다.
            - 새 이름 규칙은 생성 요청에만 적용하며 기존 책장 이름은 보존한다.

            기존 `POST /api/v1/library`는 기본 책장에 도서를 추가한다.
            """;
    private static final String LIBRARY_BOOK_ADD_SUMMARY = "책장에 도서 추가";
    private static final String LIBRARY_BOOK_ADD_DESCRIPTION = """
            인증된 회원이 소유한 책장에 ISBN-13 도서를 배치한다. 책장마다 최대 30권까지 등록할 수 있고,
            같은 책장에 같은 도서를 중복 배치할 수 없다. 이미 독서 기록이 있으면 요청 상태를 적용하지 않고
            기존 기록과 별점을 유지한다.
            """;
    private static final String LIBRARY_BOOK_DELETE_SUMMARY = "책장에서 도서 배치 삭제";
    private static final String LIBRARY_BOOK_DELETE_DESCRIPTION = """
            인증된 회원이 소유한 책장에서 도서 배치를 삭제한다. 배치가 이미 없으면 아무 변경 없이 `204`를 반환한다.
            삭제 후 회원의 다른 책장에 배치가 남지 않은 경우 독서 상태, 현재 페이지, 독서 갱신 시각만 초기화하고
            기존 별점은 유지한다.
            """;
    private static final String LIBRARY_DELETE_LIBRARY_SUMMARY = "책장 삭제";
    private static final String LIBRARY_DELETE_LIBRARY_DESCRIPTION = """
            인증된 회원이 소유한 일반 책장을 삭제한다. 기본 책장은 삭제할 수 없다.
            책장의 배치를 모두 삭제한 뒤 다른 책장에 배치가 남지 않은 도서의 독서 상태만 초기화하고
            별점은 유지한다.
            """;
    private static final String LIBRARY_RENAME_SUMMARY = "책장 이름 변경";
    private static final String LIBRARY_RENAME_DESCRIPTION = """
            인증된 회원이 소유한 책장의 이름을 변경한다. 기본 책장도 변경할 수 있으며 기본 여부, 순서와 도서 배치는 유지한다.

            - `name`: 닉네임과 같은 규칙으로 공백뿐이 아닌 1~10자를 허용한다.
            - 같은 회원의 다른 책장과 이름이 중복되면 대소문자를 구분하지 않고 `409`, `LIBRARY_NAME_ALREADY_EXISTS`를 반환한다.
            - 다른 책장과 중복되지 않으면 현재 이름의 대소문자만 바꾸는 요청도 허용한다.
            - 현재와 같은 이름이면 수정 시각과 변경 이벤트를 갱신하지 않고 성공한다.
            - 기본 책장 이름 변경은 회원 닉네임을 변경하지 않는다.
            - 성공 응답은 본문 없는 `204`다.
            """;
    private static final String LIBRARY_LIST_SUMMARY = "내 서재 조회";
    private static final String LIBRARY_LIST_DESCRIPTION = "인증된 사용자가 기본 책장에 배치된 도서를 기존 읽기 상태와 정렬 기준으로 조회한다. 기본 책장이 없으면 빈 목록을 반환한다";
    private static final String PUBLIC_LIBRARY_LIST_SUMMARY = "사용자 공개 서재 조회";
    private static final String PUBLIC_LIBRARY_LIST_DESCRIPTION = """
            비로그인 사용자를 포함한 모든 사용자가 활성 회원의 기본 책장에 배치된 도서를 조회한다. 기본 책장이 없으면 빈 목록을 반환한다.
            `page`는 1부터 시작하며 한 페이지에 최대 10개를 반환한다. 마지막 페이지의 `nextPage`는 `null`이다.
            필터 결과나 서재가 비어 있으면 200 응답과 빈 `items`를 반환한다.
            존재하지 않거나 탈퇴·정지된 회원의 서재는 `LIBRARY_NOT_FOUND`를 반환한다.
            """;
    private static final String LIBRARY_ADD_SUMMARY = "내 서재에 도서 추가";
    private static final String LIBRARY_ADD_DESCRIPTION = "인증된 사용자가 ISBN-13 도서를 기본 책장 맨 뒤에 배치한다. 같은 기본 책장 중복은 `LIBRARY_ITEM_ALREADY_EXISTS`, 30권 이상이면 `LIBRARY_ITEM_LIMIT_EXCEEDED`, 기본 책장이 없으면 `NICKNAME_REQUIRED`다. 다른 책장에 이미 있으면 공유 독서 기록을 유지하고 기본 책장에 추가한다";
    private static final String LIBRARY_UPDATE_SUMMARY = "내 서재 항목 수정";
    private static final String LIBRARY_UPDATE_DESCRIPTION = "인증된 사용자가 공유 독서 상태 또는 현재 읽은 페이지 중 하나를 수정한다. 초기화된 독서 기록은 `LIBRARY_ITEM_NOT_FOUND`이며 배치와 표시 순서는 변경하지 않는다";
    private static final String LIBRARY_DELETE_SUMMARY = "내 서재 항목 삭제";
    private static final String LIBRARY_DELETE_DESCRIPTION = "인증된 사용자가 도서 존재를 확인한 뒤 기본 책장의 배치만 삭제한다. 배치가 없으면 `204`이며 마지막 배치만 독서 기록을 초기화하고 공유 행과 별점을 보존한다";
    private static final String LIBRARY_BULK_DELETE_SUMMARY = "내 서재 항목 일괄 삭제";
    private static final String LIBRARY_BULK_DELETE_DESCRIPTION = "인증된 사용자가 기본 책장의 배치를 최대 10개 한 번에 삭제한다. 대상이 하나라도 기본 책장에 없으면 `LIBRARY_ITEM_NOT_FOUND`로 전체 실패한다. 마지막 배치만 독서 기록을 초기화하고 별점을 보존한다";
    private static final String LIBRARY_BULK_STATUS_SUMMARY = "내 서재 읽기 상태 일괄 변경";
    private static final String LIBRARY_BULK_STATUS_DESCRIPTION = "인증된 사용자가 최대 10개의 공유 독서 상태를 한 번에 변경한다. 초기화된 기록이 하나라도 있으면 `LIBRARY_ITEM_NOT_FOUND`로 전체 실패하며 배치와 표시 순서는 유지한다";
    private static final String LIBRARY_RATE_SUMMARY = "도서 별점 등록";
    private static final String LIBRARY_RATE_DESCRIPTION = """
            인증된 회원이 도서에 0.1부터 5.0까지 별점을 등록한다. 책장 배치와 독서 기록을 새로 만들지 않으며
            기존 독서 기록은 유지하고 초기화된 기록도 되살리지 않는다. 책장 한도와 닉네임 설정 여부에 영향을 받지 않는다.
            별점만 있는 응답의 status, currentPage, readingUpdatedAt은 null이다. 기존 URI와 응답 필드 구조를 유지한다.
            """;
    private static final String ISBN_RATE_SUMMARY = "ISBN으로 도서 별점 등록, 수정";
    private static final String ISBN_RATE_DESCRIPTION = """
            인증된 활성 회원이 ISBN으로 도서에 별점을 등록하거나 기존 별점을 변경한다.

            - `isbn13`: 검색, 상세 응답의 ISBN 문자열을 그대로 전달한다. `bookId` 확보 요청은 필요하지 않다.
            - `rating`: 기존 ID 경로와 같은 `0.1`부터 `5.0`까지 소수 첫째 자리의 별점이다.
            - DB에 있는 도서는 그대로 사용하고, 미등록 도서만 외부 상세 조회 후 저장한다.
            - 책장, 닉네임과 감상 등록을 요구하지 않으며 책장 배치와 독서 기록을 만들지 않는다.
            - 기존 독서 상태, 현재 페이지와 독서 갱신 시각은 유지하고 초기화된 기록도 되살리지 않는다.
            - 등록, 수정과 같은 값 재요청 모두 `200`이며 기존 `PUT /api/v1/library/{bookId}/rating`과
              응답 필드, 타입, nullable 조건과 의미가 같다. 별점만 있으면 `status`, `currentPage`, `readingUpdatedAt`은 `null`이다.
            - 응답의 `bookId`로 기존 `DELETE /api/v1/library/{bookId}/rating`을 호출할 수 있다.
            - 미등록 도서의 외부 조회 결과가 없으면 `404`, `BOOK_NOT_FOUND`, 외부 조회 실패는 `502`, `EXTERNAL_API_ERROR`다.
            - 도서 확보 후 별점 저장에 실패하면 도서만 남을 수 있다. 같은 요청을 다시 보내면 기존 도서를 재사용한다.
            """;
    private static final String LIBRARY_REMOVE_RATING_SUMMARY = "도서 별점 삭제";
    private static final String LIBRARY_REMOVE_RATING_DESCRIPTION = "인증된 사용자가 내 별점을 삭제한다";
    private static final String RATING_COMPARISON_SUMMARY = "별점 비교 도서 조회";
    private static final String RATING_COMPARISON_DESCRIPTION = "인증된 사용자가 지금 별점을 주려는 도서를 제외하고 기준 별점과 같거나 낮거나 높은 내 별점 도서를 각각 한 권씩 조회한다";
    private static final FieldDescriptor[] LIBRARY_ITEM_RESPONSE_FIELDS = {
            fieldWithPath("bookId").type(JsonFieldType.NUMBER).description("도서 ID"),
            fieldWithPath("isbn13").type(JsonFieldType.STRING).description("ISBN-13"),
            fieldWithPath("title").type(JsonFieldType.STRING).description("도서 제목"),
            fieldWithPath("coverImageUrl").type(JsonFieldType.STRING).description("표지 이미지 URL"),
            fieldWithPath("spineImageUrl").type(JsonFieldType.STRING).description("책등 이미지 URL")
                    .optional(),
            fieldWithPath("backImageUrl").type(JsonFieldType.STRING).description("책뒷면 이미지 URL")
                    .optional(),
            fieldWithPath("authors").type(JsonFieldType.ARRAY).description("저자 이름 목록")
                    .attributes(key("itemsType").value(JsonFieldType.STRING)),
            fieldWithPath("translators").type(JsonFieldType.ARRAY).description("옮긴이 이름 목록")
                    .attributes(key("itemsType").value(JsonFieldType.STRING)),
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
    private static final FieldDescriptor[] LIBRARY_CREATE_REQUEST_FIELDS = {
            fieldWithPath("name").type(JsonFieldType.STRING)
                    .description("닉네임과 같은 규칙으로 공백뿐이 아닌 1~10자를 허용한다")
    };
    private static final FieldDescriptor[] LIBRARY_RENAME_REQUEST_FIELDS = {
            fieldWithPath("name").type(JsonFieldType.STRING)
                    .description("닉네임과 같은 규칙으로 공백뿐이 아닌 1~10자를 허용한다")
    };
    private static final FieldDescriptor[] LIBRARY_RESPONSE_FIELDS = {
            fieldWithPath("libraryId").type(JsonFieldType.NUMBER).description("생성된 책장 ID"),
            fieldWithPath("name").type(JsonFieldType.STRING).description("저장된 책장 이름"),
            fieldWithPath("isDefault").type(JsonFieldType.BOOLEAN).description("기본 책장 여부. 일반 책장은 false"),
            fieldWithPath("totalCount").type(JsonFieldType.NUMBER).description("책장의 전체 도서 수"),
            fieldWithPath("items").type(JsonFieldType.ARRAY).description("표시 순서대로 반환하는 전체 도서 목록"),
            fieldWithPath("items[].bookId").type(JsonFieldType.NUMBER).description("도서 ID").optional(),
            fieldWithPath("items[].title").type(JsonFieldType.STRING).description("도서 제목").optional(),
            fieldWithPath("items[].spineImageUrl").type(JsonFieldType.STRING)
                    .description("책등 이미지 URL. 이미지가 없으면 null").optional(),
            fieldWithPath("items[].isbn13").type(JsonFieldType.STRING)
                    .description("도서 상세 조회에 사용하는 ISBN-13").optional()
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
    private LibraryBookService libraryBookService;

    @MockitoBean
    private CurrentMemberIdProvider currentMemberIdProvider;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(currentMemberIdProvider.getCurrentMemberId()).thenReturn(MEMBER_ID);
        when(currentMemberIdProvider.getCurrentMemberIdForWrite()).thenReturn(MEMBER_ID);
    }

    @Test
    @DisplayName("이름을 지정하면 인증된 회원의 서재를 생성하고 서재 URI를 반환한다")
    void should_CreateLibrary_When_NameIsValid() throws Exception {
        // given
        when(libraryService.create(MEMBER_ID, "올해읽을책")).thenReturn(libraryResponse("올해읽을책"));

        // when & then
        mockMvc.perform(post("/api/v1/libraries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"올해읽을책"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost:8080/api/v1/libraries/7"))
                .andExpect(jsonPath("$.libraryId").value(7L))
                .andExpect(jsonPath("$.name").value("올해읽을책"))
                .andExpect(jsonPath("$.isDefault").value(false))
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.items").isEmpty())
                .andDo(document(
                        "library-create",
                        requestFields(LIBRARY_CREATE_REQUEST_FIELDS),
                        responseFields(LIBRARY_RESPONSE_FIELDS),
                        responseHeaders(headerWithName(HttpHeaders.LOCATION)
                                .description("생성된 책장 URI")),
                        resource(ResourceSnippetParameters.builder()
                                .summary(LIBRARY_CREATE_SUMMARY)
                                .description(LIBRARY_CREATE_DESCRIPTION)
                                .tag("책장")
                                .requestSchema(Schema.schema("CreateLibraryRequest"))
                                .requestFields(LIBRARY_CREATE_REQUEST_FIELDS)
                                .responseSchema(Schema.schema("LibraryShelf"))
                                .responseFields(LIBRARY_RESPONSE_FIELDS)
                                .responseHeaders(ResourceDocumentation.headerWithName(HttpHeaders.LOCATION)
                                        .type(SimpleType.STRING)
                                        .description("생성된 책장 URI"))
                                .build())
                ));
        verify(libraryService).create(MEMBER_ID, "올해읽을책");
    }

    @Test
    @DisplayName("유효한 이름을 지정하면 소유한 책장 이름을 변경하고 204를 반환한다")
    void should_RenameLibrary_When_RequestIsValid() throws Exception {
        mockMvc.perform(patch("/api/v1/libraries/{libraryId}", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"읽을 책"}
                                """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andDo(document(
                        "library-rename",
                        pathParameters(libraryRenamePathParameter()),
                        requestFields(LIBRARY_RENAME_REQUEST_FIELDS),
                        resource(ResourceSnippetParameters.builder()
                                .summary(LIBRARY_RENAME_SUMMARY)
                                .description(LIBRARY_RENAME_DESCRIPTION)
                                .tag("책장")
                                .pathParameters(libraryRenameResourcePathParameters())
                                .requestSchema(Schema.schema("RenameLibraryRequest"))
                                .requestFields(LIBRARY_RENAME_REQUEST_FIELDS)
                                .build())
                ));

        verify(libraryService).rename(MEMBER_ID, 7L, "읽을 책");
    }

    @Test
    @DisplayName("10자를 초과하는 이름으로 책장 변경을 요청하면 잘못된 요청 오류를 반환한다")
    void should_ReturnBadRequest_When_RenameNameIsInvalid() throws Exception {
        expectProblemDetail(
                mockMvc.perform(patch("/api/v1/libraries/{libraryId}", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + "가".repeat(11) + "\"}")),
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_REQUEST,
                "/api/v1/libraries/7"
        );
        verifyNoInteractions(libraryService);
    }

    @Test
    @DisplayName("유효한 요청이라면 소유한 책장에 도서를 배치하고 배치 URI를 반환한다")
    void should_AddBookToLibrary_When_RequestIsValid() throws Exception {
        // given
        LibraryItemResponse response = libraryItemResponse();
        when(libraryBookService.addByIsbn13(MEMBER_ID, 7L, new Isbn13(ISBN13), ReadingStatus.READING, 368))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/libraries/{libraryId}", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isbn13":"9788936433598","status":"READING","totalPages":368}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        "http://localhost:8080/api/v1/libraries/7/books/" + BOOK_ID
                ))
                .andExpect(jsonPath("$.bookId").value(BOOK_ID))
                .andDo(document(
                        "library-book-add",
                        pathParameters(libraryIdPathParameter()),
                        requestFields(addLibraryItemRequestFields()),
                        responseFields(LIBRARY_ITEM_RESPONSE_FIELDS),
                        responseHeaders(headerWithName(HttpHeaders.LOCATION).description("책장에 담긴 도서의 URI")),
                        resource(ResourceSnippetParameters.builder()
                                .summary(LIBRARY_BOOK_ADD_SUMMARY)
                                .description(LIBRARY_BOOK_ADD_DESCRIPTION)
                                .tag("책장 도서")
                                .pathParameters(libraryIdResourcePathParameters())
                                .requestSchema(Schema.schema("AddLibraryItemRequest"))
                                .requestFields(addLibraryItemRequestFields())
                                .responseFields(LIBRARY_ITEM_RESPONSE_FIELDS)
                                .responseHeaders(ResourceDocumentation.headerWithName(HttpHeaders.LOCATION)
                                        .type(SimpleType.STRING).description("책장에 담긴 도서의 URI"))
                                .build())
                ));

        verify(libraryBookService).addByIsbn13(MEMBER_ID, 7L, new Isbn13(ISBN13), ReadingStatus.READING, 368);
    }

    @Test
    @DisplayName("양수가 아닌 책장 ID로 도서 배치를 요청하면 잘못된 요청 오류를 반환한다")
    void should_ReturnBadRequest_When_LibraryIdIsNotPositive() throws Exception {
        // when & then
        expectProblemDetail(
                postLibraryBook(0L),
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_REQUEST,
                "/api/v1/libraries/0"
        );
        verifyNoInteractions(libraryBookService);
    }

    @Test
    @DisplayName("배치 식별자를 지정하면 소유한 책장에서 도서 배치를 멱등 삭제한다")
    void should_DeleteBookPlacement_When_LibraryAndBookIdsAreValid() throws Exception {
        mockMvc.perform(delete("/api/v1/libraries/{libraryId}/books/{bookId}", 7L, BOOK_ID))
                .andExpect(status().isNoContent())
                .andDo(document(
                        "library-book-delete",
                        pathParameters(libraryIdPathParameter(), bookIdPathParameter()),
                        resource(ResourceSnippetParameters.builder()
                                .summary(LIBRARY_BOOK_DELETE_SUMMARY)
                                .description(LIBRARY_BOOK_DELETE_DESCRIPTION)
                                .tag("책장 도서")
                                .pathParameters(libraryBookDeleteResourcePathParameters())
                                .build())
                ));

        verify(libraryBookService).delete(MEMBER_ID, 7L, BOOK_ID);
    }

    @Test
    @DisplayName("책장 ID를 지정하면 책장을 삭제하고 204를 반환한다")
    void should_DeleteLibrary_When_LibraryIdIsValid() throws Exception {
        mockMvc.perform(delete("/api/v1/libraries/{libraryId}", 7L))
                .andExpect(status().isNoContent())
                .andDo(document(
                        "library-delete-library",
                        pathParameters(libraryDeletePathParameter()),
                        resource(ResourceSnippetParameters.builder()
                                .summary(LIBRARY_DELETE_LIBRARY_SUMMARY)
                                .description(LIBRARY_DELETE_LIBRARY_DESCRIPTION)
                                .tag("책장")
                                .pathParameters(libraryDeleteResourcePathParameters())
                                .build())
                ));

        verify(libraryBookService).deleteLibrary(MEMBER_ID, 7L);
    }

    @Test
    @DisplayName("허용되는 문자 10자로 책장을 생성할 수 있다")
    void should_CreateLibrary_When_NameHasTenAllowedCharacters() throws Exception {
        // given
        String name = "가".repeat(10);
        when(libraryService.create(MEMBER_ID, name)).thenReturn(libraryResponse(name));

        // when & then
        mockMvc.perform(post("/api/v1/libraries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name));
        verify(libraryService).create(MEMBER_ID, name);
    }

    @Test
    @DisplayName("서재 이름이 없으면 잘못된 요청 오류를 반환한다")
    void should_ReturnBadRequest_When_LibraryNameIsMissing() throws Exception {
        // when & then
        documentProblemDetail(
                mockMvc.perform(post("/api/v1/libraries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")),
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_REQUEST,
                "/api/v1/libraries",
                "library-create-invalid-request",
                LIBRARY_CREATE_SUMMARY,
                LIBRARY_CREATE_DESCRIPTION,
                noPathParameters(),
                noQueryParameters()
        );
        verifyNoInteractions(libraryService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "\"\"", "\"   \"", "\"\u2003\""})
    @DisplayName("서재 이름이 null이거나 공백뿐이면 서비스에 전달하지 않는다")
    void should_ReturnBadRequest_When_LibraryNameIsNullOrBlank(String name) throws Exception {
        // when & then
        expectProblemDetail(
                mockMvc.perform(post("/api/v1/libraries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":" + name + "}")),
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_REQUEST,
                "/api/v1/libraries"
        );
        verifyNoInteractions(libraryService);
    }

    @Test
    @DisplayName("공백과 문장부호가 있는 이름으로 책장을 생성할 수 있다")
    void should_CreateLibrary_When_NameContainsSpacesAndPunctuation() throws Exception {
        // given
        String name = "가을 책!";
        when(libraryService.create(MEMBER_ID, name)).thenReturn(libraryResponse(name));

        // when & then
        mockMvc.perform(post("/api/v1/libraries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name));
        verify(libraryService).create(MEMBER_ID, name);
    }

    @Test
    @DisplayName("책장 이름 앞뒤 공백을 제거하지 않고 그대로 수용한다")
    void should_CreateLibrary_When_LibraryNameHasSurroundingSpaces() throws Exception {
        // given
        String name = " 올해읽을책 ";
        when(libraryService.create(MEMBER_ID, name)).thenReturn(libraryResponse(name));

        // when & then
        mockMvc.perform(post("/api/v1/libraries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name));
        verify(libraryService).create(MEMBER_ID, name);
    }

    @Test
    @DisplayName("책장 이름이 10자를 초과하면 잘못된 요청 오류를 반환한다")
    void should_ReturnBadRequest_When_LibraryNameIsLongerThanTenCharacters() throws Exception {
        // when & then
        expectProblemDetail(
                mockMvc.perform(post("/api/v1/libraries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + "가".repeat(11) + "\"}")),
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_REQUEST,
                "/api/v1/libraries"
        );
        verifyNoInteractions(libraryService);
    }

    @Test
    @DisplayName("이미 같은 이름의 서재가 있으면 서재명 중복 오류를 반환한다")
    void should_ReturnConflict_When_LibraryNameAlreadyExists() throws Exception {
        // given
        when(libraryService.create(MEMBER_ID, "올해읽을책"))
                .thenThrow(new BusinessException(ErrorCode.LIBRARY_NAME_ALREADY_EXISTS));

        // when & then
        documentProblemDetail(
                mockMvc.perform(post("/api/v1/libraries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"올해읽을책"}
                                """)),
                HttpStatus.CONFLICT,
                ErrorCode.LIBRARY_NAME_ALREADY_EXISTS,
                "/api/v1/libraries",
                "library-create-name-already-exists",
                LIBRARY_CREATE_SUMMARY,
                LIBRARY_CREATE_DESCRIPTION,
                noPathParameters(),
                noQueryParameters()
        );
    }

    @Test
    @DisplayName("닉네임이 없으면 책장을 생성하지 않고 닉네임 설정 오류를 반환한다")
    void should_ReturnUnprocessableEntity_When_MemberHasNoNickname() throws Exception {
        // given
        when(libraryService.create(MEMBER_ID, "올해읽을책"))
                .thenThrow(new BusinessException(ErrorCode.NICKNAME_REQUIRED));

        // when & then
        documentProblemDetail(
                mockMvc.perform(post("/api/v1/libraries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"올해읽을책"}
                                """)),
                HttpStatus.UNPROCESSABLE_CONTENT,
                ErrorCode.NICKNAME_REQUIRED,
                "/api/v1/libraries",
                "library-create-nickname-required",
                LIBRARY_CREATE_SUMMARY,
                LIBRARY_CREATE_DESCRIPTION,
                noPathParameters(),
                noQueryParameters()
        );
    }

    @Test
    @DisplayName("비활성 회원은 책장을 생성할 수 없다")
    void should_ReturnForbidden_When_MemberIsInactive() throws Exception {
        // given
        when(libraryService.create(MEMBER_ID, "올해읽을책"))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

        // when & then
        documentProblemDetail(
                mockMvc.perform(post("/api/v1/libraries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"올해읽을책"}
                                """)),
                HttpStatus.FORBIDDEN,
                ErrorCode.FORBIDDEN,
                "/api/v1/libraries",
                "library-create-forbidden",
                LIBRARY_CREATE_SUMMARY,
                LIBRARY_CREATE_DESCRIPTION,
                noPathParameters(),
                noQueryParameters()
        );
    }

    @Test
    @DisplayName("예상하지 못한 오류가 발생하면 내부 서버 오류를 반환한다")
    void should_ReturnInternalServerError_When_CreateFailsUnexpectedly() throws Exception {
        // given
        when(libraryService.create(MEMBER_ID, "올해읽을책"))
                .thenThrow(new IllegalStateException("unexpected failure"));

        // when & then
        documentProblemDetail(
                mockMvc.perform(post("/api/v1/libraries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"올해읽을책"}
                                """)),
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_SERVER_ERROR,
                "/api/v1/libraries",
                "library-create-internal-server-error",
                LIBRARY_CREATE_SUMMARY,
                LIBRARY_CREATE_DESCRIPTION,
                noPathParameters(),
                noQueryParameters()
        );
    }

    @Test
    @DisplayName("유효한 요청이라면 내 서재 목록을 반환한다")
    void should_ReturnLibraryList_When_RequestIsValid() throws Exception {
        // given
        LibraryListResponse response = new LibraryListResponse(
                3,
                2,
                2,
                List.of(libraryItemResponse())
        );
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
                .andExpect(jsonPath("$.items[0].spineImageUrl")
                        .value("https://image.yes24.com/goods/118578901/side"))
                .andExpect(jsonPath("$.items[0].backImageUrl")
                        .value("https://image.yes24.com/goods/118578901/back"))
                .andDo(document(
                        "library-list",
                        queryParameters(libraryListQueryParameters()),
                        responseFields(libraryListResponseFields()),
                        resource(ResourceSnippetParameters.builder()
                                .summary(LIBRARY_LIST_SUMMARY)
                                .description(LIBRARY_LIST_DESCRIPTION)
                                .tag(LIBRARY_TAG)
                                .queryParameters(libraryListResourceQueryParameters())
                                .responseFields(libraryListResponseFields())
                                .build())
                ));

        verify(libraryService).getLibrary(MEMBER_ID, 1, ReadingStatus.READING, LibrarySort.RATING);
    }

    @Test
    @DisplayName("책등과 책뒷면 이미지가 없으면 null로 반환한다")
    void should_ReturnNullSpineAndBackImageUrls_When_ImagesAreMissing() throws Exception {
        // given
        LibraryListResponse response = new LibraryListResponse(
                1,
                1,
                null,
                List.of(libraryItemResponseWithoutImages())
        );
        when(libraryService.getLibrary(MEMBER_ID, 1, ReadingStatus.READING, LibrarySort.RATING))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/library")
                        .param("page", "1")
                        .param("status", "READING")
                        .param("sort", "RATING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].spineImageUrl").value(nullValue()))
                .andExpect(jsonPath("$.items[0].backImageUrl").value(nullValue()));
    }

    @Test
    @DisplayName("서재 조회 페이지가 유효하지 않다면 문제 응답을 반환한다")
    void should_ReturnProblemDetail_When_LibraryListPageIsInvalid() throws Exception {
        // when & then
        expectProblemDetail(
                mockMvc.perform(get("/api/v1/library").param("page", "0")),
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_REQUEST,
                "/api/v1/library"
        )
                .andDo(problemDetailDocument(
                        "library-list-invalid-request",
                        LIBRARY_LIST_SUMMARY,
                        LIBRARY_LIST_DESCRIPTION,
                        noPathParameters(),
                        libraryListResourceQueryParameters()
                ));
    }

    @Test
    @DisplayName("비로그인 사용자도 활성 회원의 공개 서재를 조회할 수 있다")
    void should_ReturnPublicLibrary_When_RequestIsValid() throws Exception {
        // given
        PublicLibraryListResponse response = new PublicLibraryListResponse(
                new PublicMemberResponse(
                        MEMBER_ID,
                        "책책이",
                        "https://example.com/profile.jpg"
                ),
                1,
                1,
                null,
                List.of(PublicLibraryItemResponse.from(libraryItemResponse()))
        );
        when(libraryService.getPublicLibrary(MEMBER_ID, 1, ReadingStatus.READING, LibrarySort.RECENT))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/members/{memberId}/library", MEMBER_ID)
                        .param("page", "1")
                        .param("status", "READING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member.memberId").value(MEMBER_ID))
                .andExpect(jsonPath("$.member.displayName").value("책책이"))
                .andExpect(jsonPath("$.member.profileImageUrl").value("https://example.com/profile.jpg"))
                .andExpect(jsonPath("$.items[0].bookId").value(BOOK_ID))
                .andExpect(jsonPath("$.items[0].spineImageUrl")
                        .value("https://image.yes24.com/goods/118578901/side"))
                .andExpect(jsonPath("$.items[0].backImageUrl")
                        .value("https://image.yes24.com/goods/118578901/back"))
                .andExpect(jsonPath("$.items[0].status").value("READING"))
                .andExpect(jsonPath("$.items[0].currentPage").value(100))
                .andExpect(jsonPath("$.items[0].rating").value(4.5))
                .andExpect(jsonPath("$.items[0].addedAt").doesNotExist())
                .andExpect(jsonPath("$.items[0].readingUpdatedAt").doesNotExist())
                .andDo(document(
                        "public-library-list",
                        pathParameters(memberIdPathParameter()),
                        queryParameters(libraryListQueryParameters()),
                        responseFields(publicLibraryListResponseFields()),
                        resource(ResourceSnippetParameters.builder()
                                .summary(PUBLIC_LIBRARY_LIST_SUMMARY)
                                .description(PUBLIC_LIBRARY_LIST_DESCRIPTION)
                                .tag(LIBRARY_TAG)
                                .pathParameters(memberIdResourcePathParameters())
                                .queryParameters(libraryListResourceQueryParameters())
                                .responseFields(publicLibraryListResponseFields())
                                .build())
                ));

        verify(libraryService).getPublicLibrary(MEMBER_ID, 1, ReadingStatus.READING, LibrarySort.RECENT);
    }

    @Test
    @DisplayName("공개 서재에서 책등과 책뒷면 이미지가 없으면 null로 반환한다")
    void should_ReturnNullSpineAndBackImageUrls_When_PublicLibraryImagesAreMissing() throws Exception {
        // given
        PublicLibraryListResponse response = new PublicLibraryListResponse(
                new PublicMemberResponse(
                        MEMBER_ID,
                        "책책이",
                        null
                ),
                1,
                1,
                null,
                List.of(PublicLibraryItemResponse.from(libraryItemResponseWithoutImages()))
        );
        when(libraryService.getPublicLibrary(MEMBER_ID, 1, ReadingStatus.READING, LibrarySort.RECENT))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/members/{memberId}/library", MEMBER_ID)
                        .param("page", "1")
                        .param("status", "READING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].spineImageUrl").value(nullValue()))
                .andExpect(jsonPath("$.items[0].backImageUrl").value(nullValue()));
    }

    @Test
    @DisplayName("공개할 수 없는 회원의 서재는 찾을 수 없음 응답을 반환한다")
    void should_ReturnNotFound_When_PublicLibraryIsUnavailable() throws Exception {
        // given
        when(libraryService.getPublicLibrary(MEMBER_ID, 1, null, LibrarySort.RECENT))
                .thenThrow(new BusinessException(ErrorCode.LIBRARY_NOT_FOUND));

        // when & then
        expectProblemDetail(
                mockMvc.perform(get("/api/v1/members/{memberId}/library", MEMBER_ID)
                        .param("page", "1")),
                HttpStatus.NOT_FOUND,
                ErrorCode.LIBRARY_NOT_FOUND,
                "/api/v1/members/" + MEMBER_ID + "/library"
        )
                .andExpect(jsonPath("$.detail").value("책장을 찾을 수 없습니다."))
                .andDo(problemDetailDocument(
                        "public-library-list-not-found",
                        PUBLIC_LIBRARY_LIST_SUMMARY,
                        PUBLIC_LIBRARY_LIST_DESCRIPTION,
                        memberIdResourcePathParameters(),
                        libraryListResourceQueryParameters()
                ));
    }

    @Test
    @DisplayName("유효한 요청이라면 도서를 내 서재에 추가한다")
    void should_AddLibraryItem_When_RequestIsValid() throws Exception {
        // given
        LibraryItemResponse response = libraryItemResponse();
        when(libraryService.addByIsbn13(MEMBER_ID, new Isbn13(ISBN13), ReadingStatus.READING, 368))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/library")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isbn13":"9788936433598","status":"READING","totalPages":368}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        "http://localhost:8080/api/v1/library/" + BOOK_ID
                ))
                .andExpect(jsonPath("$.bookId").value(BOOK_ID))
                .andDo(document(
                        "library-add",
                        requestFields(addLibraryItemRequestFields()),
                        responseFields(LIBRARY_ITEM_RESPONSE_FIELDS),
                        responseHeaders(headerWithName(HttpHeaders.LOCATION).description("생성된 내 서재 항목 URI")),
                        resource(ResourceSnippetParameters.builder()
                                .summary(LIBRARY_ADD_SUMMARY)
                                .description(LIBRARY_ADD_DESCRIPTION)
                                .tag(LIBRARY_TAG)
                                .requestSchema(Schema.schema("AddLibraryItemRequest"))
                                .requestFields(addLibraryItemRequestFields())
                                .responseFields(LIBRARY_ITEM_RESPONSE_FIELDS)
                                .responseHeaders(ResourceDocumentation.headerWithName(HttpHeaders.LOCATION)
                                        .type(SimpleType.STRING).description("생성된 내 서재 항목 URI"))
                                .build())
                ));

        verify(libraryService).addByIsbn13(MEMBER_ID, new Isbn13(ISBN13), ReadingStatus.READING, 368);
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
                                .summary(LIBRARY_UPDATE_SUMMARY)
                                .description(LIBRARY_UPDATE_DESCRIPTION)
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
                                .summary(LIBRARY_DELETE_SUMMARY)
                                .description(LIBRARY_DELETE_DESCRIPTION)
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
                                .summary(LIBRARY_BULK_DELETE_SUMMARY)
                                .description(LIBRARY_BULK_DELETE_DESCRIPTION)
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
                                .summary(LIBRARY_BULK_STATUS_SUMMARY)
                                .description(LIBRARY_BULK_STATUS_DESCRIPTION)
                                .tag(LIBRARY_TAG)
                                .requestSchema(Schema.schema("BulkUpdateLibraryStatusRequest"))
                                .requestFields(bulkStatusRequestFields())
                                .build())
                ));

        verify(libraryService).bulkChangeStatus(
                MEMBER_ID,
                List.of(10L, 20L),
                ReadingStatus.WANT_TO_READ
        );
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
                        responseFields(ratingResponseFields()),
                        resource(ResourceSnippetParameters.builder()
                                .summary(LIBRARY_RATE_SUMMARY)
                                .description(LIBRARY_RATE_DESCRIPTION)
                                .tag(LIBRARY_TAG)
                                .pathParameters(bookIdResourcePathParameter())
                                .requestSchema(Schema.schema("RateBookRequest"))
                                .responseSchema(Schema.schema("LegacyRatingResponse"))
                                .requestFields(ratingRequestFields())
                                .responseFields(ratingResponseFields())
                                .build())
                ));

        verify(libraryService).rate(MEMBER_ID, BOOK_ID, new BigDecimal("4.5"));
    }

    @Test
    @DisplayName("독서 기록 없이 저장한 별점은 nullable 독서 필드를 명시한다")
    void should_ReturnNullReadingFields_When_RatingWithoutReadingRecord() throws Exception {
        // given
        LibraryItemResponse ratingOnly = ratingOnlyResponse();
        when(libraryService.rate(MEMBER_ID, BOOK_ID, new BigDecimal("4.5"))).thenReturn(ratingOnly);

        // when & then
        mockMvc.perform(put("/api/v1/library/{bookId}/rating", BOOK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(nullValue()))
                .andExpect(jsonPath("$.currentPage").value(nullValue()))
                .andExpect(jsonPath("$.readingUpdatedAt").value(nullValue()))
                .andExpect(jsonPath("$.rating").value(4.5))
                .andDo(document(
                        "library-rate-without-reading-state",
                        resource(ResourceSnippetParameters.builder()
                                .summary(LIBRARY_RATE_SUMMARY)
                                .description(LIBRARY_RATE_DESCRIPTION)
                                .tag(LIBRARY_TAG)
                                .pathParameters(bookIdResourcePathParameter())
                                .requestSchema(Schema.schema("RateBookRequest"))
                                .requestFields(ratingRequestFields())
                                .responseSchema(Schema.schema("LegacyRatingResponse"))
                                .responseFields(ratingResponseFields())
                                .build())
                ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("ISBN 별점은 기존 ID 별점과 같은 응답과 nullable 독서 필드를 반환한다")
    void should_ReturnCommonRatingResponse_When_RatingByIsbn(boolean hasReadingState) throws Exception {
        // given
        LibraryItemResponse response = hasReadingState ? libraryItemResponse() : ratingOnlyResponse();
        when(libraryService.rateByIsbn13(MEMBER_ID, new Isbn13(ISBN13), new BigDecimal("4.5")))
                .thenReturn(response);

        // when & then
        putIsbnRating()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookId").value(BOOK_ID))
                .andExpect(jsonPath("$.isbn13").value(ISBN13))
                .andExpect(jsonPath("$.rating").value(4.5))
                .andExpect(jsonPath("$.status").value(hasReadingState ? "READING" : null))
                .andExpect(jsonPath("$.currentPage").value(hasReadingState ? 100 : null))
                .andDo(document(
                        hasReadingState ? "books-rate-by-isbn" : "books-rate-by-isbn-without-reading-state",
                        pathParameters(parameterWithName("isbn13").description("검색, 상세 응답의 ISBN 문자열")),
                        requestFields(ratingRequestFields()),
                        responseFields(ratingResponseFields()),
                        resource(ResourceSnippetParameters.builder()
                                .summary(ISBN_RATE_SUMMARY)
                                .description(ISBN_RATE_DESCRIPTION)
                                .tag("별점")
                                .pathParameters(isbnRatingPathParameters())
                                .requestSchema(Schema.schema("RateBookRequest"))
                                .responseSchema(Schema.schema("LegacyRatingResponse"))
                                .requestFields(ratingRequestFields())
                                .responseFields(ratingResponseFields())
                                .build())
                ));

        verify(libraryService).rateByIsbn13(MEMBER_ID, new Isbn13(ISBN13), new BigDecimal("4.5"));
    }

    @ParameterizedTest
    @EnumSource(value = ErrorCode.class, names = {
            "INVALID_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "BOOK_NOT_FOUND", "EXTERNAL_API_ERROR", "INTERNAL_SERVER_ERROR"
    })
    @DisplayName("ISBN 별점의 입력, 인증과 도서 확보 오류를 문서화한다")
    void should_DocumentProblemDetails_When_RatingByIsbnFails(ErrorCode errorCode) throws Exception {
        // given
        HttpStatus expectedStatus = switch (errorCode) {
            case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case BOOK_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case EXTERNAL_API_ERROR -> HttpStatus.BAD_GATEWAY;
            case INTERNAL_SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> throw new IllegalArgumentException("지원하지 않는 별점 오류 코드");
        };
        when(libraryService.rateByIsbn13(MEMBER_ID, new Isbn13(ISBN13), new BigDecimal("4.5")))
                .thenThrow(new BusinessException(errorCode));

        // when & then
        documentProblemDetail(
                putIsbnRating(),
                expectedStatus,
                errorCode,
                "/api/v1/books/by-isbn/" + ISBN13 + "/rating",
                "books-rate-by-isbn-" + errorCode.name().toLowerCase(Locale.ROOT).replace('_', '-'),
                ISBN_RATE_SUMMARY,
                ISBN_RATE_DESCRIPTION,
                isbnRatingPathParameters(),
                noQueryParameters()
        );
    }

    private FieldDescriptor[] ratingResponseFields() {
        List<String> ratingFields = List.of("status", "currentPage", "readingUpdatedAt", "rating");
        return Stream.concat(
                Stream.of(LIBRARY_ITEM_RESPONSE_FIELDS)
                        .filter(field -> !ratingFields.contains(field.getPath())),
                Stream.of(
                        fieldWithPath("status").type(JsonFieldType.STRING).description("기존 독서 상태, 없으면 null").optional(),
                        fieldWithPath("currentPage").type(JsonFieldType.NUMBER).description("기존 현재 페이지, 없으면 null").optional(),
                        fieldWithPath("readingUpdatedAt").type(JsonFieldType.STRING).description("기존 독서 갱신 시각, 없으면 null").optional(),
                        fieldWithPath("rating").type(JsonFieldType.NUMBER).description("저장한 내 별점")
                )
        ).toArray(FieldDescriptor[]::new);
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
                                .summary(LIBRARY_REMOVE_RATING_SUMMARY)
                                .description(LIBRARY_REMOVE_RATING_DESCRIPTION)
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
                comparisonBook(9L, "9788954699919", "파친코", "이민진", "4.0", Instant.parse("2026-08-01T00:00:00Z")),
                comparisonBook(12L, "9788965746829", "아몬드", "손원평", "4.5", Instant.parse("2026-08-03T00:00:00Z")),
                comparisonBook(11L, "9788956609959", "불편한 편의점", "김호연", "4.8", Instant.parse("2026-08-05T00:00:00Z"))
        );
        when(libraryService.compareRatingsByIsbn13(MEMBER_ID, new Isbn13(ISBN13), new BigDecimal("4.5")))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/members/me/ratings/comparison")
                        .param("isbn13", ISBN13)
                        .param("criterion", "4.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lower.isbn13").value("9788954699919"))
                .andExpect(jsonPath("$.current.bookId").value(12L))
                .andExpect(jsonPath("$.current.isbn13").value("9788965746829"))
                .andExpect(jsonPath("$.higher.isbn13").value("9788956609959"))
                .andExpect(jsonPath("$.lower.ratingUpdatedAt").value("2026-08-01T00:00:00Z"))
                .andExpect(jsonPath("$.current.ratingUpdatedAt").value("2026-08-03T00:00:00Z"))
                .andExpect(jsonPath("$.higher.ratingUpdatedAt").value("2026-08-05T00:00:00Z"))
                .andDo(document(
                        "library-rating-comparison",
                        queryParameters(ratingComparisonQueryParameters()),
                        responseFields(ratingComparisonResponseFields()),
                        resource(ResourceSnippetParameters.builder()
                                .summary(RATING_COMPARISON_SUMMARY)
                                .description(RATING_COMPARISON_DESCRIPTION)
                                .tag(LIBRARY_TAG)
                                .queryParameters(ratingComparisonResourceQueryParameters())
                                .responseFields(ratingComparisonResponseFields())
                                .build())
                ));

        verify(libraryService).compareRatingsByIsbn13(MEMBER_ID, new Isbn13(ISBN13), new BigDecimal("4.5"));
    }

    @Test
    @DisplayName("같은 별점을 준 책이 없다면 current가 null인 응답을 반환한다")
    void should_ReturnNullableRatingComparison_When_NoBookHasCriterionRating() throws Exception {
        // given
        RatingComparisonResponse response = new RatingComparisonResponse(
                null,
                null,
                null
        );
        when(libraryService.compareRatingsByIsbn13(MEMBER_ID, new Isbn13(ISBN13), new BigDecimal("4.5")))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/members/me/ratings/comparison")
                        .param("isbn13", ISBN13)
                        .param("criterion", "4.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lower").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.current").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.higher").value(Matchers.nullValue()))
                .andDo(document(
                        "library-rating-comparison-null-boundary",
                        queryParameters(ratingComparisonQueryParameters()),
                        responseFields(nullableRatingComparisonResponseFields()),
                        resource(ResourceSnippetParameters.builder()
                                .summary(RATING_COMPARISON_SUMMARY)
                                .description(RATING_COMPARISON_DESCRIPTION)
                                .tag(LIBRARY_TAG)
                                .queryParameters(ratingComparisonResourceQueryParameters())
                                .responseFields(nullableRatingComparisonResponseFields())
                                .build())
                ));
    }

    @Test
    @DisplayName("내 서재 추가의 오류 응답을 문서화한다")
    void should_DocumentProblemDetails_When_AddingLibraryItemFails() throws Exception {
        // given
        doThrow(new BusinessException(ErrorCode.INVALID_REQUEST)).when(libraryService)
                .addByIsbn13(MEMBER_ID, new Isbn13(ISBN13), ReadingStatus.READING, 368);

        // when & then
        documentProblemDetail(
                postLibraryItem(),
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_REQUEST,
                "/api/v1/library",
                "library-add-invalid-request",
                LIBRARY_ADD_SUMMARY,
                LIBRARY_ADD_DESCRIPTION,
                noPathParameters(),
                noQueryParameters()
        );

        doThrow(new BusinessException(ErrorCode.BOOK_NOT_FOUND)).when(libraryService)
                .addByIsbn13(MEMBER_ID, new Isbn13(ISBN13), ReadingStatus.READING, 368);
        documentProblemDetail(
                postLibraryItem(),
                HttpStatus.NOT_FOUND,
                ErrorCode.BOOK_NOT_FOUND,
                "/api/v1/library",
                "library-add-book-not-found",
                LIBRARY_ADD_SUMMARY,
                LIBRARY_ADD_DESCRIPTION,
                noPathParameters(),
                noQueryParameters()
        );

        doThrow(new BusinessException(ErrorCode.LIBRARY_ITEM_ALREADY_EXISTS)).when(libraryService)
                .addByIsbn13(MEMBER_ID, new Isbn13(ISBN13), ReadingStatus.READING, 368);
        documentProblemDetail(
                postLibraryItem(),
                HttpStatus.CONFLICT,
                ErrorCode.LIBRARY_ITEM_ALREADY_EXISTS,
                "/api/v1/library",
                "library-add-already-exists",
                LIBRARY_ADD_SUMMARY,
                LIBRARY_ADD_DESCRIPTION,
                noPathParameters(),
                noQueryParameters()
        );

        doThrow(new BusinessException(ErrorCode.LIBRARY_ITEM_LIMIT_EXCEEDED)).when(libraryService)
                .addByIsbn13(MEMBER_ID, new Isbn13(ISBN13), ReadingStatus.READING, 368);
        documentProblemDetail(
                postLibraryItem(),
                HttpStatus.CONFLICT,
                ErrorCode.LIBRARY_ITEM_LIMIT_EXCEEDED,
                "/api/v1/library",
                "library-add-limit-exceeded",
                LIBRARY_ADD_SUMMARY,
                LIBRARY_ADD_DESCRIPTION,
                noPathParameters(),
                noQueryParameters()
        );

        doThrow(new BusinessException(ErrorCode.NICKNAME_REQUIRED)).when(libraryService)
                .addByIsbn13(MEMBER_ID, new Isbn13(ISBN13), ReadingStatus.READING, 368);
        documentProblemDetail(
                postLibraryItem(),
                HttpStatus.UNPROCESSABLE_CONTENT,
                ErrorCode.NICKNAME_REQUIRED,
                "/api/v1/library",
                "library-add-nickname-required",
                LIBRARY_ADD_SUMMARY,
                LIBRARY_ADD_DESCRIPTION,
                noPathParameters(),
                noQueryParameters()
        );

        doThrow(new BusinessException(ErrorCode.TOTAL_PAGES_CONFLICT)).when(libraryService)
                .addByIsbn13(MEMBER_ID, new Isbn13(ISBN13), ReadingStatus.READING, 368);
        documentProblemDetail(
                postLibraryItem(),
                HttpStatus.CONFLICT,
                ErrorCode.TOTAL_PAGES_CONFLICT,
                "/api/v1/library",
                "library-add-total-pages-conflict",
                LIBRARY_ADD_SUMMARY,
                LIBRARY_ADD_DESCRIPTION,
                noPathParameters(),
                noQueryParameters()
        );

        doThrow(new BusinessException(ErrorCode.INVALID_READING_STATE)).when(libraryService)
                .addByIsbn13(MEMBER_ID, new Isbn13(ISBN13), ReadingStatus.READING, 368);
        documentProblemDetail(
                postLibraryItem(),
                HttpStatus.UNPROCESSABLE_CONTENT,
                ErrorCode.INVALID_READING_STATE,
                "/api/v1/library",
                "library-add-invalid-reading-state",
                LIBRARY_ADD_SUMMARY,
                LIBRARY_ADD_DESCRIPTION,
                noPathParameters(),
                noQueryParameters()
        );

    }

    @Test
    @DisplayName("책장 도서 배치 추가의 오류 응답을 문서화한다")
    void should_DocumentProblemDetails_When_AddingBookToLibraryFails() throws Exception {
        // when & then
        documentProblemDetail(
                mockMvc.perform(post("/api/v1/libraries/{libraryId}", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isbn13":"9788936433598","status":"READING","totalPages":0}
                                """)),
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_REQUEST,
                "/api/v1/libraries/7",
                "library-book-add-invalid-request",
                LIBRARY_BOOK_ADD_SUMMARY,
                LIBRARY_BOOK_ADD_DESCRIPTION,
                libraryIdResourcePathParameters(),
                noQueryParameters()
        );

        documentLibraryBookAddProblem(
                ErrorCode.LIBRARY_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "library-book-add-library-not-found"
        );
        documentLibraryBookAddProblem(
                ErrorCode.FORBIDDEN,
                HttpStatus.FORBIDDEN,
                "library-book-add-forbidden"
        );
        documentLibraryBookAddProblem(
                ErrorCode.BOOK_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "library-book-add-book-not-found"
        );
        documentLibraryBookAddProblem(
                ErrorCode.LIBRARY_ITEM_ALREADY_EXISTS,
                HttpStatus.CONFLICT,
                "library-book-add-already-exists"
        );
        documentLibraryBookAddProblem(
                ErrorCode.LIBRARY_ITEM_LIMIT_EXCEEDED,
                HttpStatus.CONFLICT,
                "library-book-add-limit-exceeded"
        );
        documentLibraryBookAddProblem(
                ErrorCode.TOTAL_PAGES_CONFLICT,
                HttpStatus.CONFLICT,
                "library-book-add-total-pages-conflict"
        );
        documentLibraryBookAddProblem(
                ErrorCode.INVALID_READING_STATE,
                HttpStatus.UNPROCESSABLE_CONTENT,
                "library-book-add-invalid-reading-state"
        );
        documentLibraryBookAddProblem(
                ErrorCode.EXTERNAL_API_ERROR,
                HttpStatus.BAD_GATEWAY,
                "library-book-add-external-api-error"
        );
    }

    @Test
    @DisplayName("책장 도서 배치 삭제의 오류 응답을 문서화한다")
    void should_DocumentProblemDetails_When_DeletingBookPlacementFails() throws Exception {
        documentProblemDetail(
                mockMvc.perform(delete("/api/v1/libraries/{libraryId}/books/{bookId}", 0L, BOOK_ID)),
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_REQUEST,
                "/api/v1/libraries/0/books/10",
                "library-book-delete-invalid-request",
                LIBRARY_BOOK_DELETE_SUMMARY,
                LIBRARY_BOOK_DELETE_DESCRIPTION,
                libraryBookDeleteResourcePathParameters(),
                noQueryParameters()
        );

        documentLibraryBookDeleteProblem(
                ErrorCode.FORBIDDEN,
                HttpStatus.FORBIDDEN,
                "library-book-delete-forbidden"
        );
        documentLibraryBookDeleteProblem(
                ErrorCode.LIBRARY_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "library-book-delete-library-not-found"
        );
    }

    @Test
    @DisplayName("책장 삭제의 오류 응답을 문서화한다")
    void should_DocumentProblemDetails_When_DeletingLibraryFails() throws Exception {
        documentProblemDetail(
                mockMvc.perform(delete("/api/v1/libraries/{libraryId}", 0L)),
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_REQUEST,
                "/api/v1/libraries/0",
                "library-delete-library-invalid-request",
                LIBRARY_DELETE_LIBRARY_SUMMARY,
                LIBRARY_DELETE_LIBRARY_DESCRIPTION,
                libraryDeleteResourcePathParameters(),
                noQueryParameters()
        );

        documentLibraryDeleteProblem(
                ErrorCode.FORBIDDEN,
                HttpStatus.FORBIDDEN,
                "library-delete-library-forbidden"
        );
        documentLibraryDeleteProblem(
                ErrorCode.LIBRARY_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "library-delete-library-not-found"
        );
        documentLibraryDeleteProblem(
                ErrorCode.DEFAULT_LIBRARY_DELETE_NOT_ALLOWED,
                HttpStatus.CONFLICT,
                "library-delete-library-default-library"
        );
    }

    @Test
    @DisplayName("책장 이름 변경의 오류 응답을 문서화한다")
    void should_DocumentProblemDetails_When_RenamingLibraryFails() throws Exception {
        documentProblemDetail(
                patchLibraryRename(0L),
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_REQUEST,
                "/api/v1/libraries/0",
                "library-rename-invalid-request",
                LIBRARY_RENAME_SUMMARY,
                LIBRARY_RENAME_DESCRIPTION,
                libraryRenameResourcePathParameters(),
                noQueryParameters()
        );
        documentLibraryRenameProblem(
                ErrorCode.FORBIDDEN,
                HttpStatus.FORBIDDEN,
                "library-rename-forbidden"
        );
        documentLibraryRenameProblem(
                ErrorCode.LIBRARY_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "library-rename-library-not-found"
        );
        documentLibraryRenameProblem(
                ErrorCode.LIBRARY_NAME_ALREADY_EXISTS,
                HttpStatus.CONFLICT,
                "library-rename-name-already-exists"
        );
        documentLibraryRenameProblem(
                ErrorCode.INTERNAL_SERVER_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "library-rename-internal-server-error"
        );
    }

    private void documentLibraryDeleteProblem(ErrorCode errorCode, HttpStatus status, String identifier)
            throws Exception {
        doThrow(new BusinessException(errorCode)).when(libraryBookService).deleteLibrary(MEMBER_ID, 7L);
        documentProblemDetail(
                mockMvc.perform(delete("/api/v1/libraries/{libraryId}", 7L)),
                status,
                errorCode,
                "/api/v1/libraries/7",
                identifier,
                LIBRARY_DELETE_LIBRARY_SUMMARY,
                LIBRARY_DELETE_LIBRARY_DESCRIPTION,
                libraryDeleteResourcePathParameters(),
                noQueryParameters()
        );
    }

    private void documentLibraryRenameProblem(ErrorCode errorCode, HttpStatus status, String identifier)
            throws Exception {
        doThrow(new BusinessException(errorCode)).when(libraryService).rename(MEMBER_ID, 7L, "읽을책");
        documentProblemDetail(
                patchLibraryRename(7L),
                status,
                errorCode,
                "/api/v1/libraries/7",
                identifier,
                LIBRARY_RENAME_SUMMARY,
                LIBRARY_RENAME_DESCRIPTION,
                libraryRenameResourcePathParameters(),
                noQueryParameters()
        );
    }

    private void documentLibraryBookDeleteProblem(ErrorCode errorCode, HttpStatus status, String identifier)
            throws Exception {
        doThrow(new BusinessException(errorCode)).when(libraryBookService).delete(MEMBER_ID, 7L, BOOK_ID);
        documentProblemDetail(
                mockMvc.perform(delete("/api/v1/libraries/{libraryId}/books/{bookId}", 7L, BOOK_ID)),
                status,
                errorCode,
                "/api/v1/libraries/7/books/10",
                identifier,
                LIBRARY_BOOK_DELETE_SUMMARY,
                LIBRARY_BOOK_DELETE_DESCRIPTION,
                libraryBookDeleteResourcePathParameters(),
                noQueryParameters()
        );
    }

    private void documentLibraryBookAddProblem(ErrorCode errorCode, HttpStatus status, String identifier)
            throws Exception {
        doThrow(new BusinessException(errorCode)).when(libraryBookService)
                .addByIsbn13(MEMBER_ID, 7L, new Isbn13(ISBN13), ReadingStatus.READING, 368);
        documentProblemDetail(
                postLibraryBook(7L),
                status,
                errorCode,
                "/api/v1/libraries/7",
                identifier,
                LIBRARY_BOOK_ADD_SUMMARY,
                LIBRARY_BOOK_ADD_DESCRIPTION,
                libraryIdResourcePathParameters(),
                noQueryParameters()
        );
    }

    @Test
    @DisplayName("내 서재 수정의 오류 응답을 문서화한다")
    void should_DocumentProblemDetails_When_UpdatingLibraryItemFails() throws Exception {
        // given
        doThrow(new BusinessException(ErrorCode.INVALID_REQUEST)).when(libraryService)
                .update(MEMBER_ID, BOOK_ID, null, 150, 368);

        // when & then
        documentProblemDetail(
                patchLibraryItem(),
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_REQUEST,
                "/api/v1/library/10",
                "library-update-invalid-request",
                LIBRARY_UPDATE_SUMMARY,
                LIBRARY_UPDATE_DESCRIPTION,
                bookIdResourcePathParameters(),
                noQueryParameters()
        );

        doThrow(new BusinessException(ErrorCode.LIBRARY_ITEM_NOT_FOUND)).when(libraryService)
                .update(MEMBER_ID, BOOK_ID, null, 150, 368);
        documentProblemDetail(
                patchLibraryItem(),
                HttpStatus.NOT_FOUND,
                ErrorCode.LIBRARY_ITEM_NOT_FOUND,
                "/api/v1/library/10",
                "library-update-not-found",
                LIBRARY_UPDATE_SUMMARY,
                LIBRARY_UPDATE_DESCRIPTION,
                bookIdResourcePathParameters(),
                noQueryParameters()
        );

        doThrow(new BusinessException(ErrorCode.TOTAL_PAGES_CONFLICT)).when(libraryService)
                .update(MEMBER_ID, BOOK_ID, null, 150, 368);
        documentProblemDetail(
                patchLibraryItem(),
                HttpStatus.CONFLICT,
                ErrorCode.TOTAL_PAGES_CONFLICT,
                "/api/v1/library/10",
                "library-update-total-pages-conflict",
                LIBRARY_UPDATE_SUMMARY,
                LIBRARY_UPDATE_DESCRIPTION,
                bookIdResourcePathParameters(),
                noQueryParameters()
        );

        doThrow(new BusinessException(ErrorCode.INVALID_READING_STATE)).when(libraryService)
                .update(MEMBER_ID, BOOK_ID, null, 150, 368);
        documentProblemDetail(
                patchLibraryItem(),
                HttpStatus.UNPROCESSABLE_CONTENT,
                ErrorCode.INVALID_READING_STATE,
                "/api/v1/library/10",
                "library-update-invalid-reading-state",
                LIBRARY_UPDATE_SUMMARY,
                LIBRARY_UPDATE_DESCRIPTION,
                bookIdResourcePathParameters(),
                noQueryParameters()
        );
    }

    @Test
    @DisplayName("내 서재 삭제와 일괄 처리의 오류 응답을 문서화한다")
    void should_DocumentProblemDetails_When_DeletingLibraryItemsFails() throws Exception {
        // given
        doThrow(new BusinessException(ErrorCode.BOOK_NOT_FOUND)).when(libraryService).delete(MEMBER_ID, BOOK_ID);

        // when & then
        documentProblemDetail(
                mockMvc.perform(delete("/api/v1/library/{bookId}", BOOK_ID)),
                HttpStatus.NOT_FOUND,
                ErrorCode.BOOK_NOT_FOUND,
                "/api/v1/library/10",
                "library-delete-book-not-found",
                LIBRARY_DELETE_SUMMARY,
                LIBRARY_DELETE_DESCRIPTION,
                bookIdResourcePathParameters(),
                noQueryParameters()
        );

        doThrow(new BusinessException(ErrorCode.INVALID_REQUEST)).when(libraryService)
                .bulkDelete(MEMBER_ID, List.of(10L, 20L));
        documentProblemDetail(
                postBulkDelete(),
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_REQUEST,
                "/api/v1/library/bulk-delete",
                "library-bulk-delete-invalid-request",
                LIBRARY_BULK_DELETE_SUMMARY,
                LIBRARY_BULK_DELETE_DESCRIPTION,
                noPathParameters(),
                noQueryParameters()
        );

        doThrow(new BusinessException(ErrorCode.LIBRARY_ITEM_NOT_FOUND)).when(libraryService)
                .bulkDelete(MEMBER_ID, List.of(10L, 20L));
        documentProblemDetail(
                postBulkDelete(),
                HttpStatus.NOT_FOUND,
                ErrorCode.LIBRARY_ITEM_NOT_FOUND,
                "/api/v1/library/bulk-delete",
                "library-bulk-delete-not-found",
                LIBRARY_BULK_DELETE_SUMMARY,
                LIBRARY_BULK_DELETE_DESCRIPTION,
                noPathParameters(),
                noQueryParameters()
        );

        doThrow(new BusinessException(ErrorCode.INVALID_REQUEST)).when(libraryService)
                .bulkChangeStatus(MEMBER_ID, List.of(10L, 20L), ReadingStatus.WANT_TO_READ);
        documentProblemDetail(
                patchBulkStatus(),
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_REQUEST,
                "/api/v1/library/bulk-status",
                "library-bulk-status-invalid-request",
                LIBRARY_BULK_STATUS_SUMMARY,
                LIBRARY_BULK_STATUS_DESCRIPTION,
                noPathParameters(),
                noQueryParameters()
        );

        doThrow(new BusinessException(ErrorCode.LIBRARY_ITEM_NOT_FOUND)).when(libraryService)
                .bulkChangeStatus(MEMBER_ID, List.of(10L, 20L), ReadingStatus.WANT_TO_READ);
        documentProblemDetail(
                patchBulkStatus(),
                HttpStatus.NOT_FOUND,
                ErrorCode.LIBRARY_ITEM_NOT_FOUND,
                "/api/v1/library/bulk-status",
                "library-bulk-status-not-found",
                LIBRARY_BULK_STATUS_SUMMARY,
                LIBRARY_BULK_STATUS_DESCRIPTION,
                noPathParameters(),
                noQueryParameters()
        );

        doThrow(new BusinessException(ErrorCode.INVALID_READING_STATE)).when(libraryService)
                .bulkChangeStatus(MEMBER_ID, List.of(10L, 20L), ReadingStatus.WANT_TO_READ);
        documentProblemDetail(
                patchBulkStatus(),
                HttpStatus.UNPROCESSABLE_CONTENT,
                ErrorCode.INVALID_READING_STATE,
                "/api/v1/library/bulk-status",
                "library-bulk-status-invalid-reading-state",
                LIBRARY_BULK_STATUS_SUMMARY,
                LIBRARY_BULK_STATUS_DESCRIPTION,
                noPathParameters(),
                noQueryParameters()
        );
    }

    @Test
    @DisplayName("별점 API의 오류 응답을 문서화한다")
    void should_DocumentProblemDetails_When_RatingOperationsFail() throws Exception {
        // given
        doThrow(new BusinessException(ErrorCode.INVALID_REQUEST)).when(libraryService)
                .rate(MEMBER_ID, BOOK_ID, new BigDecimal("4.5"));

        // when & then
        documentProblemDetail(
                putRating(),
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_REQUEST,
                "/api/v1/library/10/rating",
                "library-rate-invalid-request",
                LIBRARY_RATE_SUMMARY,
                LIBRARY_RATE_DESCRIPTION,
                bookIdResourcePathParameters(),
                noQueryParameters()
        );

        doThrow(new BusinessException(ErrorCode.BOOK_NOT_FOUND)).when(libraryService)
                .rate(MEMBER_ID, BOOK_ID, new BigDecimal("4.5"));
        documentProblemDetail(
                putRating(),
                HttpStatus.NOT_FOUND,
                ErrorCode.BOOK_NOT_FOUND,
                "/api/v1/library/10/rating",
                "library-rate-book-not-found",
                LIBRARY_RATE_SUMMARY,
                LIBRARY_RATE_DESCRIPTION,
                bookIdResourcePathParameters(),
                noQueryParameters()
        );

        doThrow(new BusinessException(ErrorCode.BOOK_NOT_FOUND)).when(libraryService)
                .removeRating(MEMBER_ID, BOOK_ID);
        documentProblemDetail(
                mockMvc.perform(delete("/api/v1/library/{bookId}/rating", BOOK_ID)),
                HttpStatus.NOT_FOUND,
                ErrorCode.BOOK_NOT_FOUND,
                "/api/v1/library/10/rating",
                "library-remove-rating-book-not-found",
                LIBRARY_REMOVE_RATING_SUMMARY,
                LIBRARY_REMOVE_RATING_DESCRIPTION,
                bookIdResourcePathParameters(),
                noQueryParameters()
        );
    }

    @Test
    @DisplayName("별점 비교의 오류 응답을 문서화한다")
    void should_DocumentProblemDetails_When_ComparingRatingsFails() throws Exception {
        // given
        doThrow(new BusinessException(ErrorCode.INVALID_REQUEST)).when(libraryService)
                .compareRatingsByIsbn13(MEMBER_ID, new Isbn13(ISBN13), new BigDecimal("4.5"));

        // when & then
        documentProblemDetail(
                getRatingComparison(),
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_REQUEST,
                "/api/v1/members/me/ratings/comparison",
                "library-rating-comparison-invalid-request",
                RATING_COMPARISON_SUMMARY,
                RATING_COMPARISON_DESCRIPTION,
                noPathParameters(),
                ratingComparisonResourceQueryParameters()
        );

        doThrow(new BusinessException(ErrorCode.BOOK_NOT_FOUND)).when(libraryService)
                .compareRatingsByIsbn13(MEMBER_ID, new Isbn13(ISBN13), new BigDecimal("4.5"));
        documentProblemDetail(
                getRatingComparison(),
                HttpStatus.NOT_FOUND,
                ErrorCode.BOOK_NOT_FOUND,
                "/api/v1/members/me/ratings/comparison",
                "library-rating-comparison-book-not-found",
                RATING_COMPARISON_SUMMARY,
                RATING_COMPARISON_DESCRIPTION,
                noPathParameters(),
                ratingComparisonResourceQueryParameters()
        );
    }

    @Test
    @DisplayName("인증이 필요하다면 모든 내 서재 API가 401 응답을 반환한다")
    void should_DocumentUnauthorizedProblemDetails_When_MemberIsNotAuthenticated() throws Exception {
        // given
        when(currentMemberIdProvider.getCurrentMemberId()).thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED));
        when(currentMemberIdProvider.getCurrentMemberIdForWrite())
                .thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED));

        // when & then
        documentProblemDetail(
                mockMvc.perform(post("/api/v1/libraries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"올해읽을책"}
                                """)),
                HttpStatus.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED,
                "/api/v1/libraries",
                "library-create-unauthorized",
                LIBRARY_CREATE_SUMMARY,
                LIBRARY_CREATE_DESCRIPTION,
                noPathParameters(),
                noQueryParameters()
        );
        documentProblemDetail(
                patchLibraryRename(7L),
                HttpStatus.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED,
                "/api/v1/libraries/7",
                "library-rename-unauthorized",
                LIBRARY_RENAME_SUMMARY,
                LIBRARY_RENAME_DESCRIPTION,
                libraryRenameResourcePathParameters(),
                noQueryParameters()
        );
        documentProblemDetail(
                postLibraryBook(7L),
                HttpStatus.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED,
                "/api/v1/libraries/7",
                "library-book-add-unauthorized",
                LIBRARY_BOOK_ADD_SUMMARY,
                LIBRARY_BOOK_ADD_DESCRIPTION,
                libraryIdResourcePathParameters(),
                noQueryParameters()
        );
        documentProblemDetail(
                mockMvc.perform(get("/api/v1/library").param("page", "1")),
                HttpStatus.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED,
                "/api/v1/library",
                "library-list-unauthorized",
                LIBRARY_LIST_SUMMARY,
                LIBRARY_LIST_DESCRIPTION,
                noPathParameters(),
                libraryListResourceQueryParameters()
        );
        documentProblemDetail(
                postLibraryItem(),
                HttpStatus.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED,
                "/api/v1/library",
                "library-add-unauthorized",
                LIBRARY_ADD_SUMMARY,
                LIBRARY_ADD_DESCRIPTION,
                noPathParameters(),
                noQueryParameters()
        );
        documentProblemDetail(
                patchLibraryItem(),
                HttpStatus.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED,
                "/api/v1/library/10",
                "library-update-unauthorized",
                LIBRARY_UPDATE_SUMMARY,
                LIBRARY_UPDATE_DESCRIPTION,
                bookIdResourcePathParameters(),
                noQueryParameters()
        );
        documentProblemDetail(
                mockMvc.perform(delete("/api/v1/library/{bookId}", BOOK_ID)),
                HttpStatus.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED,
                "/api/v1/library/10",
                "library-delete-unauthorized",
                LIBRARY_DELETE_SUMMARY,
                LIBRARY_DELETE_DESCRIPTION,
                bookIdResourcePathParameters(),
                noQueryParameters()
        );
        documentProblemDetail(
                mockMvc.perform(delete("/api/v1/libraries/{libraryId}/books/{bookId}", 7L, BOOK_ID)),
                HttpStatus.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED,
                "/api/v1/libraries/7/books/10",
                "library-book-delete-unauthorized",
                LIBRARY_BOOK_DELETE_SUMMARY,
                LIBRARY_BOOK_DELETE_DESCRIPTION,
                libraryBookDeleteResourcePathParameters(),
                noQueryParameters()
        );
        documentProblemDetail(
                mockMvc.perform(delete("/api/v1/libraries/{libraryId}", 7L)),
                HttpStatus.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED,
                "/api/v1/libraries/7",
                "library-delete-library-unauthorized",
                LIBRARY_DELETE_LIBRARY_SUMMARY,
                LIBRARY_DELETE_LIBRARY_DESCRIPTION,
                libraryDeleteResourcePathParameters(),
                noQueryParameters()
        );
        documentProblemDetail(
                postBulkDelete(),
                HttpStatus.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED,
                "/api/v1/library/bulk-delete",
                "library-bulk-delete-unauthorized",
                LIBRARY_BULK_DELETE_SUMMARY,
                LIBRARY_BULK_DELETE_DESCRIPTION,
                noPathParameters(),
                noQueryParameters()
        );
        documentProblemDetail(
                patchBulkStatus(),
                HttpStatus.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED,
                "/api/v1/library/bulk-status",
                "library-bulk-status-unauthorized",
                LIBRARY_BULK_STATUS_SUMMARY,
                LIBRARY_BULK_STATUS_DESCRIPTION,
                noPathParameters(),
                noQueryParameters()
        );
        documentProblemDetail(
                putRating(),
                HttpStatus.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED,
                "/api/v1/library/10/rating",
                "library-rate-unauthorized",
                LIBRARY_RATE_SUMMARY,
                LIBRARY_RATE_DESCRIPTION,
                bookIdResourcePathParameters(),
                noQueryParameters()
        );
        documentProblemDetail(
                mockMvc.perform(delete("/api/v1/library/{bookId}/rating", BOOK_ID)),
                HttpStatus.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED,
                "/api/v1/library/10/rating",
                "library-remove-rating-unauthorized",
                LIBRARY_REMOVE_RATING_SUMMARY,
                LIBRARY_REMOVE_RATING_DESCRIPTION,
                bookIdResourcePathParameters(),
                noQueryParameters()
        );
        documentProblemDetail(
                getRatingComparison(),
                HttpStatus.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED,
                "/api/v1/members/me/ratings/comparison",
                "library-rating-comparison-unauthorized",
                RATING_COMPARISON_SUMMARY,
                RATING_COMPARISON_DESCRIPTION,
                noPathParameters(),
                ratingComparisonResourceQueryParameters()
        );
    }

    @Test
    @DisplayName("비활성 회원의 기존 서재 쓰기는 FORBIDDEN으로 구분한다")
    void should_DocumentForbidden_When_AuthenticatedMemberIsInactive() throws Exception {
        // given
        when(currentMemberIdProvider.getCurrentMemberIdForWrite()).thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

        // when & then
        documentProblemDetail(
                postLibraryItem(),
                HttpStatus.FORBIDDEN,
                ErrorCode.FORBIDDEN,
                "/api/v1/library",
                "library-add-forbidden",
                LIBRARY_ADD_SUMMARY,
                LIBRARY_ADD_DESCRIPTION,
                noPathParameters(),
                noQueryParameters()
        );
        documentProblemDetail(
                patchLibraryItem(),
                HttpStatus.FORBIDDEN,
                ErrorCode.FORBIDDEN,
                "/api/v1/library/10",
                "library-update-forbidden",
                LIBRARY_UPDATE_SUMMARY,
                LIBRARY_UPDATE_DESCRIPTION,
                bookIdResourcePathParameters(),
                noQueryParameters()
        );
        documentProblemDetail(
                mockMvc.perform(delete("/api/v1/library/{bookId}", BOOK_ID)),
                HttpStatus.FORBIDDEN,
                ErrorCode.FORBIDDEN,
                "/api/v1/library/10",
                "library-delete-forbidden",
                LIBRARY_DELETE_SUMMARY,
                LIBRARY_DELETE_DESCRIPTION,
                bookIdResourcePathParameters(),
                noQueryParameters()
        );
        documentProblemDetail(
                postBulkDelete(),
                HttpStatus.FORBIDDEN,
                ErrorCode.FORBIDDEN,
                "/api/v1/library/bulk-delete",
                "library-bulk-delete-forbidden",
                LIBRARY_BULK_DELETE_SUMMARY,
                LIBRARY_BULK_DELETE_DESCRIPTION,
                noPathParameters(),
                noQueryParameters()
        );
        documentProblemDetail(
                patchBulkStatus(),
                HttpStatus.FORBIDDEN,
                ErrorCode.FORBIDDEN,
                "/api/v1/library/bulk-status",
                "library-bulk-status-forbidden",
                LIBRARY_BULK_STATUS_SUMMARY,
                LIBRARY_BULK_STATUS_DESCRIPTION,
                noPathParameters(),
                noQueryParameters()
        );
        documentProblemDetail(
                putRating(),
                HttpStatus.FORBIDDEN,
                ErrorCode.FORBIDDEN,
                "/api/v1/library/10/rating",
                "library-rate-forbidden",
                LIBRARY_RATE_SUMMARY,
                LIBRARY_RATE_DESCRIPTION,
                bookIdResourcePathParameters(),
                noQueryParameters()
        );
        documentProblemDetail(
                mockMvc.perform(delete("/api/v1/library/{bookId}/rating", BOOK_ID)),
                HttpStatus.FORBIDDEN,
                ErrorCode.FORBIDDEN,
                "/api/v1/library/10/rating",
                "library-remove-rating-forbidden",
                LIBRARY_REMOVE_RATING_SUMMARY,
                LIBRARY_REMOVE_RATING_DESCRIPTION,
                bookIdResourcePathParameters(),
                noQueryParameters()
        );
    }

    @Test
    @DisplayName("기존 일괄 삭제와 수정의 ID 중복은 INVALID_REQUEST로 거절한다")
    void should_RejectDuplicateBookIds_When_UsingLegacyBulkWrites() throws Exception {
        // given

        // when & then
        mockMvc.perform(post("/api/v1/library/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookIds\":[10,10]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mockMvc.perform(patch("/api/v1/library/bulk-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookIds\":[10,10],\"status\":\"READING\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(libraryService);
    }

    private ResultActions expectProblemDetail(
            ResultActions result,
            HttpStatus status,
            ErrorCode errorCode,
            String instance
    )
            throws Exception {
        return result
                .andExpect(status().is(status.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value(status.getReasonPhrase()))
                .andExpect(jsonPath("$.status").value(status.value()))
                .andExpect(jsonPath("$.code").value(errorCode.getCode()))
                .andExpect(jsonPath("$.instance").value(instance));
    }

    private RestDocumentationResultHandler problemDetailDocument(
            String identifier,
            String summary,
            String description,
            ParameterDescriptorWithType[] pathParameters,
            ParameterDescriptorWithType[] queryParameters
    ) {
        var parameters = ResourceSnippetParameters.builder()
                .summary(summary)
                .description(description)
                .tag(documentationTag(summary))
                .pathParameters(pathParameters)
                .queryParameters(queryParameters)
                .responseSchema(Schema.schema("ProblemDetail"))
                .responseFields(PROBLEM_DETAIL_FIELDS);
        if (summary.equals(ISBN_RATE_SUMMARY) || summary.equals(LIBRARY_RATE_SUMMARY)) {
            parameters.requestSchema(Schema.schema("RateBookRequest"))
                    .requestFields(ratingRequestFields());
        }
        return document(
                identifier,
                responseFields(PROBLEM_DETAIL_FIELDS),
                resource(parameters.build())
        );
    }

    private String documentationTag(String summary) {
        return switch (summary) {
            case LIBRARY_CREATE_SUMMARY, LIBRARY_RENAME_SUMMARY, LIBRARY_DELETE_LIBRARY_SUMMARY -> "책장";
            case LIBRARY_BOOK_ADD_SUMMARY, LIBRARY_BOOK_DELETE_SUMMARY -> "책장 도서";
            case ISBN_RATE_SUMMARY -> "별점";
            default -> LIBRARY_TAG;
        };
    }

    private void documentProblemDetail(
            ResultActions result,
            HttpStatus status,
            ErrorCode errorCode,
            String instance,
            String identifier,
            String summary,
            String description,
            ParameterDescriptorWithType[] pathParameters,
            ParameterDescriptorWithType[] queryParameters
    ) throws Exception {
        expectProblemDetail(result, status, errorCode, instance)
                .andDo(problemDetailDocument(identifier, summary, description, pathParameters, queryParameters));
    }

    private ResultActions postLibraryItem() throws Exception {
        return mockMvc.perform(post("/api/v1/library")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"isbn13":"9788936433598","status":"READING","totalPages":368}
                        """));
    }

    private ResultActions postLibraryBook(long libraryId) throws Exception {
        return mockMvc.perform(post("/api/v1/libraries/{libraryId}", libraryId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"isbn13":"9788936433598","status":"READING","totalPages":368}
                        """));
    }

    private ResultActions patchLibraryRename(long libraryId) throws Exception {
        return mockMvc.perform(patch("/api/v1/libraries/{libraryId}", libraryId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"읽을책"}
                        """));
    }

    private ResultActions patchLibraryItem() throws Exception {
        return mockMvc.perform(patch("/api/v1/library/{bookId}", BOOK_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"currentPage":150,"totalPages":368}
                        """));
    }

    private ResultActions postBulkDelete() throws Exception {
        return mockMvc.perform(post("/api/v1/library/bulk-delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"bookIds":[10,20]}
                        """));
    }

    private ResultActions patchBulkStatus() throws Exception {
        return mockMvc.perform(patch("/api/v1/library/bulk-status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"bookIds":[10,20],"status":"WANT_TO_READ"}
                        """));
    }

    private ResultActions putRating() throws Exception {
        return mockMvc.perform(put("/api/v1/library/{bookId}/rating", BOOK_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"rating":4.5}
                        """));
    }

    private ResultActions putIsbnRating() throws Exception {
        return mockMvc.perform(put("/api/v1/books/by-isbn/{isbn13}/rating", ISBN13)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":4.5}"));
    }

    private ParameterDescriptorWithType[] isbnRatingPathParameters() {
        return new ParameterDescriptorWithType[]{
                ResourceDocumentation.parameterWithName("isbn13").type(SimpleType.STRING)
                        .description("검색, 상세 응답의 ISBN 문자열")
        };
    }

    private ResultActions getRatingComparison() throws Exception {
        return mockMvc.perform(get("/api/v1/members/me/ratings/comparison")
                .param("isbn13", ISBN13)
                .param("criterion", "4.5"));
    }

    private ParameterDescriptor[] libraryListQueryParameters() {
        return new ParameterDescriptor[]{
                parameterWithName("page").description("1부터 시작하는 페이지 번호"),
                parameterWithName("status").description("필터할 읽기 상태. 생략하면 전체 상태를 조회한다").optional(),
                parameterWithName("sort").description("정렬 기준. 생략하면 RECENT를 사용한다").optional()
        };
    }

    private ParameterDescriptorWithType[] libraryListResourceQueryParameters() {
        return new ParameterDescriptorWithType[]{
                ResourceDocumentation.parameterWithName("page").type(SimpleType.INTEGER)
                        .description("1부터 시작하는 페이지 번호"),
                ResourceDocumentation.parameterWithName("status").type(SimpleType.STRING)
                        .description("필터할 읽기 상태. 생략하면 전체 상태를 조회한다").optional(),
                ResourceDocumentation.parameterWithName("sort").type(SimpleType.STRING)
                        .description("정렬 기준. 생략하면 RECENT를 사용한다").optional()
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
                fieldWithPath("items[].spineImageUrl").type(JsonFieldType.STRING)
                        .description("책등 이미지 URL").optional(),
                fieldWithPath("items[].backImageUrl").type(JsonFieldType.STRING)
                        .description("책뒷면 이미지 URL").optional(),
                fieldWithPath("items[].authors").type(JsonFieldType.ARRAY).description("저자 이름 목록")
                        .attributes(key("itemsType").value(JsonFieldType.STRING)),
                fieldWithPath("items[].translators").type(JsonFieldType.ARRAY).description("옮긴이 이름 목록")
                        .attributes(key("itemsType").value(JsonFieldType.STRING)),
                fieldWithPath("items[].publisher").type(JsonFieldType.STRING).description("출판사"),
                fieldWithPath("items[].category").type(JsonFieldType.STRING).description("도서 카테고리"),
                fieldWithPath("items[].publishedDate").type(JsonFieldType.STRING).description("출판일"),
                fieldWithPath("items[].totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수").optional(),
                fieldWithPath("items[].commentCount").type(JsonFieldType.NUMBER).description("감상과 답글 수"),
                fieldWithPath("items[].status").type(JsonFieldType.STRING)
                        .description("읽기 상태. 독서 기록이 초기화된 항목이면 null")
                        .attributes(key("nullable").value(true)),
                fieldWithPath("items[].currentPage").type(JsonFieldType.NUMBER)
                        .description("현재 읽은 페이지. 독서 기록이 초기화된 항목이면 null")
                        .attributes(key("nullable").value(true)),
                fieldWithPath("items[].rating").type(JsonFieldType.NUMBER).description("내 별점").optional(),
                fieldWithPath("items[].addedAt").type(JsonFieldType.STRING).description("서재 추가 시각"),
                fieldWithPath("items[].readingUpdatedAt").type(JsonFieldType.STRING)
                        .description("읽기 정보 갱신 시각. 독서 기록이 초기화된 항목이면 null")
                        .attributes(key("nullable").value(true))
        };
    }

    private FieldDescriptor[] publicLibraryListResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("member").type(JsonFieldType.OBJECT).description("공개 서재 회원 정보"),
                fieldWithPath("member.memberId").type(JsonFieldType.NUMBER).description("회원 ID"),
                fieldWithPath("member.displayName").type(JsonFieldType.STRING)
                        .description("회원 표시 이름"),
                fieldWithPath("member.profileImageUrl").type(JsonFieldType.STRING)
                        .description("회원 프로필 이미지 URL").optional(),
                fieldWithPath("totalCount").type(JsonFieldType.NUMBER).description("공개 서재 전체 도서 수"),
                fieldWithPath("filteredCount").type(JsonFieldType.NUMBER).description("필터 적용 후 도서 수"),
                fieldWithPath("nextPage").type(JsonFieldType.NUMBER)
                        .description("다음 페이지 번호. 마지막 페이지면 null").optional(),
                fieldWithPath("items").type(JsonFieldType.ARRAY).description("공개 서재 도서 목록"),
                fieldWithPath("items[].bookId").type(JsonFieldType.NUMBER).description("도서 ID"),
                fieldWithPath("items[].isbn13").type(JsonFieldType.STRING).description("ISBN-13"),
                fieldWithPath("items[].title").type(JsonFieldType.STRING).description("도서 제목"),
                fieldWithPath("items[].coverImageUrl").type(JsonFieldType.STRING).description("표지 이미지 URL"),
                fieldWithPath("items[].spineImageUrl").type(JsonFieldType.STRING)
                        .description("책등 이미지 URL").optional(),
                fieldWithPath("items[].backImageUrl").type(JsonFieldType.STRING)
                        .description("책뒷면 이미지 URL").optional(),
                fieldWithPath("items[].authors").type(JsonFieldType.ARRAY).description("저자 이름 목록")
                        .attributes(key("itemsType").value(JsonFieldType.STRING)),
                fieldWithPath("items[].translators").type(JsonFieldType.ARRAY).description("옮긴이 이름 목록")
                        .attributes(key("itemsType").value(JsonFieldType.STRING)),
                fieldWithPath("items[].publisher").type(JsonFieldType.STRING).description("출판사"),
                fieldWithPath("items[].category").type(JsonFieldType.STRING).description("도서 카테고리"),
                fieldWithPath("items[].publishedDate").type(JsonFieldType.STRING).description("출판일"),
                fieldWithPath("items[].totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수").optional(),
                fieldWithPath("items[].commentCount").type(JsonFieldType.NUMBER).description("감상과 답글 수"),
                fieldWithPath("items[].status").type(JsonFieldType.STRING)
                        .description("서재 소유자의 읽기 상태. 독서 기록이 초기화된 항목이면 null")
                        .attributes(key("nullable").value(true)),
                fieldWithPath("items[].currentPage").type(JsonFieldType.NUMBER)
                        .description("서재 소유자의 현재 읽은 페이지. 독서 기록이 초기화된 항목이면 null")
                        .attributes(key("nullable").value(true)),
                fieldWithPath("items[].rating").type(JsonFieldType.NUMBER)
                        .description("서재 소유자가 부여한 별점").optional()
        };
    }

    private FieldDescriptor[] addLibraryItemRequestFields() {
        return new FieldDescriptor[]{
                fieldWithPath("isbn13").type(JsonFieldType.STRING).description("추가할 도서의 ISBN-13"),
                fieldWithPath("status").type(JsonFieldType.STRING).description("초기 읽기 상태"),
                fieldWithPath("totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수").optional()
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
                parameterWithName("isbn13").description("지금 별점을 주려는 도서의 ISBN-13"),
                parameterWithName("criterion").description("0.1부터 5.0까지 0.1 단위의 비교 기준 별점")
        };
    }

    private ParameterDescriptorWithType[] ratingComparisonResourceQueryParameters() {
        return new ParameterDescriptorWithType[]{
                ResourceDocumentation.parameterWithName("isbn13").type(SimpleType.STRING)
                        .description("지금 별점을 주려는 도서의 ISBN-13"),
                ResourceDocumentation.parameterWithName("criterion").type(SimpleType.NUMBER)
                        .description("0.1부터 5.0까지 0.1 단위의 비교 기준 별점")
        };
    }

    private ParameterDescriptorWithType[] noPathParameters() {
        return new ParameterDescriptorWithType[0];
    }

    private ParameterDescriptorWithType[] noQueryParameters() {
        return new ParameterDescriptorWithType[0];
    }

    private FieldDescriptor[] ratingComparisonResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("lower").type(JsonFieldType.OBJECT).description("기준보다 낮은 별점 중 가장 가까운 도서")
                        .optional().attributes(key("nullable").value(true)),
                fieldWithPath("lower.bookId").type(JsonFieldType.NUMBER).description("도서 ID"),
                fieldWithPath("lower.isbn13").type(JsonFieldType.STRING).description("ISBN-13"),
                fieldWithPath("lower.title").type(JsonFieldType.STRING).description("도서 제목"),
                fieldWithPath("lower.coverImageUrl").type(JsonFieldType.STRING).description("표지 이미지 URL"),
                fieldWithPath("lower.authors").type(JsonFieldType.ARRAY).description("저자 이름 목록")
                        .attributes(key("itemsType").value(JsonFieldType.STRING)),
                fieldWithPath("lower.myRating").type(JsonFieldType.NUMBER).description("내 별점"),
                fieldWithPath("lower.ratingUpdatedAt").type(JsonFieldType.STRING).description("별점을 남긴 시각"),
                fieldWithPath("current").type(JsonFieldType.OBJECT)
                        .description("기준 별점과 같은 별점 중 가장 최근에 별점을 준 도서. 없으면 null")
                        .optional().attributes(key("nullable").value(true)),
                fieldWithPath("current.bookId").type(JsonFieldType.NUMBER).description("도서 ID"),
                fieldWithPath("current.isbn13").type(JsonFieldType.STRING).description("ISBN-13"),
                fieldWithPath("current.title").type(JsonFieldType.STRING).description("도서 제목"),
                fieldWithPath("current.coverImageUrl").type(JsonFieldType.STRING).description("표지 이미지 URL"),
                fieldWithPath("current.authors").type(JsonFieldType.ARRAY).description("저자 이름 목록")
                        .attributes(key("itemsType").value(JsonFieldType.STRING)),
                fieldWithPath("current.myRating").type(JsonFieldType.NUMBER).description("내 별점"),
                fieldWithPath("current.ratingUpdatedAt").type(JsonFieldType.STRING).description("별점을 남긴 시각"),
                fieldWithPath("higher").type(JsonFieldType.OBJECT).description("기준보다 높은 별점 중 가장 가까운 도서")
                        .optional().attributes(key("nullable").value(true)),
                fieldWithPath("higher.bookId").type(JsonFieldType.NUMBER).description("도서 ID"),
                fieldWithPath("higher.isbn13").type(JsonFieldType.STRING).description("ISBN-13"),
                fieldWithPath("higher.title").type(JsonFieldType.STRING).description("도서 제목"),
                fieldWithPath("higher.coverImageUrl").type(JsonFieldType.STRING).description("표지 이미지 URL"),
                fieldWithPath("higher.authors").type(JsonFieldType.ARRAY).description("저자 이름 목록")
                        .attributes(key("itemsType").value(JsonFieldType.STRING)),
                fieldWithPath("higher.myRating").type(JsonFieldType.NUMBER).description("내 별점"),
                fieldWithPath("higher.ratingUpdatedAt").type(JsonFieldType.STRING).description("별점을 남긴 시각")
        };
    }

    private FieldDescriptor[] nullableRatingComparisonResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("lower").type(JsonFieldType.OBJECT)
                        .description("기준보다 낮은 별점 중 가장 가까운 도서. 없으면 null")
                        .optional().attributes(key("nullable").value(true)),
                fieldWithPath("current").type(JsonFieldType.OBJECT)
                        .description("기준 별점과 같은 별점 중 가장 최근에 별점을 준 도서. 없으면 null")
                        .optional().attributes(key("nullable").value(true)),
                fieldWithPath("higher").type(JsonFieldType.OBJECT)
                        .description("기준보다 높은 별점 중 가장 가까운 도서. 없으면 null")
                        .optional().attributes(key("nullable").value(true))
        };
    }

    private ParameterDescriptor bookIdPathParameter() {
        return RequestDocumentation.parameterWithName("bookId")
                .description("도서 ID");
    }

    private ParameterDescriptor memberIdPathParameter() {
        return RequestDocumentation.parameterWithName("memberId")
                .description("공개 서재를 조회할 회원 ID");
    }

    private ParameterDescriptor libraryIdPathParameter() {
        return RequestDocumentation.parameterWithName("libraryId")
                .description("도서를 담을 책장 ID");
    }

    private ParameterDescriptor libraryRenamePathParameter() {
        return RequestDocumentation.parameterWithName("libraryId")
                .description("이름을 변경할 책장 ID");
    }

    private ParameterDescriptor libraryDeletePathParameter() {
        return RequestDocumentation.parameterWithName("libraryId")
                .description("삭제할 책장 ID");
    }

    private ParameterDescriptorWithType[] libraryIdResourcePathParameters() {
        return new ParameterDescriptorWithType[]{
                ResourceDocumentation.parameterWithName("libraryId")
                        .type(SimpleType.INTEGER)
                        .description("도서를 담을 책장 ID")
        };
    }

    private ParameterDescriptorWithType[] libraryRenameResourcePathParameters() {
        return new ParameterDescriptorWithType[]{
                ResourceDocumentation.parameterWithName("libraryId")
                        .type(SimpleType.INTEGER)
                        .description("이름을 변경할 책장 ID")
        };
    }

    private ParameterDescriptorWithType[] libraryDeleteResourcePathParameters() {
        return new ParameterDescriptorWithType[]{
                ResourceDocumentation.parameterWithName("libraryId")
                        .type(SimpleType.INTEGER)
                        .description("삭제할 책장 ID")
        };
    }

    private ParameterDescriptorWithType[] libraryBookDeleteResourcePathParameters() {
        return new ParameterDescriptorWithType[]{
                ResourceDocumentation.parameterWithName("libraryId")
                        .type(SimpleType.INTEGER)
                        .description("도서를 꺼낼 책장 ID"),
                bookIdResourcePathParameter()
        };
    }

    private ParameterDescriptorWithType[] memberIdResourcePathParameters() {
        return new ParameterDescriptorWithType[]{
                ResourceDocumentation.parameterWithName("memberId")
                        .type(SimpleType.INTEGER)
                        .description("공개 서재를 조회할 회원 ID")
        };
    }

    private ParameterDescriptorWithType bookIdResourcePathParameter() {
        return ResourceDocumentation.parameterWithName("bookId")
                .type(SimpleType.INTEGER)
                .description("도서 ID");
    }

    private ParameterDescriptorWithType[] bookIdResourcePathParameters() {
        return new ParameterDescriptorWithType[]{bookIdResourcePathParameter()};
    }

    private LibraryItemResponse ratingOnlyResponse() {
        LibraryItemResponse original = libraryItemResponse();
        return new LibraryItemResponse(
                original.bookId(),
                original.isbn13(),
                original.title(),
                original.coverImageUrl(),
                original.spineImageUrl(),
                original.backImageUrl(),
                original.authors(),
                original.translators(),
                original.publisher(),
                original.category(),
                original.publishedDate(),
                original.totalPages(),
                original.commentCount(),
                null,
                null,
                original.rating(),
                original.addedAt(),
                null
        );
    }

    private LibraryItemResponse libraryItemResponse() {
        return libraryItemResponse(
                "https://image.yes24.com/goods/118578901/side",
                "https://image.yes24.com/goods/118578901/back"
        );
    }

    private LibraryResponse libraryResponse(String name) {
        return new LibraryResponse(7L, name, false, 0, List.of());
    }

    private LibraryItemResponse libraryItemResponseWithoutImages() {
        return libraryItemResponse(null, null);
    }

    private LibraryItemResponse libraryItemResponse(String spineImageUrl, String backImageUrl) {
        return new LibraryItemResponse(
                BOOK_ID,
                ISBN13,
                "채식주의자",
                "https://image.yes24.com/goods/118578901/xL",
                spineImageUrl,
                backImageUrl,
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

    private RatingComparisonBookResponse comparisonBook(
            Long bookId,
            String isbn13,
            String title,
            String author,
            String rating,
            Instant ratingUpdatedAt
    ) {
        return new RatingComparisonBookResponse(
                bookId,
                isbn13,
                title,
                "https://example.com/books/" + bookId + ".jpg",
                List.of(author),
                new BigDecimal(rating),
                ratingUpdatedAt
        );
    }

}
