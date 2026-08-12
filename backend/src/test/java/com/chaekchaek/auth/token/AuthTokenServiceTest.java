package com.chaekchaek.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AuthTokenServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AccessTokenProvider accessTokenProvider;

    @Mock
    private RefreshTokenProvider refreshTokenProvider;

    @InjectMocks
    private AuthTokenService authTokenService;

    @Test
    @DisplayName("회원에게 AccessToken과 RefreshToken을 발급한다")
    void should_IssueAccessTokenAndRefreshToken_When_Member() {
        // given
        Member member = mock(Member.class);

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));
        when(accessTokenProvider.issue(member))
                .thenReturn("access-token");

        IssuedRefreshToken refreshToken =
                new IssuedRefreshToken(
                        "refresh-token",
                        LocalDateTime.of(2026, 8, 26, 12, 0)
                );

        when(refreshTokenProvider.issue(member))
                .thenReturn(refreshToken);

        // when
        IssuedTokens result = authTokenService.issue(1L);

        // then
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isSameAs(refreshToken);

        verify(accessTokenProvider).issue(member);
        verify(refreshTokenProvider).issue(member);
    }
}
