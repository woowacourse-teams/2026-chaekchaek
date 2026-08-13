package com.chaekchaek.auth.token.access;

import com.chaekchaek.member.domain.Member;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
public class AccessTokenProvider {

    private static final String MEMBER_MUST_BE_SAVED_ERROR_MESSAGE =
            "[ERROR] Access Token을 발급할 회원은 저장된 상태여야 합니다";

    private final JwtEncoder jwtEncoder;
    private final AccessTokenProperties properties;
    private final Clock clock;

    AccessTokenProvider(JwtEncoder jwtEncoder, AccessTokenProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    public String issue(Member member) {
        if (member.getId() == null) {
            throw new IllegalArgumentException(MEMBER_MUST_BE_SAVED_ERROR_MESSAGE);
        }

        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.expiration());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(member.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("memberType", member.getType().name())
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }


}
