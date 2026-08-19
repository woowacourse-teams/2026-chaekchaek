package com.chaekchaek.auth.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.auth.token.cookie.AuthCookieProvider;
import com.chaekchaek.auth.service.AuthTokenService;
import com.chaekchaek.auth.token.dto.IssuedTokens;
import com.chaekchaek.auth.token.refresh.IssuedRefreshToken;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import java.time.LocalDateTime;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.restdocs.headers.HeaderDescriptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = AuthController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class
)
@AutoConfigureRestDocs
class AuthControllerTest {

    private static final String AUTH_TAG = "인증";
    private static final HeaderDescriptor SET_COOKIE_HEADER = headerWithName(HttpHeaders.SET_COOKIE)
            .description("갱신되거나 삭제된 access_token·refresh_token 쿠키");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthTokenService authTokenService;

    @MockitoBean
    private AuthCookieProvider authCookieProvider;

    @Test
    @DisplayName("웹 Refresh Token 쿠키로 Access Token을 재발급한다")
    void should_ReissueWebTokenCookies_When_RefreshTokenCookieExists() throws Exception {
        // given
        IssuedTokens tokens = new IssuedTokens(
                "new-access-token",
                new IssuedRefreshToken("new-refresh-token", LocalDateTime.now().plusDays(14))
        );
        when(authTokenService.reissue("refresh-token")).thenReturn(tokens);
        when(authCookieProvider.createAccessTokenCookie("new-access-token"))
                .thenReturn(ResponseCookie.from("access_token", "new-access-token").build());
        when(authCookieProvider.createRefreshTokenCookie("new-refresh-token"))
                .thenReturn(ResponseCookie.from("refresh_token", "new-refresh-token").build());

        // when & then
        mockMvc.perform(post("/api/v1/auth/reissue")
                        .cookie(new Cookie("refresh_token", "refresh-token")))
                .andExpect(status().isNoContent())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andDo(document(
                        "web-token-reissue",
                        responseHeaders(SET_COOKIE_HEADER),
                        resource(ResourceSnippetParameters.builder()
                                .summary("웹 토큰 재발급")
                                .description("refresh_token 쿠키를 사용해 새 access_token과 refresh_token 쿠키를 발급한다")
                                .tag(AUTH_TAG)
                                .responseHeaders(SET_COOKIE_HEADER)
                                .build())));
    }

    @Test
    @DisplayName("웹 로그아웃 시 인증 쿠키를 삭제한다")
    void should_DeleteWebTokenCookies_When_LoggingOut() throws Exception {
        // given
        when(authCookieProvider.deleteAccessTokenCookie())
                .thenReturn(ResponseCookie.from("access_token", "").maxAge(0).build());
        when(authCookieProvider.deleteRefreshTokenCookie())
                .thenReturn(ResponseCookie.from("refresh_token", "").maxAge(0).build());

        // when & then
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new Cookie("refresh_token", "refresh-token")))
                .andExpect(status().isNoContent())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andDo(document(
                        "web-logout",
                        responseHeaders(SET_COOKIE_HEADER),
                        resource(ResourceSnippetParameters.builder()
                                .summary("웹 로그아웃")
                                .description("refresh_token 쿠키를 폐기하고 access_token·refresh_token 쿠키를 삭제한다")
                                .tag(AUTH_TAG)
                                .responseHeaders(SET_COOKIE_HEADER)
                                .build())));

        verify(authTokenService).logout("refresh-token");
    }
}
