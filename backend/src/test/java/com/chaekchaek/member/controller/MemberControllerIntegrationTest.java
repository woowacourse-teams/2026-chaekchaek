package com.chaekchaek.member.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
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
            fieldWithPath("profileImageUrl").type(JsonFieldType.STRING).description("프로필 이미지 URL"),
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
        Member member = memberRepository.save(
                Member.create(
                        "우아한 달빛 참새",
                        "exUrl",
                        LocalDateTime.of(2026, 8, 13, 12, 0)
                )
        );

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
                .andExpect(jsonPath("$.nickname").doesNotExist())
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
                "우아한 달빛 참새", null, LocalDateTime.now()));
        Cookie cookie = accessTokenCookie(member);

        mockMvc.perform(patch("/api/v1/members/me/nickname")
                        .with(csrf())
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"책책이\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("책책이"))
                .andExpect(jsonPath("$.anonymousNickname").value("우아한 달빛 참새"))
                .andExpect(jsonPath("$.displayAnonymous").value(true));
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
                        .with(csrf())
                        .cookie(accessTokenCookie(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"책책이\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NICKNAME_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("공개 닉네임 없이 익명 상태 해제를 거부한다")
    void should_RejectDisableAnonymityWithoutNickname() throws Exception {
        Member member = memberRepository.save(Member.create(
                "우아한 달빛 참새", null, LocalDateTime.now()));

        mockMvc.perform(patch("/api/v1/members/me/anonymity")
                        .with(csrf())
                        .cookie(accessTokenCookie(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayAnonymous\":false}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("NICKNAME_REQUIRED"));
    }

    @Test
    @DisplayName("공개 닉네임 설정 후 익명 상태를 해제한다")
    void should_DisableAnonymityAfterNicknameIsSet() throws Exception {
        Member member = Member.create("우아한 달빛 참새", null, LocalDateTime.now());
        member.updateNickname("책책이");
        memberRepository.save(member);

        mockMvc.perform(patch("/api/v1/members/me/anonymity")
                        .with(csrf())
                        .cookie(accessTokenCookie(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayAnonymous\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("책책이"))
                .andExpect(jsonPath("$.displayAnonymous").value(false));
    }

    @Test
    @DisplayName("회원을 탈퇴 처리하고 인증 쿠키를 삭제한다")
    void should_WithdrawMember() throws Exception {
        Member member = Member.create("우아한 달빛 참새", "profile", LocalDateTime.now());
        member.updateNickname("책책이");
        memberRepository.save(member);

        var result = mockMvc.perform(delete("/api/v1/members/me")
                        .with(csrf())
                        .cookie(accessTokenCookie(member)))
                .andExpect(status().isNoContent())
                .andExpect(header().exists("Set-Cookie"))
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
}
