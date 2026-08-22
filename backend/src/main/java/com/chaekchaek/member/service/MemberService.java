package com.chaekchaek.member.service;

import com.chaekchaek.auth.token.refresh.RefreshTokenRepository;
import com.chaekchaek.auth.service.AppleAccountService;
import com.chaekchaek.common.exception.MemberNotFoundException;
import com.chaekchaek.common.exception.NicknameAlreadyExistsException;
import com.chaekchaek.common.exception.NicknameRequiredException;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.dto.MemberResponse;
import com.chaekchaek.member.repository.MemberRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AppleAccountService appleAccountService;

    public MemberResponse getMyInfo(Long memberId) {
        Member member = getMember(memberId);

        return MemberResponse.from(member);
    }

    @Transactional
    public MemberResponse updateNickname(Long memberId, String nickname) {
        Member member = getMember(memberId);
        if (memberRepository.existsByNicknameAndIdNot(nickname, memberId)) {
            throw new NicknameAlreadyExistsException();
        }

        member.updateNickname(nickname);
        return MemberResponse.from(member);
    }

    @Transactional
    public MemberResponse updateAnonymity(Long memberId, boolean displayAnonymous) {
        Member member = getMember(memberId);
        if (!displayAnonymous && member.getNickname() == null) {
            throw new NicknameRequiredException();
        }

        member.changeAnonymousDisplay(displayAnonymous);
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
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
    }
}
