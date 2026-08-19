package com.chaekchaek.review.member;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(PersistentReviewMemberReader.class)
class PersistentReviewMemberReaderTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ReviewMemberReader reviewMemberReader;

    @Test
    @DisplayName("감상 작성자에게 필요한 실제 회원 정보를 조회한다")
    void should_ReturnReviewMemberProfile_When_MemberExists() {
        // given
        Member member = memberRepository.save(Member.create(
                "책책 회원",
                "exUrl",
                LocalDateTime.of(2026, 8, 18, 12, 0)
        ));

        // when
        Map<Long, ReviewMemberProfile> profiles =
                reviewMemberReader.findByMemberIds(List.of(member.getId()));

        // then
        assertThat(profiles).containsEntry(member.getId(), new ReviewMemberProfile(
                null,
                "exUrl",
                "책책 회원",
                true,
                false
        ));
    }
}
