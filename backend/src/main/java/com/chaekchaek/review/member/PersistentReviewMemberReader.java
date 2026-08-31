package com.chaekchaek.review.member;

import com.chaekchaek.actor.domain.Actor;
import com.chaekchaek.actor.repository.ActorRepository;
import com.chaekchaek.common.auth.ActorType;
import com.chaekchaek.member.domain.AccountStatus;
import com.chaekchaek.member.domain.Member;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PersistentReviewMemberReader implements ReviewMemberReader {

    private final ActorRepository actorRepository;

    @Override
    public Map<Long, ReviewMemberProfile> findByActorIds(Collection<Long> actorIds) {
        return actorRepository.findAllById(actorIds).stream()
                .collect(Collectors.toMap(Actor::getId, this::toProfile));
    }

    private ReviewMemberProfile toProfile(Actor actor) {
        if (actor.getType() == ActorType.GUEST) {
            return new ReviewMemberProfile(null, actor.getGuestNickname(), null, actor.getGuestNickname(), true, null,
                    actor.getType());
        }
        Member member = actor.getMember();
        return new ReviewMemberProfile(
                member.getId(),
                member.getNickname(),
                member.getProfileImageUrl(),
                member.getAnonymousNickname(),
                member.isDisplayAnonymous(),
                member.getAccountStatus(),
                ActorType.MEMBER
        );
    }
}
