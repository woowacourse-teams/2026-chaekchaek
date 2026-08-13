package com.chaekchaek.common.auth;

/**
 * Provides the authenticated member identifier from the current request.
 *
 * <p>The authentication module will implement this interface using the JWT subject.</p>
 */
public interface CurrentMemberIdProvider {

    long getCurrentMemberId();
}
