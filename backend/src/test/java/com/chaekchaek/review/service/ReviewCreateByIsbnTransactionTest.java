package com.chaekchaek.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.client.AladinBookClient;
import com.chaekchaek.book.client.dto.AladinBookItem;
import com.chaekchaek.book.client.dto.AladinBookSubInfo;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.book.service.BookResolver;
import com.chaekchaek.common.auth.ActorType;
import com.chaekchaek.common.auth.CurrentActor;
import com.chaekchaek.review.book.ReviewBookReader;
import com.chaekchaek.review.domain.Review;
import com.chaekchaek.review.dto.ReviewCreateRequest;
import com.chaekchaek.review.library.ReadingRecordCoordinator;
import com.chaekchaek.review.member.ReviewMemberProfile;
import com.chaekchaek.review.member.ReviewMemberReader;
import com.chaekchaek.review.repository.ReplyReactionRepository;
import com.chaekchaek.review.repository.ReplyRepository;
import com.chaekchaek.review.repository.ReviewReactionRepository;
import com.chaekchaek.review.repository.ReviewRepository;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@DataJpaTest
class ReviewCreateByIsbnTransactionTest {

    private static final Isbn13 ISBN13 = new Isbn13("9788925568683");

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("ISBN13 감상 저장이 실패해도 자동 등록한 책은 유지한다")
    void should_KeepRegisteredBook_When_ReviewWriteFailsAfterResolvingIsbn13() {
        // given
        AtomicBoolean externalCallInTransaction = new AtomicBoolean(true);
        AtomicBoolean reviewSaveInTransaction = new AtomicBoolean(false);
        AladinBookClient bookClient = mock(AladinBookClient.class);
        when(bookClient.findBookByIsbn13(ISBN13)).thenAnswer(invocation -> {
            externalCallInTransaction.set(TransactionSynchronizationManager.isActualTransactionActive());
            return new AladinBookItem(
                    "마션", "https://image.example/martian.jpg", "앤디 위어 (지은이)", "책 설명",
                    "2026-01-01", ISBN13.value(), "SF", "알에이치코리아", new AladinBookSubInfo(308)
            );
        });
        BookResolver bookResolver = new BookResolver(bookClient, bookRepository, transactionManager);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            reviewSaveInTransaction.set(TransactionSynchronizationManager.isActualTransactionActive());
            throw new IllegalStateException("review write failed");
        });
        ReviewService reviewService = new ReviewService(
                reviewRepository, mock(ReplyRepository.class), mock(ReviewReactionRepository.class),
                mock(ReplyReactionRepository.class), () -> CurrentActor.guest(7L),
                mock(ReadingRecordCoordinator.class), requireStoredBook(), guestProfileReader(),
                bookResolver, transactionManager
        );

        // when & then
        assertThatThrownBy(() -> reviewService.createReviewByIsbn13(ISBN13,
                new ReviewCreateRequest("감상", null, null, null, null, false)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("review write failed");
        assertThat(externalCallInTransaction).isFalse();
        assertThat(reviewSaveInTransaction).isTrue();
        assertThat(bookRepository.findByIsbn13(ISBN13)).isPresent();
    }

    private ReviewBookReader requireStoredBook() {
        return bookId -> {
            if (bookRepository.findById(bookId).isEmpty()) {
                throw new IllegalStateException("book is not stored");
            }
        };
    }

    private ReviewMemberReader guestProfileReader() {
        return actorIds -> Map.of(7L,
                new ReviewMemberProfile("게스트", null, "다정한 참새", true, false, ActorType.GUEST));
    }
}
