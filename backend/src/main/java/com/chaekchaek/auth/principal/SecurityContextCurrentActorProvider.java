package com.chaekchaek.auth.principal;

import com.chaekchaek.actor.domain.Actor;
import com.chaekchaek.actor.repository.ActorRepository;
import com.chaekchaek.common.auth.CurrentActor;
import com.chaekchaek.common.auth.CurrentActorProvider;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityContextCurrentActorProvider implements CurrentActorProvider {

    private final SecurityContextCurrentMemberIdProvider currentMemberIdProvider;
    private final ActorRepository actorRepository;

    @Override
    public CurrentActor getCurrentActor() {
        return findCurrentActor().orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    @Override
    public Optional<CurrentActor> findCurrentActor() {
        return currentMemberIdProvider.findCurrentMemberId().stream()
                .mapToObj(memberId -> actorRepository.findByMemberId(memberId)
                        .map(this::toCurrentActor))
                .findFirst()
                .flatMap(actor -> actor);
    }

    private CurrentActor toCurrentActor(Actor actor) {
        return CurrentActor.member(actor.getId(), actor.getMember().getId());
    }
}
