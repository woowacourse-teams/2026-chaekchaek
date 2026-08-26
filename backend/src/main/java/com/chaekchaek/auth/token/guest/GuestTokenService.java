package com.chaekchaek.auth.token.guest;

import com.chaekchaek.actor.domain.Actor;
import com.chaekchaek.actor.repository.ActorRepository;
import com.chaekchaek.member.service.NicknameGenerator;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GuestTokenService {

    private static final int TOKEN_BYTES = 32;

    private final ActorRepository actorRepository;
    private final GuestTokenHasher tokenHasher;
    private final GuestTokenProperties properties;
    private final NicknameGenerator nicknameGenerator;
    private final SecureRandom secureRandom;
    private final Clock clock;

    @Transactional
    public IssuedGuestToken issue() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String nickname = nicknameGenerator.generate();
        LocalDateTime issuedAt = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        LocalDateTime expiresAt = issuedAt.plus(properties.expiration());
        actorRepository.save(Actor.guest(tokenHasher.hash(token), nickname, issuedAt, expiresAt));
        return new IssuedGuestToken(token, nickname, expiresAt);
    }
}
