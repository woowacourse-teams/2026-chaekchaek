package com.chaekchaek.member.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.auth.token.access.AccessTokenProvider;
import com.chaekchaek.auth.token.cookie.AuthCookieProvider;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@ActiveProfiles("test")
public class MemberControllerIntegrationTest {

    private static final String MEMBER_TAG = "회원";
    private static final FieldDescriptor[] MEMBER_RESPONSE_FIELDS = {
            fieldWithPath("memberId").type(JsonFieldType.NUMBER).description("회원 ID"),
            fieldWithPath("nickname").type(JsonFieldType.STRING).description("사용자가 설정한 공개 닉네임").optional(),
            fieldWithPath("anonymousNickname").type(JsonFieldType.STRING).description("가입 시 생성된 익명 닉네임"),
            fieldWithPath("profileImageUrl").type(JsonFieldType.STRING).description("프로필 이미지 URL").optional(),
            fieldWithPath("displayAnonymous").type(JsonFieldType.BOOLEAN)
                    .description("감상 작성 시 익명 표시를 기본으로 사용하는지 여부"),
            fieldWithPath("accountStatus").type(JsonFieldType.STRING).description("계정 상태")
    };
    private static final FieldDescriptor[] PROBLEM_DETAIL_FIELDS = {
            fieldWithPath("type").type(JsonFieldType.STRING)
                    .description("현재 about:blank로 고정되는 문제 유형 URI"),
            fieldWithPath("title").type(JsonFieldType.STRING).description("HTTP 상태 설명"),
            fieldWithPath("status").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
            fieldWithPath("detail").type(JsonFieldType.STRING)
                    .description("오류 상세 메시지. 클라이언트 분기에는 사용하지 않는다"),
            fieldWithPath("instance").type(JsonFieldType.STRING)
                    .description("오류가 발생한 요청 경로. 인증 진입점 응답에서는 생략될 수 있다")
                    .optional(),
            fieldWithPath("code").type(JsonFieldType.STRING)
                    .description("클라이언트 오류 분기에 사용하는 애플리케이션 오류 코드")
    };
    private static final FieldDescriptor[] UPDATE_NICKNAME_REQUEST_FIELDS = {
            fieldWithPath("nickname").type(JsonFieldType.STRING)
                    .description("설정할 공개 닉네임. 공백일 수 없으며 최대 100자")
    };
    private static final FieldDescriptor[] UPDATE_ANONYMITY_REQUEST_FIELDS = {
            fieldWithPath("displayAnonymous").type(JsonFieldType.BOOLEAN)
                    .description("변경할 익명 여부. false는 공개 닉네임 설정 후에만 가능")
    };

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AccessTokenProvider accessTokenProvider;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("AccessToken 쿠키로 내 정보를 조회한다")
    void should_GetMyInfo() throws Exception {
        // given
        Member member = Member.create(
                "우아한 달빛 참새",
                "exUrl",
                LocalDateTime.of(2026, 8, 13, 12, 0)
        );
        member.updateNickname("책책이");
        memberRepository.save(member);

        String accessToken = accessTokenProvider.issue(member);

        Cookie cookie = new Cookie(
                AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME,
                accessToken
        );

        // when && then
        mockMvc.perform(get("/api/v1/members/me")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(member.getId()))
                .andExpect(jsonPath("$.nickname").value("책책이"))
                .andExpect(jsonPath("$.anonymousNickname").value("우아한 달빛 참새"))
                .andExpect(jsonPath("$.profileImageUrl")
                        .value("exUrl"))
                .andExpect(jsonPath("$.displayAnonymous").value(true))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"))
                .andDo(document(
                        "member-me",
                        responseFields(MEMBER_RESPONSE_FIELDS),
                        resource(ResourceSnippetParameters.builder()
                                .summary("내 정보 조회")
                                .description("웹에서는 access_token 쿠키로, 모바일에서는 Authorization Bearer 헤더로 인증한 사용자의 정보를 조회한다")
                                .tag(MEMBER_TAG)
                                .responseSchema(Schema.schema("MemberResponse"))
                                .responseFields(MEMBER_RESPONSE_FIELDS)
                                .build())));
    }

    @Test
    @DisplayName("AccessToken 쿠키가 없으면 내 정보 조회를 거부한다")
    void should_RejectGetMyInfo_WithoutAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andDo(document(
                        "member-me-unauthorized",
                        responseFields(PROBLEM_DETAIL_FIELDS),
                        resource(ResourceSnippetParameters.builder()
                                .summary("내 정보 조회")
                                .description(
                                        "Access Token이 없거나 유효하지 않으면 인증 오류를 반환한다. 클라이언트는 code를 기준으로 로그인 또는 토큰 재발급 흐름을 선택한다")
                                .tag(MEMBER_TAG)
                                .responseSchema(Schema.schema("ProblemDetail"))
                                .responseFields(PROBLEM_DETAIL_FIELDS)
                                .build())));
    }

    @Test
    @DisplayName("변조된 Access Token으로 내 정보를 조회를 요청하면 거부한다")
    void should_RejectGetMyInfo_WithTamperedAccessToken() throws Exception {
        Cookie cookie = new Cookie(
                AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME,
                "invalid.access.token"
        );

        mockMvc.perform(get("/api/v1/members/me")
                        .cookie(cookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("공개 닉네임을 설정한다")
    void should_UpdateNickname() throws Exception {
        Member member = memberRepository.save(Member.create(
                "우아한 달빛 참새", "exUrl", LocalDateTime.now()));
        Cookie cookie = accessTokenCookie(member);

        mockMvc.perform(patch("/api/v1/members/me/nickname")
                        .with(csrf().asHeader())
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"책책이\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("책책이"))
                .andExpect(jsonPath("$.anonymousNickname").value("우아한 달빛 참새"))
                .andExpect(jsonPath("$.displayAnonymous").value(true))
                .andDo(document(
                        "member-nickname-update",
                        requestFields(UPDATE_NICKNAME_REQUEST_FIELDS),
                        responseFields(MEMBER_RESPONSE_FIELDS),
                        resource(ResourceSnippetParameters.builder()
                                .summary("닉네임 설정")
                                .description("중복되지 않은 공개 닉네임을 설정한다. 설정 후에도 익명 여부는 변경되지 않는다")
                                .tag(MEMBER_TAG)
                                .requestSchema(Schema.schema("UpdateNicknameRequest"))
                                .requestFields(UPDATE_NICKNAME_REQUEST_FIELDS)
                                .responseSchema(Schema.schema("MemberResponse"))
                                .responseFields(MEMBER_RESPONSE_FIELDS)
                                .build())));
    }

    @Test
    @DisplayName("중복된 공개 닉네임 설정을 거부한다")
    void should_RejectDuplicatedNickname() throws Exception {
        Member existingMember = Member.create("다정한 별빛 참새", null, LocalDateTime.now());
        existingMember.updateNickname("책책이");
        memberRepository.save(existingMember);
        Member member = memberRepository.save(Member.create(
                "우아한 달빛 참새", null, LocalDateTime.now()));

        mockMvc.perform(patch("/api/v1/members/me/nickname")
                        .with(csrf().asHeader())
                        .cookie(accessTokenCookie(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"책책이\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NICKNAME_ALREADY_EXISTS"))
                .andDo(problemDetailDocument(
                        "member-nickname-update-conflict", "닉네임 설정",
                        "이미 사용 중인 공개 닉네임이면 충돌 오류를 반환한다"));
    }

    @Test
    @DisplayName("공개 닉네임 없이 익명 상태 해제를 거부한다")
    void should_RejectDisableAnonymityWithoutNickname() throws Exception {
        Member member = memberRepository.save(Member.create(
                "우아한 달빛 참새", null, LocalDateTime.now()));

        mockMvc.perform(patch("/api/v1/members/me/anonymity")
                        .with(csrf().asHeader())
                        .cookie(accessTokenCookie(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayAnonymous\":false}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("NICKNAME_REQUIRED"))
                .andDo(problemDetailDocument(
                        "member-anonymity-update-nickname-required", "익명 여부 수정",
                        "공개 닉네임을 설정하지 않고 익명을 해제하면 처리 불가 오류를 반환한다"));
    }

    @Test
    @DisplayName("공개 닉네임 설정 후 익명 상태를 해제한다")
    void should_DisableAnonymityAfterNicknameIsSet() throws Exception {
        Member member = Member.create("우아한 달빛 참새", "exUrl", LocalDateTime.now());
        member.updateNickname("책책이");
        memberRepository.save(member);

        mockMvc.perform(patch("/api/v1/members/me/anonymity")
                        .with(csrf().asHeader())
                        .cookie(accessTokenCookie(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayAnonymous\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("책책이"))
                .andExpect(jsonPath("$.displayAnonymous").value(false))
                .andDo(document(
                        "member-anonymity-update",
                        requestFields(UPDATE_ANONYMITY_REQUEST_FIELDS),
                        responseFields(MEMBER_RESPONSE_FIELDS),
                        resource(ResourceSnippetParameters.builder()
                                .summary("익명 여부 수정")
                                .description("이후 작성할 감상과 댓글에 적용할 익명 여부를 수정한다")
                                .tag(MEMBER_TAG)
                                .requestSchema(Schema.schema("UpdateAnonymityRequest"))
                                .requestFields(UPDATE_ANONYMITY_REQUEST_FIELDS)
                                .responseSchema(Schema.schema("MemberResponse"))
                                .responseFields(MEMBER_RESPONSE_FIELDS)
                                .build())));
    }

    @Test
    @DisplayName("공백 닉네임 설정 요청을 거부한다")
    void should_RejectBlankNickname() throws Exception {
        Member member = memberRepository.save(Member.create(
                "우아한 달빛 참새", null, LocalDateTime.now()));

        mockMvc.perform(patch("/api/v1/members/me/nickname")
                        .with(csrf().asHeader())
                        .cookie(accessTokenCookie(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andDo(problemDetailDocument(
                        "member-nickname-update-invalid", "닉네임 설정",
                        "닉네임이 공백이거나 100자를 초과하면 잘못된 요청 오류를 반환한다"));
    }

    @Test
    @DisplayName("인증 없이 닉네임 설정을 요청하면 거부한다")
    void should_RejectUpdateNicknameWithoutAuthentication() throws Exception {
        mockMvc.perform(patch("/api/v1/members/me/nickname")
                        .with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"책책이\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andDo(problemDetailDocument(
                        "member-nickname-update-unauthorized", "닉네임 설정",
                        "유효한 Access Token이 없으면 인증 오류를 반환한다"));
    }

    @Test
    @DisplayName("익명 여부가 없는 요청을 거부한다")
    void should_RejectMissingAnonymity() throws Exception {
        Member member = memberRepository.save(Member.create(
                "우아한 달빛 참새", null, LocalDateTime.now()));

        mockMvc.perform(patch("/api/v1/members/me/anonymity")
                        .with(csrf().asHeader())
                        .cookie(accessTokenCookie(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andDo(problemDetailDocument(
                        "member-anonymity-update-invalid", "익명 여부 수정",
                        "displayAnonymous가 없으면 잘못된 요청 오류를 반환한다"));
    }

    @Test
    @DisplayName("인증 없이 익명 여부 수정을 요청하면 거부한다")
    void should_RejectUpdateAnonymityWithoutAuthentication() throws Exception {
        mockMvc.perform(patch("/api/v1/members/me/anonymity")
                        .with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayAnonymous\":true}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andDo(problemDetailDocument(
                        "member-anonymity-update-unauthorized", "익명 여부 수정",
                        "유효한 Access Token이 없으면 인증 오류를 반환한다"));
    }

    @Test
    @DisplayName("회원을 탈퇴 처리하고 인증 쿠키를 삭제한다")
    void should_WithdrawMember() throws Exception {
        Member member = Member.create("우아한 달빛 참새", "profile", LocalDateTime.now());
        member.updateNickname("책책이");
        memberRepository.save(member);

        var result = mockMvc.perform(delete("/api/v1/members/me")
                        .with(csrf().asHeader())
                        .cookie(accessTokenCookie(member)))
                .andExpect(status().isNoContent())
                .andExpect(header().exists("Set-Cookie"))
                .andDo(document(
                        "member-withdraw",
                        responseHeaders(headerWithName(HttpHeaders.SET_COOKIE)
                                .description("만료된 access_token 및 refresh_token 쿠키")),
                        resource(ResourceSnippetParameters.builder()
                                .summary("회원 탈퇴")
                                .description("회원을 탈퇴 처리하고 인증 토큰을 폐기한다. 기존 감상과 댓글은 유지된다")
                                .tag(MEMBER_TAG)
                                .responseHeaders(com.epages.restdocs.apispec.ResourceDocumentation
                                        .headerWithName(HttpHeaders.SET_COOKIE)
                                        .description("만료된 인증 쿠키"))
                                .build())))
                .andReturn();

        assertThat(result.getResponse().getHeaders("Set-Cookie"))
                .anyMatch(value -> value.contains("access_token=;") && value.contains("Max-Age=0"))
                .anyMatch(value -> value.contains("refresh_token=;") && value.contains("Max-Age=0"));

        Member withdrawnMember = memberRepository.findById(member.getId()).orElseThrow();
        assertAll(
                () -> assertThat(withdrawnMember.getAccountStatus().name()).isEqualTo("WITHDRAWN"),
                () -> assertThat(withdrawnMember.getWithdrawnAt()).isNotNull(),
                () -> assertThat(withdrawnMember.getNickname()).isNull(),
                () -> assertThat(withdrawnMember.getProfileImageUrl()).isNull()
        );
    }

    private Cookie accessTokenCookie(Member member) {
        return new Cookie(
                AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME,
                accessTokenProvider.issue(member)
        );
    }

    private org.springframework.restdocs.mockmvc.RestDocumentationResultHandler problemDetailDocument(
            String identifier, String summary, String description
    ) {
        return document(
                identifier,
                responseFields(PROBLEM_DETAIL_FIELDS),
                resource(ResourceSnippetParameters.builder()
                        .summary(summary)
                        .description(description)
                        .tag(MEMBER_TAG)
                        .responseSchema(Schema.schema("ProblemDetail"))
                        .responseFields(PROBLEM_DETAIL_FIELDS)
                        .build())
        );
    }

    @Test
    @DisplayName("인증 없이 회원 탈퇴를 요청하면 거부한다")
    void should_RejectWithdrawWithoutAuthentication() throws Exception {
        mockMvc.perform(delete("/api/v1/members/me")
                        .with(csrf().asHeader()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andDo(problemDetailDocument(
                        "member-withdraw-unauthorized", "회원 탈퇴",
                        "유효한 Access Token이 없으면 인증 오류를 반환한다"));
    }
}
