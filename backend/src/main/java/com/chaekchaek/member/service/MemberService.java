package com.chaekchaek.member.service;

import com.chaekchaek.actor.domain.Actor;
import com.chaekchaek.actor.repository.ActorRepository;
import com.chaekchaek.auth.service.AppleAccountService;
import com.chaekchaek.auth.token.refresh.RefreshTokenRepository;
import com.chaekchaek.common.exception.MemberNotFoundException;
import com.chaekchaek.common.exception.NicknameAlreadyExistsException;
import com.chaekchaek.common.exception.NicknameRequiredException;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.dto.MemberResponse;
import com.chaekchaek.member.dto.MyInfoResponse;
import com.chaekchaek.member.repository.MemberRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private static final Logger log = LoggerFactory.getLogger(MemberService.class);

    private final MemberRepository memberRepository;
    private final ActorRepository actorRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AppleAccountService appleAccountService;

    public MyInfoResponse getMyInfo(Long memberId) {
        Member member = getMember(memberId);
        Actor actor = actorRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalStateException("회원 Actor가 존재해야 합니다: memberId=" + memberId));

        return MyInfoResponse.from(member, actor);
    }

    @Transactional
    public MemberResponse updateNickname(Long memberId, String nickname) {
        Member member = getMember(memberId);
        if (memberRepository.existsByNicknameAndIdNot(nickname, memberId)) {
            throw new NicknameAlreadyExistsException();
        }

        member.updateNickname(nickname);
        log.atInfo()
                .addKeyValue("type", "event")
                .addKeyValue("event", "nickname_updated")
                .addKeyValue("memberId", memberId)
                .log("Nickname updated");
        return MemberResponse.from(member);
    }

    @Transactional
    public MemberResponse updateAnonymity(Long memberId, boolean displayAnonymous) {
        Member member = getMember(memberId);
        if (!displayAnonymous && member.getNickname() == null) {
            throw new NicknameRequiredException();
        }

        member.changeAnonymousDisplay(displayAnonymous);
        log.atInfo()
                .addKeyValue("type", "event")
                .addKeyValue("event", "anonymity_updated")
                .addKeyValue("memberId", memberId)
                .addKeyValue("displayAnonymous", displayAnonymous)
                .log("Anonymity updated");
        return MemberResponse.from(member);
    }

    @Transactional
    public void withdraw(Long memberId) {
        Member member = getMember(memberId);
        appleAccountService.revokeIfConnected(memberId);
        LocalDateTime withdrawnAt = LocalDateTime.now();

        member.withdraw(withdrawnAt);
        refreshTokenRepository.findAllByMemberIdAndRevokedAtIsNull(memberId)
                .forEach(refreshToken -> refreshToken.revoke(withdrawnAt));
        log.atInfo()
                .addKeyValue("type", "event")
                .addKeyValue("event", "member_withdrawn")
                .addKeyValue("memberId", memberId)
                .log("Member withdrawn");
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
    }
}
