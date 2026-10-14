package com.chaekchaek.common.logging;

import com.chaekchaek.common.auth.ActorType;
import com.chaekchaek.common.auth.CurrentActor;
import jakarta.servlet.http.HttpServletRequest;

public final class RequestLogAttributes {

    static final String ACTOR_ID = RequestLogAttributes.class.getName() + ".actorId";
    static final String MEMBER_ID = RequestLogAttributes.class.getName() + ".memberId";
    static final String AUTH_TYPE = RequestLogAttributes.class.getName() + ".authType";

    private static final String ANONYMOUS_AUTH_TYPE = "ANONYMOUS";

    private RequestLogAttributes() {

    }

    public static void recordMember(HttpServletRequest request, long memberId) {
        request.setAttribute(MEMBER_ID, memberId);
        if (request.getAttribute(AUTH_TYPE) == null) {
            request.setAttribute(AUTH_TYPE, ActorType.MEMBER.name());
        }
    }

    public static void recordActor(HttpServletRequest request, CurrentActor actor) {
        request.setAttribute(ACTOR_ID, actor.actorId());
        request.setAttribute(AUTH_TYPE, actor.type().name());
        if (actor.memberId() != null) {
            request.setAttribute(MEMBER_ID, actor.memberId());
        }
    }

    static String authType(HttpServletRequest request) {
        Object authType = request.getAttribute(AUTH_TYPE);
        if (authType == null) {
            return ANONYMOUS_AUTH_TYPE;
        }
        return authType.toString();
    }
}
