package com.chaekchaek.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chaekchaek.actor.domain.Actor;
import com.chaekchaek.auth.token.guest.GuestTokenService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OAuthGuestContextServiceTest {

    @Mock
    private GuestTokenService guestTokenService;

    @Test
    @DisplayName("유효한 게스트 토큰의 Actor ID를 OAuth 세션에 저장한다")
    void should_RememberGuestActorIdInSession() {
        OAuthGuestContextService service = new OAuthGuestContextService(guestTokenService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        Actor actor = Actor.guest(
                "a".repeat(64),
                "게스트 참새",
                LocalDateTime.of(2026, 9, 1, 0, 0),
                LocalDateTime.of(2026, 12, 1, 0, 0));
        ReflectionTestUtils.setField(actor, "id", 7L);
        when(guestTokenService.findUsableActor("guest-token")).thenReturn(actor);

        service.rememberGuestActor(request, "guest-token");

        assertThat(service.findGuestActorId(request)).contains(7L);
        verify(guestTokenService).findUsableActor("guest-token");
    }

    @Test
    @DisplayName("OAuth 로그인 컨텍스트를 조회한 뒤 삭제할 수 있다")
    void should_ClearGuestActorIdFromSession() {
        OAuthGuestContextService service = new OAuthGuestContextService(guestTokenService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        Actor actor = Actor.guest(
                "a".repeat(64),
                "게스트 참새",
                LocalDateTime.of(2026, 9, 1, 0, 0),
                LocalDateTime.of(2026, 12, 1, 0, 0));
        ReflectionTestUtils.setField(actor, "id", 7L);
        when(guestTokenService.findUsableActor("guest-token")).thenReturn(actor);

        service.rememberGuestActor(request, "guest-token");
        service.clear(request);

        assertThat(service.findGuestActorId(request)).isEmpty();
    }
}
