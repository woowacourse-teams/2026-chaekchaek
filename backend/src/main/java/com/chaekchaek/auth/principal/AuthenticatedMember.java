package com.chaekchaek.auth.principal;

import com.chaekchaek.member.domain.Member;
import java.util.Collection;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public class AuthenticatedMember implements OidcUser {

    private static final String MEMBER_MUST_BE_SAVED_ERROR_MESSAGE =
            "[ERROR] 인증 주체가 될 회원은 저장된 상태여야 합니다";

    private final Long memberId;
    private final OidcUser delegate;

    public AuthenticatedMember(Long memberId, OidcUser delegate) {
        this.memberId = memberId;
        this.delegate = delegate;
    }

    public static AuthenticatedMember of(Member member, OidcUser oidcUser) {
        if (member.getId() == null) {
            throw new IllegalArgumentException(MEMBER_MUST_BE_SAVED_ERROR_MESSAGE);
        }

        return new AuthenticatedMember(
                member.getId(),
                oidcUser
        );
    }

    public Long getMemberId() {
        return memberId;
    }

    @Override
    public Map<String, Object> getClaims() {
        return delegate.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return delegate.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return delegate.getIdToken();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return delegate.getAuthorities();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }
}
