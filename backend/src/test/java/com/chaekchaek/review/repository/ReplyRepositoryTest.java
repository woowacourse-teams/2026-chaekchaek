package com.chaekchaek.review.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaekchaek.review.domain.Reply;
import com.chaekchaek.review.domain.Review;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class ReplyRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReplyRepository replyRepository;

    @Test
    @DisplayName("감상마다 최신 답글 세 개를 선택한 뒤 오래된 순으로 반환한다")
    void should_ReturnLatestThreePerReviewInAscendingOrder_When_FindingRecentReplies() {
        // given
        Review firstReview = reviewRepository.save(Review.create(1L, 1L, "첫 감상", null, null, null, false, false));
        Review secondReview = reviewRepository.save(Review.create(1L, 2L, "둘째 감상", null, null, null, false, false));
        List<Reply> firstReplies = List.of(
                replyRepository.save(Reply.create(firstReview.getId(), 1L, "1", false)),
                replyRepository.save(Reply.create(firstReview.getId(), 1L, "2", false)),
                replyRepository.save(Reply.create(firstReview.getId(), 1L, "3", false)),
                replyRepository.save(Reply.create(firstReview.getId(), 1L, "4", false))
        );
        List<Reply> secondReplies = List.of(
                replyRepository.save(Reply.create(secondReview.getId(), 2L, "5", false)),
                replyRepository.save(Reply.create(secondReview.getId(), 2L, "6", false)),
                replyRepository.save(Reply.create(secondReview.getId(), 2L, "7", false)),
                replyRepository.save(Reply.create(secondReview.getId(), 2L, "8", false))
        );

        // when
        List<Reply> actual = replyRepository.findRecentThreeByReviewIdIn(List.of(firstReview.getId(), secondReview.getId()));

        firstReview.deleteBy(1L);
        firstReplies.get(0).deleteBy(1L);

        // then
        assertThat(actual).extracting(Reply::getId).containsExactly(
                firstReplies.get(1).getId(), firstReplies.get(2).getId(), firstReplies.get(3).getId(),
                secondReplies.get(1).getId(), secondReplies.get(2).getId(), secondReplies.get(3).getId()
        );
        assertThat(reviewRepository.countByBookIdInGroupByBookId(List.of(1L)))
                .extracting(ReviewRepository.BookCommentCount::getCount)
                .containsExactly(2L);
        assertThat(replyRepository.countByReviewBookIdInGroupByBookId(List.of(1L)))
                .extracting(ReplyRepository.BookCommentCount::getCount)
                .containsExactly(8L);
    }
}
