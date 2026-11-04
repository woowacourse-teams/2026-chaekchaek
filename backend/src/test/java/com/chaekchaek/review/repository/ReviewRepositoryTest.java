package com.chaekchaek.review.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaekchaek.review.domain.Reply;
import com.chaekchaek.review.domain.Review;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class ReviewRepositoryTest {

    private static final long BOOK_ID = 1L;
    private static final long ACTOR_ID = 1L;
    private static final long OTHER_ACTOR_ID = 2L;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReplyRepository replyRepository;

    private Review active;
    private Review deletedWithActiveReply;
    private Review deletedWithoutReply;
    private Review deletedWithDeletedReply;
    private Review otherActorReview;

    @BeforeEach
    void setUp() {
        active = saveReview(ACTOR_ID);
        deletedWithActiveReply = saveReview(ACTOR_ID);
        replyRepository.save(Reply.create(deletedWithActiveReply.getId(), OTHER_ACTOR_ID, "답글", false));
        deletedWithActiveReply.deleteBy(ACTOR_ID);
        deletedWithoutReply = saveReview(ACTOR_ID);
        deletedWithoutReply.deleteBy(ACTOR_ID);
        deletedWithDeletedReply = saveReview(ACTOR_ID);
        replyRepository.save(Reply.create(deletedWithDeletedReply.getId(), OTHER_ACTOR_ID, "삭제 답글", false))
                .deleteBy(OTHER_ACTOR_ID);
        deletedWithDeletedReply.deleteBy(ACTOR_ID);
        otherActorReview = saveReview(OTHER_ACTOR_ID);
        reviewRepository.flush();
    }

    @Test
    @DisplayName("책의 감상 목록과 인기순 목록은 정상 감상과 정상 답글이 남은 삭제 감상만 조회한다")
    void should_FindOnlyVisibleReviews_When_FindingBookReviews() {
        // given
        Pageable firstPage = PageRequest.of(0, 10);

        // when
        Page<Review> allReviews = reviewRepository.findVisibleByBookId(BOOK_ID, firstPage);
        Page<Review> myReviews = reviewRepository.findVisibleByBookIdAndActorId(BOOK_ID, ACTOR_ID, firstPage);
        List<Review> popularReviews = reviewRepository.findPopularByBookId(BOOK_ID, 10, 0);
        List<Review> myPopularReviews = reviewRepository.findPopularByBookIdAndActorId(BOOK_ID, ACTOR_ID, 10, 0);

        // then
        assertThat(allReviews.getContent()).extracting(Review::getId)
                .containsExactlyInAnyOrder(active.getId(), deletedWithActiveReply.getId(), otherActorReview.getId());
        assertThat(myReviews.getContent()).extracting(Review::getId)
                .containsExactlyInAnyOrder(active.getId(), deletedWithActiveReply.getId());
        assertThat(popularReviews).extracting(Review::getId)
                .containsExactlyInAnyOrder(active.getId(), deletedWithActiveReply.getId(), otherActorReview.getId());
        assertThat(myPopularReviews).extracting(Review::getId)
                .containsExactlyInAnyOrder(active.getId(), deletedWithActiveReply.getId());
    }

    @Test
    @DisplayName("책의 감상 개수는 정상 감상과 정상 답글이 남은 삭제 감상만 센다")
    void should_CountOnlyVisibleReviews_When_CountingBookReviews() {
        // given
        Pageable oneItem = PageRequest.of(0, 1);

        // when & then
        assertThat(reviewRepository.countVisibleByBookId(BOOK_ID)).isEqualTo(3);
        assertThat(reviewRepository.countVisibleByBookIdAndActorId(BOOK_ID, ACTOR_ID)).isEqualTo(2);
        assertThat(reviewRepository.findVisibleByBookId(BOOK_ID, oneItem).getTotalElements()).isEqualTo(3);
        assertThat(reviewRepository.findVisibleByBookIdAndActorId(BOOK_ID, ACTOR_ID, oneItem).getTotalElements())
                .isEqualTo(2);
    }

    @Test
    @DisplayName("정상 감상과 정상 답글이 남은 삭제 감상만 보이는 감상으로 확인한다")
    void should_ConfirmOnlyVisibleReviews_When_CheckingVisibleReviewById() {
        // when & then
        assertThat(reviewRepository.existsVisibleById(active.getId())).isTrue();
        assertThat(reviewRepository.existsVisibleById(deletedWithActiveReply.getId())).isTrue();
        assertThat(reviewRepository.existsVisibleById(deletedWithoutReply.getId())).isFalse();
        assertThat(reviewRepository.existsVisibleById(deletedWithDeletedReply.getId())).isFalse();
        assertThat(reviewRepository.existsVisibleById(Long.MAX_VALUE)).isFalse();
    }

    @Test
    @DisplayName("도서별 감상 수는 정상 감상과 정상 답글이 남은 삭제 감상만 센다")
    void should_CountOnlyVisibleReviewsPerBook_When_CountingReviewsGroupedByBook() {
        // when & then
        assertThat(reviewRepository.countByBookIdInGroupByBookId(List.of(BOOK_ID)))
                .extracting(ReviewRepository.BookCommentCount::getCount)
                .containsExactly(3L);
    }

    private Review saveReview(long actorId) {
        return reviewRepository.save(Review.create(BOOK_ID, actorId, "감상", null, null, null, false, false));
    }
}
