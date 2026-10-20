package com.chaekchaek.review.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaekchaek.review.domain.Reply;
import com.chaekchaek.review.domain.Review;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class ReplyRepositoryTest {

    private static final Sort REPLY_ORDER = Sort.by("createdAt").ascending().and(Sort.by("id").ascending());

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
                firstReplies.get(1).getId(),
                firstReplies.get(2).getId(),
                firstReplies.get(3).getId(),
                secondReplies.get(1).getId(),
                secondReplies.get(2).getId(),
                secondReplies.get(3).getId()
        );
        assertThat(reviewRepository.countByBookIdInGroupByBookId(List.of(1L)))
                .extracting(ReviewRepository.BookCommentCount::getCount)
                .containsExactly(2L);
        assertThat(replyRepository.countByReviewBookIdInGroupByBookId(List.of(1L)))
                .extracting(ReplyRepository.BookCommentCount::getCount)
                .containsExactly(7L);
    }

    @Test
    @DisplayName("최신 답글에 삭제 답글이 섞여도 감상마다 정상 답글 세 개를 채운다")
    void should_FillLatestThreeActiveRepliesPerReview_When_RecentRepliesIncludeDeletedOnes() {
        // given
        Review firstReview = reviewRepository.save(Review.create(1L, 1L, "첫 감상", null, null, null, false, false));
        Review secondReview = reviewRepository.save(Review.create(1L, 2L, "둘째 감상", null, null, null, false, false));
        List<Reply> firstReplies = new ArrayList<>();
        List<Reply> secondReplies = new ArrayList<>();
        for (int index = 0; index < 4; index++) {
            firstReplies.add(replyRepository.save(Reply.create(firstReview.getId(), 1L, "첫 답글 " + index, false)));
            replyRepository.save(Reply.create(firstReview.getId(), 1L, "삭제 답글", false)).deleteBy(1L);
            secondReplies.add(replyRepository.save(Reply.create(secondReview.getId(), 2L, "둘째 답글 " + index, false)));
        }
        replyRepository.flush();

        // when
        List<Reply> actual = replyRepository.findRecentThreeByReviewIdIn(List.of(firstReview.getId(), secondReview.getId()));

        // then
        assertThat(actual).extracting(Reply::getId).containsExactly(
                firstReplies.get(1).getId(),
                firstReplies.get(2).getId(),
                firstReplies.get(3).getId(),
                secondReplies.get(1).getId(),
                secondReplies.get(2).getId(),
                secondReplies.get(3).getId()
        );
    }

    @Test
    @DisplayName("감상의 답글이 모두 삭제되면 최근 답글이 비어 있다")
    void should_ReturnNoRecentReplies_When_AllRepliesAreDeleted() {
        // given
        Review review = reviewRepository.save(Review.create(1L, 1L, "감상", null, null, null, false, false));
        replyRepository.save(Reply.create(review.getId(), 1L, "삭제 답글", false)).deleteBy(1L);
        replyRepository.save(Reply.create(99L, 1L, "다른 감상의 답글", false));
        replyRepository.flush();

        // when
        List<Reply> actual = replyRepository.findRecentThreeByReviewIdIn(List.of(review.getId()));

        // then
        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("삭제되지 않은 감상과 답글만 책별로 집계한다")
    void should_ExcludeDeletedReviewsAndReplies_When_CountingActiveBookActivities() {
        // given
        Review activeReview = reviewRepository.save(Review.create(1L, 1L, "유효 감상", null, null, null, false, false));
        Review deletedReview = reviewRepository.save(Review.create(2L, 2L, "삭제 감상", null, null, null, false, false));
        replyRepository.save(Reply.create(activeReview.getId(), 1L, "유효 답글", false));
        Reply deletedReply = replyRepository.save(Reply.create(activeReview.getId(), 1L, "삭제 답글", false));
        replyRepository.save(Reply.create(deletedReview.getId(), 2L, "삭제된 감상의 답글", false));
        deletedReply.deleteBy(1L);
        deletedReview.deleteBy(2L);
        replyRepository.flush();

        // when
        List<ReviewRepository.PopularBookCount> popularBookCounts = reviewRepository.findTop10PopularBookCounts();
        List<ReplyRepository.ReviewCount> replyCounts =
                replyRepository.countActiveByReviewIdInGroupByReviewId(List.of(activeReview.getId(), deletedReview.getId()));

        // then
        assertThat(popularBookCounts).singleElement().satisfies(count -> {
            assertThat(count.getBookId()).isEqualTo(1L);
            assertThat(count.getReviewCount()).isEqualTo(1L);
            assertThat(count.getReplyCount()).isEqualTo(1L);
        });
        assertThat(replyCounts).singleElement().satisfies(count -> {
            assertThat(count.getReviewId()).isEqualTo(activeReview.getId());
            assertThat(count.getCount()).isEqualTo(1L);
        });
    }

    @Test
    @DisplayName("삭제와 스포일러 감상을 제외한 최신 감상 열 개를 반환한다")
    void should_ReturnTopTenNonSpoilerReviewsInLatestOrder_When_FindingLatestReviews() {
        // given
        List<Review> nonSpoilerReviews = new ArrayList<>();
        for (int index = 0; index < 10; index++) {
            nonSpoilerReviews.add(reviewRepository.save(
                    Review.create(1L, 1L, "감상 " + index, null, null, null, false, false)
            ));
        }
        reviewRepository.save(Review.create(1L, 1L, "스포일러 감상", null, null, null, true, false));
        Review deletedReview = reviewRepository.save(Review.create(1L, 1L, "삭제 감상", null, null, null, false, false));
        deletedReview.deleteBy(1L);
        reviewRepository.flush();

        // when
        List<Review> actual = reviewRepository.findTop10ByDeletedAtIsNullAndSpoilerFalseOrderByCreatedAtDescIdDesc();

        // then
        assertThat(actual).extracting(Review::getId)
                .containsExactlyElementsOf(nonSpoilerReviews.reversed()
                        .stream()
                        .map(Review::getId)
                        .toList());
    }

    @Test
    @DisplayName("감상의 삭제되지 않은 답글만 작성 시간과 ID 오름차순으로 페이지를 나눠 조회한다")
    void should_PageOnlyActiveRepliesOfReview_When_DeletedRepliesAreMixed() {
        // given
        Review review = reviewRepository.save(Review.create(1L, 1L, "감상", null, null, null, false, false));
        Review otherReview = reviewRepository.save(Review.create(1L, 2L, "다른 감상", null, null, null, false, false));
        List<Long> activeReplyIds = new ArrayList<>();
        for (int index = 0; index < 11; index++) {
            replyRepository.save(Reply.create(review.getId(), 1L, "삭제 답글", false)).deleteBy(1L);
            activeReplyIds.add(replyRepository.save(Reply.create(review.getId(), 1L, "답글", false)).getId());
        }
        replyRepository.save(Reply.create(otherReview.getId(), 2L, "다른 감상의 답글", false));
        replyRepository.flush();

        // when
        Page<Reply> firstPage = replyRepository.findByReviewId(
                review.getId(),
                PageRequest.of(0, 10, REPLY_ORDER)
        );
        Page<Reply> secondPage = replyRepository.findByReviewId(
                review.getId(),
                PageRequest.of(1, 10, REPLY_ORDER)
        );

        // then
        assertThat(firstPage.getTotalElements()).isEqualTo(11);
        assertThat(firstPage.getContent()).extracting(Reply::getId)
                .containsExactlyElementsOf(activeReplyIds.subList(0, 10));
        assertThat(secondPage.getContent()).extracting(Reply::getId)
                .containsExactly(activeReplyIds.getLast());
    }

    @Test
    @DisplayName("감상의 답글이 모두 삭제되면 빈 목록과 전체 개수 0을 반환한다")
    void should_ReturnEmptyPage_When_AllRepliesOfReviewAreDeleted() {
        // given
        Review review = reviewRepository.save(Review.create(1L, 1L, "감상", null, null, null, false, false));
        replyRepository.save(Reply.create(review.getId(), 1L, "삭제 답글", false)).deleteBy(1L);
        replyRepository.save(Reply.create(review.getId(), 1L, "삭제 답글", false)).deleteBy(1L);
        replyRepository.flush();

        // when
        Page<Reply> actual = replyRepository.findByReviewId(
                review.getId(),
                PageRequest.of(0, 10, REPLY_ORDER)
        );

        // then
        assertThat(actual.getContent()).isEmpty();
        assertThat(actual.getTotalElements()).isZero();
    }
}
