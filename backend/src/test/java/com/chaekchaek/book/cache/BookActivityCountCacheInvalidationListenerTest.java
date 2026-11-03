package com.chaekchaek.book.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaekchaek.library.service.BookActivityCountReader.ActivityCounts;
import com.chaekchaek.review.event.BookActivityChangedEvent;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@ActiveProfiles("test")
@Import({BookActivityCountCacheConfiguration.class, BookActivityCountCacheInvalidationListener.class})
class BookActivityCountCacheInvalidationListenerTest {

    @Autowired
    private Cache<Long, ActivityCounts> cache;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        cache.invalidateAll();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("책 활동 변경이 커밋되면 해당 책의 캐시만 비운다")
    void should_InvalidateOnlyChangedBook_When_TransactionCommits() {
        // given
        ActivityCounts changedBookCounts = new ActivityCounts(1L, 2L);
        ActivityCounts otherBookCounts = new ActivityCounts(3L, 4L);
        cache.put(42L, changedBookCounts);
        cache.put(99L, otherBookCounts);

        // when
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            eventPublisher.publishEvent(new BookActivityChangedEvent(42L));
            assertThat(cache.getIfPresent(42L)).isEqualTo(changedBookCounts);
        });

        // then
        assertThat(cache.getIfPresent(42L)).isNull();
        assertThat(cache.getIfPresent(99L)).isEqualTo(otherBookCounts);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("책 활동 변경이 롤백되면 캐시 값을 유지한다")
    void should_KeepCachedCounts_When_TransactionRollsBack() {
        // given
        ActivityCounts counts = new ActivityCounts(1L, 2L);
        cache.put(42L, counts);

        // when
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            eventPublisher.publishEvent(new BookActivityChangedEvent(42L));
            status.setRollbackOnly();
        });

        // then
        assertThat(cache.getIfPresent(42L)).isEqualTo(counts);
    }
}
