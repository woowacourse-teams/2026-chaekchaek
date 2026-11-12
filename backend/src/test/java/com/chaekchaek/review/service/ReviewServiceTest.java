package com.chaekchaek.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.service.BookResolver;
import com.chaekchaek.common.auth.ActorType;
import com.chaekchaek.common.auth.CurrentActor;
import com.chaekchaek.common.auth.CurrentActorProvider;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.library.service.BookActivityCountReader.ActivityCounts;
import com.chaekchaek.review.book.ReviewBookReader;
import com.chaekchaek.review.domain.Reply;
import com.chaekchaek.review.domain.ReplyReaction;
import com.chaekchaek.review.domain.Review;
import com.chaekchaek.review.domain.ReviewReaction;
import com.chaekchaek.review.dto.AuthorProfileStatus;
import com.chaekchaek.review.dto.ReplyCreateRequest;
import com.chaekchaek.review.dto.ReviewCreateByIsbnResponse;
import com.chaekchaek.review.dto.ReviewCreateRequest;
import com.chaekchaek.review.dto.ReviewResponse;
import com.chaekchaek.review.dto.ReviewUpdateRequest;
import com.chaekchaek.review.event.BookActivityChangedEvent;
import com.chaekchaek.review.member.ReviewMemberProfile;
import com.chaekchaek.review.member.ReviewMemberReader;
import com.chaekchaek.review.repository.ReplyReactionRepository;
import com.chaekchaek.review.repository.ReplyRepository;
import com.chaekchaek.review.repository.ReviewReactionRepository;
import com.chaekchaek.review.repository.ReviewRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class ReviewServiceTest {

    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    @Test
    @DisplayName("게스트는 서재를 변경하지 않고 감상을 작성한다")
    void should_CreateReviewWithoutChangingLibrary_When_ActorIsGuest() {
        // given
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        ReviewBookReader bookReader = mock(ReviewBookReader.class);
        ReviewMemberReader memberReader = mock(ReviewMemberReader.class);
        when(memberReader.findByActorIds(List.of(7L))).thenReturn(Map.of(
                7L,
                new ReviewMemberProfile(
                        "게스트",
                        null,
                        "다정한 참새",
                        true,
                        false,
                        ActorType.GUEST
                )
        ));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            ReflectionTestUtils.setField(review, "id", 10L);
            return review;
        });
        ReviewService service = new ReviewService(
                reviewRepository,
                mock(ReplyRepository.class),
                mock(ReviewReactionRepository.class),
                mock(ReplyReactionRepository.class),
                () -> CurrentActor.guest(7L),
                bookReader,
                memberReader,
                mock(BookResolver.class),
                transactionManager(),
                eventPublisher
        );

        // when
        ReviewResponse response = service.createReview(
                5L,
                new ReviewCreateRequest(
                        "게스트 감상",
                        null,
                        null,
                        false
                )
        );

        // then
        assertThat(response.author().displayName()).isEqualTo("다정한 참새");
        assertThat(response.author().profileImageUrl()).isNull();
        assertThat(response.author().anonymous()).isTrue();
        assertThat(response.author().mine()).isTrue();
        assertThat(response.author().actorType()).isEqualTo(ActorType.GUEST);
        assertThat(response.author().memberId()).isNull();
        assertThat(response.author().profileStatus()).isEqualTo(AuthorProfileStatus.UNAVAILABLE);
    }



    @Test
    @DisplayName("감상과 답글 좋아요는 현재 Actor에 귀속된다")
    void should_AssignReactions_When_CurrentActorLikesReviewOrReply() {
        // given
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        ReplyRepository replyRepository = mock(ReplyRepository.class);
        ReviewReactionRepository reviewReactionRepository = mock(ReviewReactionRepository.class);
        ReplyReactionRepository replyReactionRepository = mock(ReplyReactionRepository.class);
        Review review = Review.create(5L, 1L, "감상", null, null, false, false);
        Reply reply = Reply.create(10L, 1L, "답글", false);
        ReflectionTestUtils.setField(review, "id", 10L);
        ReflectionTestUtils.setField(reply, "id", 20L);
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));
        when(replyRepository.findById(20L)).thenReturn(Optional.of(reply));
        when(reviewReactionRepository.countByReviewId(10L)).thenReturn(1L);
        when(replyReactionRepository.countByReplyId(20L)).thenReturn(1L);
        CurrentActorProvider currentActorProvider = () -> CurrentActor.member(7L, 99L);
        ReviewService service = new ReviewService(
                reviewRepository,
                replyRepository,
                reviewReactionRepository,
                replyReactionRepository,
                currentActorProvider,
                mock(ReviewBookReader.class),
                memberReader(false),
                mock(BookResolver.class),
                transactionManager(),
                eventPublisher
        );

        ArgumentCaptor<ReviewReaction> reviewReaction = ArgumentCaptor.forClass(ReviewReaction.class);
        ArgumentCaptor<ReplyReaction> replyReaction = ArgumentCaptor.forClass(ReplyReaction.class);

        // when
        service.createReviewReaction(10L);
        service.createReplyReaction(20L);

        // then
        verify(reviewReactionRepository).saveAndFlush(reviewReaction.capture());
        verify(replyReactionRepository).saveAndFlush(replyReaction.capture());
        assertThat(reviewReaction.getValue().getActorId()).isEqualTo(7L);
        assertThat(replyReaction.getValue().getActorId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("감상 작성 시 책을 검증하고 회원 익명 설정을 snapshot으로 저장한다")
    void should_ValidateBookAndSnapshotAnonymousSetting_When_CreatingReview() {
        // given
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        ReviewBookReader bookReader = mock(ReviewBookReader.class);
        ReviewMemberReader memberReader = mock(ReviewMemberReader.class);
        when(memberReader.findByActorIds(List.of(1L))).thenReturn(Map.of(
                1L,
                new ReviewMemberProfile(
                        "닉네임",
                        "profile",
                        "참새-a1b2c3d4",
                        true,
                        false,
                        ActorType.MEMBER
                )
        ));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            ReflectionTestUtils.setField(review, "id", 10L);
            return review;
        });
        ReviewService reviewService = reviewService(reviewRepository, bookReader, memberReader);

        // when
        ReviewResponse actual = reviewService.createReview(
                5L,
                new ReviewCreateRequest(
                        "감상",
                        null,
                        null,
                        false
                )
        );

        // then
        verify(bookReader).validateBookExists(5L);
        verify(memberReader).findByActorIds(List.of(1L));
        verify(eventPublisher).publishEvent(new BookActivityChangedEvent(5L));
        assertThat(actual.author().anonymous()).isTrue();
        assertThat(actual.author().displayName()).isEqualTo("참새-a1b2c3d4");
        assertThat(actual.author().memberId()).isNull();
        assertThat(actual.author().profileStatus()).isEqualTo(AuthorProfileStatus.UNAVAILABLE);
    }

    @Test
    @DisplayName("ISBN13으로 감상을 작성하면 해소된 도서 ID와 감상을 반환한다")
    void should_ReturnResolvedBookIdAndReview_When_CreatingReviewByIsbn13() {
        // given
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        ReviewBookReader bookReader = mock(ReviewBookReader.class);
        BookResolver bookResolver = mock(BookResolver.class);
        Book book = mock(Book.class);
        when(book.getId()).thenReturn(5L);
        Isbn13 isbn13 = new Isbn13("9788925568683");
        when(bookResolver.findOrCreate(isbn13)).thenReturn(book);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            ReflectionTestUtils.setField(review, "id", 10L);
            return review;
        });
        ReviewService reviewService = reviewService(
                reviewRepository,
                bookReader,
                memberReader(false),
                bookResolver
        );

        // when
        ReviewCreateByIsbnResponse actual = reviewService.createReviewByIsbn13(
                isbn13,
                new ReviewCreateRequest(
                        "감상",
                        null,
                        null,
                        false
                )
        );

        // then
        assertThat(actual.bookId()).isEqualTo(5L);
        assertThat(actual.review().reviewId()).isEqualTo(10L);
        verify(bookResolver).findOrCreate(isbn13);
        verify(bookReader).validateBookExists(5L);
        verify(eventPublisher).publishEvent(new BookActivityChangedEvent(5L));
    }

    @Test
    @DisplayName("감상을 삭제하면 해당 책의 활동 수 변경 이벤트를 발행한다")
    void should_PublishBookActivityChangedEvent_When_DeletingReview() {
        // given
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        Review review = Review.create(5L, 1L, "감상", null, null, false, false);
        ReflectionTestUtils.setField(review, "id", 17L);
        when(reviewRepository.findById(17L)).thenReturn(Optional.of(review));
        ReviewService service = reviewService(
                reviewRepository,
                mock(ReviewBookReader.class),
                memberReader(false)
        );

        // when
        service.deleteReview(17L);

        // then
        assertThat(review.isDeleted()).isTrue();
        verify(eventPublisher).publishEvent(new BookActivityChangedEvent(5L));
    }

    @Test
    @DisplayName("답글을 작성하면 부모 감상이 속한 책의 활동 수 변경 이벤트를 발행한다")
    void should_PublishBookActivityChangedEvent_When_CreatingReply() {
        // given
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        ReplyRepository replyRepository = mock(ReplyRepository.class);
        Review review = Review.create(5L, 1L, "감상", null, null, false, false);
        ReflectionTestUtils.setField(review, "id", 17L);
        when(reviewRepository.findById(17L)).thenReturn(Optional.of(review));
        when(replyRepository.save(any(Reply.class))).thenAnswer(invocation -> {
            Reply reply = invocation.getArgument(0);
            ReflectionTestUtils.setField(reply, "id", 99L);
            return reply;
        });
        ReviewService service = reviewService(reviewRepository, replyRepository);

        // when
        service.createReply(17L, new ReplyCreateRequest("답글"));

        // then
        verify(eventPublisher).publishEvent(new BookActivityChangedEvent(5L));
    }

    @Test
    @DisplayName("답글을 삭제하면 답글 ID가 아닌 부모 감상의 ID로 책을 찾아 이벤트를 발행한다")
    void should_PublishParentBookActivityChangedEvent_When_DeletingReply() {
        // given
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        ReplyRepository replyRepository = mock(ReplyRepository.class);
        Review review = Review.create(5L, 1L, "감상", null, null, false, false);
        Reply reply = Reply.create(17L, 1L, "답글", false);
        ReflectionTestUtils.setField(review, "id", 17L);
        ReflectionTestUtils.setField(reply, "id", 99L);
        when(reviewRepository.findById(17L)).thenReturn(Optional.of(review));
        when(replyRepository.findById(99L)).thenReturn(Optional.of(reply));
        ReviewService service = reviewService(reviewRepository, replyRepository);

        // when
        service.deleteReply(99L);

        // then
        assertThat(reply.isDeleted()).isTrue();
        verify(reviewRepository).findById(17L);
        verify(eventPublisher).publishEvent(new BookActivityChangedEvent(5L));
    }




    @Test
    @DisplayName("도서별 댓글 수는 감상과 답글을 합산하고 항목이 없으면 0을 반환한다")
    void should_SumReviewsAndRepliesAndKeepZero_When_ReadingBookCommentCounts() {
        // given
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        ReplyRepository replyRepository = mock(ReplyRepository.class);
        ReviewRepository.BookCommentCount reviewCount = mock(ReviewRepository.BookCommentCount.class);
        ReplyRepository.BookCommentCount replyCount = mock(ReplyRepository.BookCommentCount.class);
        when(reviewCount.getBookId()).thenReturn(5L);
        when(reviewCount.getCount()).thenReturn(2L);
        when(replyCount.getBookId()).thenReturn(5L);
        when(replyCount.getCount()).thenReturn(3L);
        when(reviewRepository.countByBookIdInGroupByBookId(List.of(5L, 6L)))
                .thenReturn(List.of(reviewCount));
        when(replyRepository.countByReviewBookIdInGroupByBookId(List.of(5L, 6L)))
                .thenReturn(List.of(replyCount));
        ReviewService reviewService = new ReviewService(
                reviewRepository,
                replyRepository,
                mock(ReviewReactionRepository.class),
                mock(ReplyReactionRepository.class),
                currentActorProvider(),
                mock(ReviewBookReader.class),
                memberReader(false),
                mock(BookResolver.class),
                transactionManager(),
                eventPublisher
        );

        // when
        Map<Long, Long> counts = reviewService.getCommentCounts(List.of(5L, 6L));

        // then
        assertThat(counts).containsEntry(5L, 5L).containsEntry(6L, 0L);
    }

    @Test
    @DisplayName("도서별 감상과 답글 수를 분리해서 반환한다")
    void should_ReturnSeparatedReviewAndReplyCounts_When_ReadingBookActivityCounts() {
        // given
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        ReplyRepository replyRepository = mock(ReplyRepository.class);
        ReviewRepository.BookCommentCount reviewCount = mock(ReviewRepository.BookCommentCount.class);
        ReplyRepository.BookCommentCount replyCount = mock(ReplyRepository.BookCommentCount.class);
        when(reviewCount.getBookId()).thenReturn(5L);
        when(reviewCount.getCount()).thenReturn(2L);
        when(replyCount.getBookId()).thenReturn(5L);
        when(replyCount.getCount()).thenReturn(3L);
        when(reviewRepository.countByBookIdInGroupByBookId(List.of(5L, 6L)))
                .thenReturn(List.of(reviewCount));
        when(replyRepository.countByReviewBookIdInGroupByBookId(List.of(5L, 6L)))
                .thenReturn(List.of(replyCount));
        ReviewService reviewService = new ReviewService(
                reviewRepository,
                replyRepository,
                mock(ReviewReactionRepository.class),
                mock(ReplyReactionRepository.class),
                currentActorProvider(),
                mock(ReviewBookReader.class),
                memberReader(false),
                mock(BookResolver.class),
                transactionManager(),
                eventPublisher
        );

        // when
        Map<Long, ActivityCounts> actual = reviewService.getActivityCounts(List.of(5L, 6L));

        // then
        assertThat(actual).containsEntry(
                5L,
                new ActivityCounts(
                        2L,
                        3L
                )
        )
                .containsEntry(
                6L,
                new ActivityCounts(
                        0L,
                        0L
                )
        );
    }

    private ReviewService reviewService(
            ReviewRepository reviewRepository,
            ReviewBookReader bookReader,
            ReviewMemberReader memberReader
    ) {
        return reviewService(
                reviewRepository,
                bookReader,
                memberReader,
                mock(BookResolver.class)
        );
    }

    private ReviewService reviewService(
            ReviewRepository reviewRepository,
            ReplyRepository replyRepository
    ) {
        return new ReviewService(
                reviewRepository,
                replyRepository,
                mock(ReviewReactionRepository.class),
                mock(ReplyReactionRepository.class),
                currentActorProvider(),
                mock(ReviewBookReader.class),
                memberReader(false),
                mock(BookResolver.class),
                transactionManager(),
                eventPublisher
        );
    }

    private ReviewService reviewService(
            ReviewRepository reviewRepository,
            ReviewBookReader bookReader,
            ReviewMemberReader memberReader,
            BookResolver bookResolver
    ) {
        return new ReviewService(
                reviewRepository,
                mock(ReplyRepository.class),
                mock(ReviewReactionRepository.class),
                mock(ReplyReactionRepository.class),
                currentActorProvider(),
                bookReader,
                memberReader,
                bookResolver,
                transactionManager(),
                eventPublisher
        );
    }

    private PlatformTransactionManager transactionManager() {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        return transactionManager;
    }

    private ReviewMemberReader memberReader(boolean anonymousEnabled) {
        return actorIds -> actorIds.stream()
                .collect(Collectors.toMap(
                        actorId -> actorId,
                        actorId -> new ReviewMemberProfile(
                                "닉네임",
                                "profile",
                                "참새-a1b2c3d4",
                                anonymousEnabled,
                                false,
                                ActorType.MEMBER
                        )
                ));
    }

    private CurrentActorProvider currentActorProvider() {
        return () -> CurrentActor.member(1L, 1L);
    }
}
