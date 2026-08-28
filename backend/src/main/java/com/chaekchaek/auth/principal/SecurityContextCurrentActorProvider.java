package com.chaekchaek.auth.principal;

import com.chaekchaek.actor.domain.Actor;
import com.chaekchaek.actor.repository.ActorRepository;
import com.chaekchaek.common.auth.CurrentActor;
import com.chaekchaek.common.auth.CurrentActorProvider;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.auth.token.guest.GuestTokenHasher;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityContextCurrentActorProvider implements CurrentActorProvider {

    public static final String GUEST_TOKEN_HEADER = "X-Guest-Token";

    private final SecurityContextCurrentMemberIdProvider currentMemberIdProvider;
    private final ActorRepository actorRepository;
    private final GuestTokenHasher guestTokenHasher;
    private final HttpServletRequest request;
    private final Clock clock;

    @Override
    public CurrentActor getCurrentActor() {
        return findCurrentActor().orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    @Override
    public Optional<CurrentActor> findCurrentActor() {
        Optional<CurrentActor> memberActor = currentMemberIdProvider.findCurrentMemberId().stream()
                .mapToObj(memberId -> actorRepository.findByMemberId(memberId)
                        .map(this::toMemberActor))
                .findFirst()
                .flatMap(actor -> actor);
        if (memberActor.isPresent()) {
            return memberActor;
        }
        String guestToken = request.getHeader(GUEST_TOKEN_HEADER);
        if (guestToken == null || guestToken.isBlank()) {
            return Optional.empty();
        }
        Actor actor = actorRepository.findByGuestTokenHash(guestTokenHasher.hash(guestToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_GUEST_TOKEN));
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        if (!actor.isUsableGuestAt(now)) {
            throw new BusinessException(ErrorCode.UNUSABLE_GUEST_TOKEN);
        }
        return Optional.of(CurrentActor.guest(actor.getId()));
    }

    private CurrentActor toMemberActor(Actor actor) {
        long memberId = actor.getMember().getId();
        if (actor.isAdmin()) {
            return CurrentActor.admin(actor.getId(), memberId);
        }
        return CurrentActor.member(actor.getId(), memberId);
    }
}
