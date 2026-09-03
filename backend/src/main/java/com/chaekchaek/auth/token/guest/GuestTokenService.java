package com.chaekchaek.auth.token.guest;

import com.chaekchaek.actor.domain.Actor;
import com.chaekchaek.actor.repository.ActorRepository;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
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
        String token = generateToken();
        String nickname = nicknameGenerator.generate();
        LocalDateTime issuedAt = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        LocalDateTime expiresAt = issuedAt.plus(properties.expiration());
        actorRepository.save(Actor.guest(tokenHasher.hash(token), nickname, issuedAt, expiresAt));
        return new IssuedGuestToken(token, nickname, expiresAt);
    }

    @Transactional(readOnly = true)
    public Actor findUsableActor(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_GUEST_TOKEN);
        }

        Actor actor = actorRepository.findByGuestTokenHash(tokenHasher.hash(token))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_GUEST_TOKEN));
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        if (!actor.isUsableGuestAt(now)) {
            throw new BusinessException(ErrorCode.UNUSABLE_GUEST_TOKEN);
        }
        return actor;
    }

    @Transactional
    public IssuedGuestToken refresh(String currentToken) {
        if (currentToken == null || currentToken.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_GUEST_TOKEN);
        }

        String currentTokenHash = tokenHasher.hash(currentToken);
        Actor foundActor = actorRepository.findByGuestTokenHash(currentTokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_GUEST_TOKEN));
        Actor actor = actorRepository.findByIdForUpdate(foundActor.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_GUEST_TOKEN));
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        if (!currentTokenHash.equals(actor.getGuestTokenHash())) {
            throw new BusinessException(ErrorCode.INVALID_GUEST_TOKEN);
        }
        if (!actor.isUsableGuestAt(now)) {
            throw new BusinessException(ErrorCode.UNUSABLE_GUEST_TOKEN);
        }
        if (!actor.isRefreshableGuestAt(now, properties.refreshWindow())) {
            throw new BusinessException(ErrorCode.GUEST_TOKEN_REFRESH_NOT_ALLOWED);
        }

        String newToken = generateToken();
        LocalDateTime expiresAt = now.plus(properties.expiration());
        actor.refreshGuestToken(tokenHasher.hash(newToken), now, expiresAt);
        return new IssuedGuestToken(newToken, actor.getGuestNickname(), expiresAt);
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
