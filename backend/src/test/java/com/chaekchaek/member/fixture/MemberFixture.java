package com.chaekchaek.member.fixture;

import com.chaekchaek.member.domain.Member;
import java.time.LocalDateTime;
import org.springframework.test.util.ReflectionTestUtils;

public final class MemberFixture {

    private MemberFixture() {
    }

    public static Member member() {
        return Member.create("익명 회원", null, LocalDateTime.of(2026, 8, 12, 0, 0));
    }

    public static Member savedMember(long id) {
        Member member = member();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
