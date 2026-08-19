package com.chaekchaek.member.service;

import com.chaekchaek.common.exception.MemberNotFoundException;
import com.chaekchaek.common.exception.NicknameAlreadyExistsException;
import com.chaekchaek.common.exception.NicknameRequiredException;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.dto.MemberResponse;
import com.chaekchaek.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

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

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
    }
}
