package com.chaekchaek.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.service.BookResolver;
import com.chaekchaek.common.auth.ActorType;
import com.chaekchaek.common.auth.CurrentActor;
import com.chaekchaek.common.auth.CurrentActorProvider;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.review.book.ReviewBookReader;
import com.chaekchaek.review.domain.Reply;
import com.chaekchaek.review.domain.Review;
import com.chaekchaek.review.domain.ReviewReaction;
import com.chaekchaek.review.dto.PageResponse;
import com.chaekchaek.review.dto.ReviewResponse;
import com.chaekchaek.review.library.ReadingRecordCoordinator;
import com.chaekchaek.review.member.ReviewMemberProfile;
import com.chaekchaek.review.member.ReviewMemberReader;
import com.chaekchaek.review.repository.ReplyReactionRepository;
import com.chaekchaek.review.repository.ReplyRepository;
import com.chaekchaek.review.repository.ReviewReactionRepository;
import com.chaekchaek.review.repository.ReviewRepository;
import com.chaekchaek.review.service.ReviewService.Feed;
import com.chaekchaek.review.service.ReviewService.ReviewSort;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

@DataJpaTest
@ActiveProfiles("test")
class ReviewQueryIntegrationTest {

    private static final long BOOK_ID = 42L;
    private static final long ACTOR_ID = 7L;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReplyRepository replyRepository;

    @Autowired
    private ReviewReactionRepository reactionRepository;

    @Autowired
    private ReplyReactionRepository replyReactionRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private CurrentActorProvider actorProvider;
    private ReviewService service;

    @BeforeEach
    void setUp() {
        actorProvider = mock(CurrentActorProvider.class);
        ReviewMemberReader memberReader = actorIds -> actorIds.stream()
                .collect(Collectors.toMap(
                        actorId -> actorId,
                        actorId -> new ReviewMemberProfile(
                                "작성자 " + actorId,
                                null,
                                "익명 " + actorId,
                                false,
                                false,
                                ActorType.GUEST
                        )
                ));
        service = new ReviewService(
                reviewRepository,
                replyRepository,
                reactionRepository,
                replyReactionRepository,
                actorProvider,
                mock(ReadingRecordCoordinator.class),
                mock(ReviewBookReader.class),
                memberReader,
                mock(BookResolver.class),
                transactionManager
        );
    }

    @ParameterizedTest
    @EnumSource(Feed.class)
    @DisplayName("삭제 답글을 제외한 좋아요와 답글 합계로 인기순을 정렬하고 동점이면 감상 ID가 큰 순서로 조회한다")
    void should_OrderByCombinedScoreThenId_When_PopularReviewsHaveMixedActivity(Feed feed) {
        // given
        Review olderEmpty = saveReview(BOOK_ID, ACTOR_ID, null);
        Review likesOnly = saveReview(BOOK_ID, ACTOR_ID, null);
        Review repliesOnly = saveReview(BOOK_ID, ACTOR_ID, null);
        Review mixed = saveReview(BOOK_ID, ACTOR_ID, null);
        Review deleted = saveReview(BOOK_ID, ACTOR_ID, null);
        deleted.deleteBy(ACTOR_ID);
        Review newerEmpty = saveReview(BOOK_ID, ACTOR_ID, null);
        addLikes(likesOnly, 3);
        addReplies(repliesOnly, 3);
        addLikes(mixed, 2);
        addReplies(mixed, 2);
        addReplies(deleted, 3);
        Reply deletedReply = replyRepository.save(Reply.create(deleted.getId(), ACTOR_ID, "삭제 답글", false));
        deletedReply.deleteBy(ACTOR_ID);
        Review otherBook = saveReview(99L, ACTOR_ID, null);
        addLikes(otherBook, 8);
        when(actorProvider.findCurrentActor()).thenReturn(Optional.of(CurrentActor.guest(ACTOR_ID)));
        flushAndClear();

        // when
        PageResponse<ReviewResponse> response = service.findReviews(BOOK_ID, 1, feed, ReviewSort.POPULAR);

        // then
        assertThat(response.totalCount()).isEqualTo(6);
        assertThat(response.nextPage()).isNull();
        assertThat(response.items()).extracting(ReviewResponse::reviewId)
                .containsExactly(
                        mixed.getId(),
                        deleted.getId(),
                        repliesOnly.getId(),
                        likesOnly.getId(),
                        newerEmpty.getId(),
                        olderEmpty.getId()
                );
        ReviewResponse deletedResponse = response.items().get(1);
        assertThat(deletedResponse.deleted()).isTrue();
        assertThat(deletedResponse.replyCount()).isEqualTo(3);
        assertThat(deletedResponse.recentReplies()).hasSize(3);
        assertThat(deletedResponse.recentReplies())
                .noneMatch(reply -> reply.replyId() == deletedReply.getId());
        ReviewResponse mixedResponse = response.items().getFirst();
        assertThat(mixedResponse.likeCount()).isEqualTo(2);
        assertThat(mixedResponse.replyCount()).isEqualTo(2);
        assertThat(mixedResponse.likedByMe()).isTrue();
        assertThat(response.items().get(4).likeCount()).isZero();
        assertThat(response.items().get(4).replyCount()).isZero();
    }

    @Test
    @DisplayName("인기순 감상 스물한 개를 중복과 누락 없이 페이지로 나누고 마지막 이후는 빈 목록을 반환한다")
    void should_PageWithoutDuplicatesOrOmissions_When_TwentyOnePopularReviewsExist() {
        // given
        List<Long> ids = new ArrayList<>();
        for (int index = 0; index < 21; index++) {
            ids.add(saveReview(BOOK_ID, ACTOR_ID, null).getId());
        }
        flushAndClear();

        // when
        PageResponse<ReviewResponse> first = service.findReviews(BOOK_ID, 1, Feed.ALL, ReviewSort.POPULAR);
        PageResponse<ReviewResponse> second = service.findReviews(BOOK_ID, 2, Feed.ALL, ReviewSort.POPULAR);
        PageResponse<ReviewResponse> third = service.findReviews(BOOK_ID, 3, Feed.ALL, ReviewSort.POPULAR);
        PageResponse<ReviewResponse> beyond = service.findReviews(BOOK_ID, 4, Feed.ALL, ReviewSort.POPULAR);

        // then
        assertThat(first.items()).extracting(ReviewResponse::reviewId)
                .containsExactlyElementsOf(ids.reversed().subList(0, 10));
        assertThat(second.items()).extracting(ReviewResponse::reviewId)
                .containsExactlyElementsOf(ids.reversed().subList(10, 20));
        assertThat(third.items()).extracting(ReviewResponse::reviewId).containsExactly(ids.getFirst());
        assertThat(beyond.items()).isEmpty();
        assertThat(List.of(first, second, third, beyond)).allSatisfy(response ->
                assertThat(response.totalCount()).isEqualTo(21));
        assertThat(first.nextPage()).isEqualTo(2);
        assertThat(second.nextPage()).isEqualTo(3);
        assertThat(third.nextPage()).isNull();
        assertThat(beyond.nextPage()).isNull();
    }

    @Test
    @DisplayName("인기순 감상이 정확히 열 개이면 다음 페이지가 없다")
    void should_HaveNoNextPage_When_ExactlyTenPopularReviewsExist() {
        // given
        for (int index = 0; index < 10; index++) {
            saveReview(BOOK_ID, ACTOR_ID, null);
        }
        flushAndClear();

        // when
        PageResponse<ReviewResponse> response = service.findReviews(BOOK_ID, 1, Feed.ALL, ReviewSort.POPULAR);

        // then
        assertThat(response.totalCount()).isEqualTo(10);
        assertThat(response.items()).hasSize(10);
        assertThat(response.nextPage()).isNull();
    }

    @Test
    @DisplayName("감상이 없으면 인기순 전체 개수와 목록이 비어 있다")
    void should_ReturnEmptyPage_When_NoPopularReviewsExist() {
        // given
        saveReview(99L, ACTOR_ID, null);
        flushAndClear();

        // when
        PageResponse<ReviewResponse> response = service.findReviews(BOOK_ID, 1, Feed.ALL, ReviewSort.POPULAR);

        // then
        assertThat(response.totalCount()).isZero();
        assertThat(response.items()).isEmpty();
        assertThat(response.nextPage()).isNull();
    }

    @Test
    @DisplayName("내 감상만 인기순으로 정렬하고 다른 작성자와 다른 책의 감상을 개수에서 제외한다")
    void should_PageOnlyMyReviews_When_MineFeedIsSortedByPopularity() {
        // given
        when(actorProvider.findCurrentActor()).thenReturn(Optional.of(CurrentActor.guest(ACTOR_ID)));
        Review mostPopular = saveReview(BOOK_ID, ACTOR_ID, null);
        reactionRepository.save(new ReviewReaction(mostPopular.getId(), 8L));
        reactionRepository.save(new ReviewReaction(mostPopular.getId(), 9L));
        replyRepository.save(Reply.create(mostPopular.getId(), 8L, "다른 사람의 답글", false));
        Review runnerUp = saveReview(BOOK_ID, ACTOR_ID, null);
        addLikes(runnerUp, 1);
        List<Long> emptyIds = new ArrayList<>();
        for (int index = 0; index < 9; index++) {
            emptyIds.add(saveReview(BOOK_ID, ACTOR_ID, null).getId());
        }
        Review anotherAuthor = saveReview(BOOK_ID, 8L, null);
        addLikes(anotherAuthor, 5);
        saveReview(99L, ACTOR_ID, null);
        flushAndClear();

        // when
        PageResponse<ReviewResponse> first = service.findReviews(BOOK_ID, 1, Feed.MINE, ReviewSort.POPULAR);
        PageResponse<ReviewResponse> second = service.findReviews(BOOK_ID, 2, Feed.MINE, ReviewSort.POPULAR);

        // then
        List<Long> expectedFirst = new ArrayList<>(List.of(mostPopular.getId(), runnerUp.getId()));
        expectedFirst.addAll(emptyIds.reversed().subList(0, 8));
        assertThat(first.items()).extracting(ReviewResponse::reviewId).containsExactlyElementsOf(expectedFirst);
        assertThat(second.items()).extracting(ReviewResponse::reviewId).containsExactly(emptyIds.getFirst());
        assertThat(first.totalCount()).isEqualTo(11);
        assertThat(second.totalCount()).isEqualTo(11);
        assertThat(first.nextPage()).isEqualTo(2);
        assertThat(second.nextPage()).isNull();
        assertThat(first.items()).allSatisfy(review -> assertThat(review.author().mine()).isTrue());
    }

    @Test
    @DisplayName("다른 사람의 감상만 있으면 내 인기순 감상은 빈 페이지다")
    void should_ReturnEmptyPage_When_OnlyOtherAuthorsHaveReviews() {
        // given
        when(actorProvider.findCurrentActor()).thenReturn(Optional.of(CurrentActor.guest(ACTOR_ID)));
        saveReview(BOOK_ID, 8L, null);
        flushAndClear();

        // when
        PageResponse<ReviewResponse> response = service.findReviews(BOOK_ID, 1, Feed.MINE, ReviewSort.POPULAR);

        // then
        assertThat(response.totalCount()).isZero();
        assertThat(response.items()).isEmpty();
        assertThat(response.nextPage()).isNull();
    }

    @Test
    @DisplayName("인증하지 않으면 내 감상을 인기순으로 조회할 수 없다")
    void should_RejectMineFeed_When_Unauthenticated() {
        // given
        when(actorProvider.findCurrentActor()).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.findReviews(BOOK_ID, 1, Feed.MINE, ReviewSort.POPULAR))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @ParameterizedTest
    @EnumSource(Feed.class)
    @DisplayName("인기순 감상이 많아도 요청한 페이지의 감상 열 개만 영속성 컨텍스트에 로딩한다")
    void should_LoadOnlyRequestedPage_When_ManyPopularReviewsExist(Feed feed) {
        // given
        when(actorProvider.findCurrentActor()).thenReturn(Optional.of(CurrentActor.guest(ACTOR_ID)));
        for (int index = 0; index < 25; index++) {
            saveReview(BOOK_ID, ACTOR_ID, null);
        }
        flushAndClear();

        // when
        PageResponse<ReviewResponse> response = service.findReviews(BOOK_ID, 2, feed, ReviewSort.POPULAR);

        // then
        assertThat(response.totalCount()).isEqualTo(25);
        assertThat(response.items()).hasSize(10);
        assertThat(entityManager.unwrap(Session.class).getStatistics().getEntityCount()).isEqualTo(10);
    }

    private Review saveReview(long bookId, long actorId, Integer page) {
        return reviewRepository.save(Review.create(
                bookId,
                actorId,
                "감상",
                null,
                null,
                page,
                false,
                false
        ));
    }

    private void addLikes(Review review, int count) {
        for (int index = 0; index < count; index++) {
            reactionRepository.save(new ReviewReaction(review.getId(), ACTOR_ID + index));
        }
    }

    private void addReplies(Review review, int count) {
        for (int index = 0; index < count; index++) {
            replyRepository.save(Reply.create(review.getId(), ACTOR_ID, "답글 " + index, false));
        }
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
