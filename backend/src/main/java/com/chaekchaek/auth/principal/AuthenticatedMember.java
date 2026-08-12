package com.chaekchaek.auth.principal;

import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.domain.MemberType;
import java.util.Collection;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public class AuthenticatedMember implements OidcUser {

    private static final String SAVED_MEMBER_CAN_BE_AUTHENTICATION_ERROR_MESSAGE =
            "저장된 회원만 인증 주체로 만들 수 있습니다.";

    private final Long memberId;
    private final MemberType memberType;
    private final OidcUser delegate;

    public AuthenticatedMember(Long memberId, MemberType memberType, OidcUser delegate) {
        this.memberId = memberId;
        this.memberType = memberType;
        this.delegate = delegate;
    }

    public static AuthenticatedMember of(Member member, OidcUser oidcUser) {
        if (member.getId() == null) {
            throw new IllegalArgumentException(SAVED_MEMBER_CAN_BE_AUTHENTICATION_ERROR_MESSAGE);
        }

        return new AuthenticatedMember(
                member.getId(),
                member.getType(),
                oidcUser
        );
    }

    public Long getMemberId() {
        return memberId;
    }

    public MemberType getMemberType() {
        return memberType;
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
