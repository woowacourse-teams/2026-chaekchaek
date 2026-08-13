package com.chaekchaek.member.service;

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

    private static final String MEMBER_MUST_EXIST_ERROR_MESSAGE =
            "[ERROR] 회원이 존재해야 합니다";

    private final MemberRepository memberRepository;

    public MemberResponse getMyInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException(MEMBER_MUST_EXIST_ERROR_MESSAGE));

        return MemberResponse.from(member);
    }
}
