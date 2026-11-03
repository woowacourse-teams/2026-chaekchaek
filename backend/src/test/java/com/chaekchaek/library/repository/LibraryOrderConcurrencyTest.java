package com.chaekchaek.library.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.library.domain.Library;
import com.chaekchaek.library.domain.LibraryBook;
import com.chaekchaek.library.service.LibraryOrderService;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@Import(LibraryOrderService.class)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LibraryOrderConcurrencyTest {

    @Autowired
    private LibraryOrderService service;
    @Autowired
    private MemberRepository members;
    @Autowired
    private LibraryRepository libraries;
    @Autowired
    private LibraryBookRepository placements;
    @Autowired
    private PlatformTransactionManager transactionManager;
    private TransactionTemplate transaction;
    private long memberId;
    private long libraryId;
    private List<Long> libraryIds;
    private final List<Long> bookIds = List.of(10L, 20L);

    @BeforeEach
    void setUp() {
        transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            Member member = members.saveAndFlush(Member.create("순서경쟁", null, LocalDateTime.now()));
            memberId = member.getId();
            Library first = libraries.saveAndFlush(Library.create(memberId, "첫째", Instant.now()));
            Library second = Library.create(memberId, "둘째", Instant.now());
            second.changeDisplayOrder(2L);
            libraries.saveAndFlush(second);
            libraryId = first.getId();
            libraryIds = List.of(first.getId(), second.getId());
            placements.saveAndFlush(LibraryBook.create(libraryId, 10L, 1L));
            placements.saveAndFlush(LibraryBook.create(libraryId, 20L, 2L));
        });
    }

    @AfterEach
    void cleanUp() {
        transaction.executeWithoutResult(status -> {
            placements.deleteAllInBatch();
            libraries.deleteAllInBatch();
            members.deleteAllInBatch();
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("같은 집합의 순서 요청은 회원 잠금 대기 후 마지막 순서를 저장한다")
    void should_ApplyLastOrder_When_ConcurrentRequestsHaveSameIds(boolean books) throws Exception {
        // given
        List<Long> ids = books ? bookIds : libraryIds;

        // when
        race(books, () -> reorder(books, ids.reversed()), false);

        // then
        List<Long> stored = transaction.execute(status -> {
            if (books) {
                return placements.findAllByLibraryIdOrderByDisplayOrderAscIdAsc(libraryId)
                        .stream()
                        .map(LibraryBook::getBookId)
                        .toList();
            }
            return libraries.findAllByMemberIdOrderByDisplayOrderAscIdAsc(memberId)
                    .stream()
                    .map(Library::getId)
                    .toList();
        });
        assertThat(stored).containsExactlyElementsOf(ids);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("추가가 먼저 완료되면 잠금 대기한 이전 집합의 순서 요청은 충돌한다")
    void should_RejectStaleOrder_When_ConcurrentAdditionCommitsFirst(boolean books) throws Exception {
        // when & then
        race(books, () -> {
            if (books) {
                placements.saveAndFlush(LibraryBook.create(libraryId, 30L, 3L));
                return;
            }
            Library third = Library.create(memberId, "셋째", Instant.now());
            third.changeDisplayOrder(3L);
            libraries.saveAndFlush(third);
        }, true);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("삭제가 먼저 완료되면 잠금 대기한 이전 집합의 순서 요청은 충돌한다")
    void should_RejectStaleOrder_When_ConcurrentDeletionCommitsFirst(boolean books) throws Exception {
        // when & then
        race(books, () -> {
            if (books) {
                placements.delete(placements.findByLibraryIdAndBookId(libraryId, 20L).orElseThrow());
                placements.flush();
                return;
            }
            libraries.deleteById(libraryIds.getLast());
            libraries.flush();
        }, true);
    }

    private void race(boolean books, Runnable firstWrite, boolean conflict) throws Exception {
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> first = executor.submit(() -> transaction.executeWithoutResult(status -> {
                members.findByIdForUpdate(memberId).orElseThrow();
                locked.countDown();
                await(release);
                firstWrite.run();
            }));
            assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
            Future<?> second = executor.submit(() -> {
                secondStarted.countDown();
                reorder(books, books ? bookIds : libraryIds);
            });
            try {
                assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> second.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            } finally {
                release.countDown();
            }
            first.get(5, TimeUnit.SECONDS);
            if (conflict) {
                assertThatThrownBy(() -> second.get(5, TimeUnit.SECONDS))
                        .isInstanceOfSatisfying(ExecutionException.class, exception ->
                                assertThat(exception.getCause()).isInstanceOfSatisfying(BusinessException.class,
                                        cause -> assertThat(cause.getErrorCode()).isEqualTo(ErrorCode.ORDER_CONFLICT)));
                return;
            }
            second.get(5, TimeUnit.SECONDS);
        } finally {
            release.countDown();
        }
    }

    private void reorder(boolean books, List<Long> ids) {
        if (books) {
            service.reorderBooks(memberId, libraryId, ids);
            return;
        }
        service.reorderLibraries(memberId, ids);
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent write did not release its member lock");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
