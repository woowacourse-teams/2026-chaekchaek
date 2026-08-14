package com.chaekchaek.common.auth;

import java.util.OptionalLong;

/**
 * Provides the authenticated member identifier from the current request.
 *
 * <p>The authentication module will implement this interface using the JWT subject.</p>
 */
public interface CurrentMemberIdProvider {

    long getCurrentMemberId();

    /**
     * Returns the current member when the request is authenticated.
     * Public endpoints use this method so they can still provide personalized fields when possible.
     */
    default OptionalLong findCurrentMemberId() {
        return OptionalLong.empty();
    }
}
