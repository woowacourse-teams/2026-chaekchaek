package com.chaekchaek.auth.oauth;

import com.chaekchaek.auth.token.guest.GuestTokenService;
import com.chaekchaek.actor.domain.Actor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuthGuestContextService {

    private static final String GUEST_ACTOR_ID_ATTRIBUTE =
            OAuthGuestContextService.class.getName() + ".GUEST_ACTOR_ID";

    private final GuestTokenService guestTokenService;

    public void rememberGuestActor(HttpServletRequest request, String guestToken) {
        Actor actor = guestTokenService.findUsableActor(guestToken);
        request.getSession(true).setAttribute(GUEST_ACTOR_ID_ATTRIBUTE, actor.getId());
    }

    public Optional<Long> findGuestActorId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }

        Object value = session.getAttribute(GUEST_ACTOR_ID_ATTRIBUTE);
        if (value instanceof Number actorId) {
            return Optional.of(actorId.longValue());
        }
        return Optional.empty();
    }

    public void clear(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(GUEST_ACTOR_ID_ATTRIBUTE);
        }
    }
}
