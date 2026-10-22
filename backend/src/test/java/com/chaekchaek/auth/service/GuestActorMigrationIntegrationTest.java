package com.chaekchaek.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.chaekchaek.actor.domain.Actor;
import com.chaekchaek.actor.repository.ActorRepository;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import com.chaekchaek.review.domain.Reply;
import com.chaekchaek.review.domain.ReplyReaction;
import com.chaekchaek.review.domain.Review;
import com.chaekchaek.review.domain.ReviewReaction;
import com.chaekchaek.review.repository.ReplyReactionRepository;
import com.chaekchaek.review.repository.ReplyRepository;
import com.chaekchaek.review.repository.ReviewReactionRepository;
import com.chaekchaek.review.repository.ReviewRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class GuestActorMigrationIntegrationTest {

    @Autowired
    private GuestActorMigrationService migrationService;

    @Autowired
    private ActorRepository actorRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReplyRepository replyRepository;

    @Autowired
    private ReviewReactionRepository reviewReactionRepository;

    @Autowired
    private ReplyReactionRepository replyReactionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("게스트의 삭제된 답글도 회원 Actor로 이전하고 삭제 시각은 보존한다")
    void should_ReassignDeletedRepliesWithoutChangingDeletionTime_When_GuestMigratesToExistingMember() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 9, 2, 12, 0);
        Member member = memberRepository.save(Member.create("회원 익명 닉네임", null, now));
        Actor memberActor = actorRepository.save(Actor.member(member, now));
        Actor guestActor = actorRepository.save(
                Actor.guest("b".repeat(64), "게스트 참새", now.minusDays(1), now.plusDays(89)));
        Review review = reviewRepository.save(
                Review.create(1L, guestActor.getId(), "게스트 감상", null, null, null, false, true));
        Reply activeReply = replyRepository.save(Reply.create(review.getId(), guestActor.getId(), "정상 답글", true));
        Reply deletedReply = replyRepository.save(Reply.create(review.getId(), guestActor.getId(), "삭제 답글", true));
        deletedReply.deleteBy(guestActor.getId());
        entityManager.flush();
        Object deletedAtBefore = entityManager.createNativeQuery("select deleted_at from reply where reply_id = :replyId")
                .setParameter("replyId", deletedReply.getId())
                .getSingleResult();

        // when
        migrationService.migrate(guestActor.getId(), member);
        entityManager.flush();
        entityManager.clear();

        // then
        Object[] deletedState = (Object[]) entityManager.createNativeQuery(
                        "select actor_id, deleted_at, content from reply where reply_id = :replyId")
                .setParameter("replyId", deletedReply.getId())
                .getSingleResult();
        assertThat(((Number) deletedState[0]).longValue()).isEqualTo(memberActor.getId());
        assertThat(deletedAtBefore).isNotNull();
        assertThat(deletedState[1]).isEqualTo(deletedAtBefore);
        assertThat(deletedState[2]).isEqualTo("삭제 답글");
        assertThat(replyRepository.findById(deletedReply.getId())).isEmpty();
        assertThat(replyRepository.findById(activeReply.getId()).orElseThrow().getActorId()).isEqualTo(memberActor.getId());
    }

    @Test
    @DisplayName("기존 회원 로그인 시 게스트의 공개 상호작용을 회원 Actor로 이전한다")
    void should_ReassignGuestInteractions_When_MemberActorAlreadyExists() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 9, 2, 12, 0);
        Member member = memberRepository.save(Member.create("회원 익명 닉네임", null, now));
        Actor memberActor = actorRepository.save(Actor.member(member, now));
        Actor guestActor = actorRepository.save(
                Actor.guest("a".repeat(64), "게스트 참새", now.minusDays(1), now.plusDays(89)));

        Review conflictedReview = reviewRepository.save(
                Review.create(1L, guestActor.getId(), "게스트 감상 1", null, null, null, false, true));
        Review uniqueReview = reviewRepository.save(
                Review.create(2L, guestActor.getId(), "게스트 감상 2", null, null, null, false, true));
        Reply reply = replyRepository.save(
                Reply.create(conflictedReview.getId(), guestActor.getId(), "게스트 답글", true));

        reviewReactionRepository.save(new ReviewReaction(conflictedReview.getId(), guestActor.getId()));
        reviewReactionRepository.save(new ReviewReaction(conflictedReview.getId(), memberActor.getId()));
        reviewReactionRepository.save(new ReviewReaction(uniqueReview.getId(), guestActor.getId()));
        replyReactionRepository.save(new ReplyReaction(reply.getId(), guestActor.getId()));

        migrationService.migrate(guestActor.getId(), member);
        entityManager.flush();
        entityManager.clear();

        // when & then
        assertAll(
                () -> assertThat(reviewRepository.findByBookIdAndActorId(1L, memberActor.getId()))
                        .extracting(Review::getContent).containsExactly("게스트 감상 1"),
                () -> assertThat(reviewRepository.findByBookIdAndActorId(2L, memberActor.getId()))
                        .extracting(Review::getContent).containsExactly("게스트 감상 2"),
                () -> assertThat(replyRepository.findById(reply.getId()).orElseThrow().getActorId())
                        .isEqualTo(memberActor.getId()),
                () -> assertThat(reviewReactionRepository.countByReviewId(conflictedReview.getId()))
                        .isEqualTo(2),
                () -> assertThat(reviewReactionRepository.countByReviewId(uniqueReview.getId()))
                        .isEqualTo(1),
                () -> assertThat(replyReactionRepository.countByReplyId(reply.getId()))
                        .isEqualTo(1),
                () -> assertThat(reviewReactionRepository.findByReviewIdInAndActorId(
                        List.of(conflictedReview.getId(), uniqueReview.getId()),
                        guestActor.getId()
                ))
                        .hasSize(2),
                () -> assertThat(replyReactionRepository.findByReplyIdInAndActorId(
                        List.of(reply.getId()),
                        guestActor.getId()
                ))
                        .hasSize(1),
                () -> assertThat(actorRepository.findById(guestActor.getId()).orElseThrow().getGuestTokenHash())
                        .isNull(),
                () -> assertThat(actorRepository.findById(guestActor.getId()).orElseThrow().getGuestNickname())
                        .isNull(),
                () -> assertThat(actorRepository.findById(guestActor.getId()).orElseThrow().getExpiresAt())
                        .isNull(),
                () -> assertThat(actorRepository.findById(guestActor.getId()).orElseThrow().getRevokedAt())
                        .isNotNull()
        );
    }
}
