package com.chaekchaek.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.chaekchaek.actor.repository.ActorRepository;
import com.chaekchaek.auth.oauth.google.GoogleProfile;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import com.chaekchaek.member.service.MemberService;
import com.chaekchaek.review.domain.Reply;
import com.chaekchaek.review.domain.Review;
import com.chaekchaek.review.dto.AuthorProfileStatus;
import com.chaekchaek.review.dto.ReplyCreateRequest;
import com.chaekchaek.review.dto.ReplyUpdateRequest;
import com.chaekchaek.review.repository.ReplyRepository;
import com.chaekchaek.review.repository.ReviewRepository;
import com.chaekchaek.review.service.ReviewService;
import com.chaekchaek.socialaccount.domain.Provider;
import com.chaekchaek.socialaccount.domain.SocialAccount;
import com.chaekchaek.socialaccount.repository.SocialAccountRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class SocialLoginIntegrationTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReplyRepository replyRepository;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("탈퇴와 재가입 후 공개 답글은 새 닉네임을 표시하고 기존 작성물과 소유권을 보존한다")
    void withdrawalAndRejoinPreserveOldContentAndCreateNewPublicReply() {
        GoogleProfile profile = new GoogleProfile(
                "rejoining-user",
                "user@example.com",
                "image"
        );
        Member oldMember = socialLoginService.loginOrSignUp(profile);
        memberService.updateNickname(oldMember.getId(), "이전 공개 이름");
        memberService.updateAnonymity(oldMember.getId(), false);
        long oldActorId = actorRepository.findByMemberId(oldMember.getId()).orElseThrow().getId();
        String oldAnonymousName = oldMember.getAnonymousNickname();
        var review = reviewRepository.save(Review.create(
                1L,
                oldActorId,
                "기존 감상",
                null,
                null,
                null,
                false,
                false
        ));
        authenticate(oldMember.getId());
        var oldReply = reviewService.createReply(
                review.getId(),
                new ReplyCreateRequest("기존 공개 답글")
        );
        var deletedReply = replyRepository.save(Reply.create(
                review.getId(),
                oldActorId,
                "삭제 답글",
                false
        ));
        deletedReply.deleteBy(oldActorId);
        memberService.withdraw(oldMember.getId());
        assertThatThrownBy(() -> reviewService.createReply(
                review.getId(),
                new ReplyCreateRequest("탈퇴 토큰 답글")
        ))
                .isInstanceOf(BusinessException.class);
        SecurityContextHolder.clearContext();
        entityManager.flush();
        entityManager.clear();

        Member rejoined = socialLoginService.loginOrSignUp(profile);
        long newActorId = actorRepository.findByMemberId(rejoined.getId()).orElseThrow().getId();
        assertThat(rejoined.getId()).isNotEqualTo(oldMember.getId());
        assertThat(newActorId).isNotEqualTo(oldActorId);
        assertThat(rejoined.isDisplayAnonymous()).isTrue();
        assertThat(rejoined.getNickname()).isNull();
        memberService.updateNickname(rejoined.getId(), "새 공개 이름");
        memberService.updateAnonymity(rejoined.getId(), false);
        authenticate(rejoined.getId());
        var newReply = reviewService.createReply(
                review.getId(),
                new ReplyCreateRequest("재가입 공개 답글")
        );
        assertThat(newReply.author().displayName()).isEqualTo("새 공개 이름");
        assertThat(newReply.author().memberId()).isEqualTo(rejoined.getId());
        assertThat(newReply.author().profileStatus())
                .isEqualTo(AuthorProfileStatus.AVAILABLE);
        var oldResponse = reviewService.findReplies(review.getId(), 1).items().stream()
                .filter(reply -> reply.replyId() == oldReply.replyId()).findFirst().orElseThrow();
        assertThat(oldResponse.author().displayName()).isEqualTo(oldAnonymousName);
        assertThat(oldResponse.author().memberId()).isNull();
        assertThat(oldResponse.author().mine()).isFalse();
        assertThat(replyRepository.findById(oldReply.replyId()).orElseThrow().getActorId()).isEqualTo(oldActorId);
        assertThat(reviewRepository.findById(review.getId()).orElseThrow().getActorId()).isEqualTo(oldActorId);
        assertThat(replyRepository.findById(deletedReply.getId()).orElseThrow().isDeleted()).isTrue();
        assertThatThrownBy(() -> reviewService.updateReply(
                oldReply.replyId(),
                new ReplyUpdateRequest("수정 시도")
        ))
                .isInstanceOf(BusinessException.class);
        entityManager.flush();
        entityManager.clear();
        assertThat(socialLoginService.loginOrSignUp(profile).getId()).isEqualTo(rejoined.getId());
        assertThat(socialAccountRepository.count()).isEqualTo(1);
    }

    private void authenticate(Long memberId) {
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "none").subject(memberId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of()));
    }

    @Autowired
    private SocialLoginService socialLoginService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private ActorRepository actorRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("최초 Google 로그인 시 회원과 소셜 계정을 생성한다")
    void should_CreateMemberAndSocialAccount_When_FirstGoogleLogin() {
        // given
        GoogleProfile profile = new GoogleProfile(
                "google-user-123",
                "member@example.com",
                "exUrl"
        );

        // when
        Member member = socialLoginService.loginOrSignUp(profile);

        // then
        SocialAccount socialAccount = socialAccountRepository
                .findByProviderAndProviderUserId(
                Provider.GOOGLE,
                profile.providerUserId()
        )
                .orElseThrow();

        assertAll(
                () -> assertThat(member.getId()).isNotNull(),
                () -> assertThat(memberRepository.count()).isEqualTo(1),
                () -> assertThat(socialAccountRepository.count()).isEqualTo(1),
                () -> assertThat(actorRepository.findByMemberId(member.getId())).isPresent(),
                () -> assertThat(socialAccount.getMember().getId()).isEqualTo(member.getId()),
                () -> assertThat(socialAccount.getProvider()).isEqualTo(Provider.GOOGLE),
                () -> assertThat(socialAccount.getProviderUserId()).isEqualTo(profile.providerUserId())
        );
    }

    @Test
    @DisplayName("같은 Google 계정으로 다시 로그인해도 회원을 중복 생성하지 않는다")
    void should_NotCreateDuplicateMember_When_GoogleLoginIsRepeated() {
        // given
        GoogleProfile profile = new GoogleProfile(
                "google-user-123",
                "member@example.com",
                "exUrl"
        );

        // when
        Member firstLoginMember = socialLoginService.loginOrSignUp(profile);
        Long firstMemberId = firstLoginMember.getId();
        entityManager.flush();
        entityManager.clear();
        Member secondLoginMember = socialLoginService.loginOrSignUp(profile);

        // then
        assertAll(
                () -> assertThat(secondLoginMember.getId()).isEqualTo(firstLoginMember.getId()),
                () -> assertThat(memberRepository.count()).isEqualTo(1),
                () -> assertThat(socialAccountRepository.count()).isEqualTo(1),
                () -> assertThat(secondLoginMember.getId()).isEqualTo(firstMemberId)
        );
    }
}
