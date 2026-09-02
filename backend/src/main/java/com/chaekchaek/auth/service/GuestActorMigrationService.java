package com.chaekchaek.auth.service;

import com.chaekchaek.actor.domain.Actor;
import com.chaekchaek.actor.repository.ActorRepository;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.review.repository.ReplyReactionRepository;
import com.chaekchaek.review.repository.ReplyRepository;
import com.chaekchaek.review.repository.ReviewReactionRepository;
import com.chaekchaek.review.repository.ReviewRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GuestActorMigrationService {

    private final ActorRepository actorRepository;
    private final ReviewRepository reviewRepository;
    private final ReplyRepository replyRepository;
    private final ReviewReactionRepository reviewReactionRepository;
    private final ReplyReactionRepository replyReactionRepository;
    private final Clock clock;

    @Transactional
    public void migrate(Long guestActorId, Member member) {
        if (guestActorId == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        Actor guestActor = actorRepository.findByIdForUpdate(guestActorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_GUEST_TOKEN));
        if (!guestActor.isUsableGuestAt(now)) {
            throw new BusinessException(ErrorCode.UNUSABLE_GUEST_TOKEN);
        }

        Optional<Actor> memberActor = actorRepository.findByMemberId(member.getId());
        if (memberActor.isEmpty()) {
            guestActor.convertToMember(member);
            return;
        }

        Actor targetActor = actorRepository.findByIdForUpdate(memberActor.get().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (guestActor.getId().equals(targetActor.getId())) {
            return;
        }

        reviewReactionRepository.deleteConflictingReactions(guestActorId, targetActor.getId());
        reviewReactionRepository.reassignActorId(guestActorId, targetActor.getId());
        replyReactionRepository.deleteConflictingReactions(guestActorId, targetActor.getId());
        replyReactionRepository.reassignActorId(guestActorId, targetActor.getId());
        reviewRepository.reassignActorId(guestActorId, targetActor.getId());
        replyRepository.reassignActorId(guestActorId, targetActor.getId());
        guestActor.retireGuest(now);
        actorRepository.save(guestActor);
    }
}
