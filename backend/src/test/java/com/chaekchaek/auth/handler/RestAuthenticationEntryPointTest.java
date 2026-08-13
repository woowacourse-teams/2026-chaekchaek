package com.chaekchaek.auth.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

import com.chaekchaek.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import tools.jackson.databind.json.JsonMapper;

class RestAuthenticationEntryPointTest {

    private final RestAuthenticationEntryPoint entryPoint =
            new RestAuthenticationEntryPoint(new JsonMapper());

    @Test
    @DisplayName("인증에 실패하면 공통 형식의 401 응답을 반환한다")
    void should_ReturnUnauthorizedResponse_When_AuthenticationFails()
            throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException exception =
                mock(AuthenticationException.class);

        // when
        entryPoint.commence(request, response, exception);

        // then
        assertAll(
                () -> assertThat(response.getStatus()).isEqualTo(401),
                () -> assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE),
                () -> assertThat(response.getContentAsString()).contains("\"code\":\"UNAUTHORIZED\""),
                () -> assertThat(response.getContentAsString()).contains(ErrorCode.UNAUTHORIZED.getMessage())
        );
    }
}
