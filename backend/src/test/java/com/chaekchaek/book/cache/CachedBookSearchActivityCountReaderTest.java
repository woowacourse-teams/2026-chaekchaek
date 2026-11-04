package com.chaekchaek.book.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chaekchaek.library.service.BookActivityCountReader;
import com.chaekchaek.library.service.BookActivityCountReader.ActivityCounts;
import com.chaekchaek.review.event.BookActivityChangedEvent;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CachedBookSearchActivityCountReaderTest {

    @Test
    @DisplayName("책 ID가 비어 있으면 DB를 조회하지 않는다")
    void should_ReturnEmptyMapWithoutDatabaseQuery_When_BookIdsAreEmpty() {
        BookActivityCountReader databaseReader = mock(BookActivityCountReader.class);
        CachedBookSearchActivityCountReader reader = reader(databaseReader, newCache());

        assertThat(reader.getActivityCounts(List.of())).isEmpty();

        verifyNoInteractions(databaseReader);
    }

    @Test
    @DisplayName("캐시 미스 책을 한 번에 조회하고 이후 요청에서는 캐시 값을 반환한다")
    void should_LoadAllCacheMissesTogetherAndReuseCachedCounts_When_RequestedAgain() {
        BookActivityCountReader databaseReader = mock(BookActivityCountReader.class);
        Cache<Long, ActivityCounts> cache = newCache();
        CachedBookSearchActivityCountReader reader = reader(databaseReader, cache);
        Map<Long, ActivityCounts> expected = Map.of(
                1L, new ActivityCounts(3L, 4L),
                2L, new ActivityCounts(5L, 6L)
        );
        when(databaseReader.getActivityCounts(List.of(1L, 2L))).thenReturn(expected);

        assertThat(reader.getActivityCounts(List.of(1L, 2L, 1L))).isEqualTo(expected);
        assertThat(reader.getActivityCounts(List.of(1L, 2L))).isEqualTo(expected);

        verify(databaseReader).getActivityCounts(List.of(1L, 2L));
        verify(databaseReader, never()).getActivityCounts(List.of(1L, 2L, 1L));
    }

    @Test
    @DisplayName("일부만 캐시된 경우 미스 항목만 조회하고 DB 결과가 없는 책은 0으로 캐싱한다")
    void should_LoadOnlyMissesAndCacheZeroCounts_When_SomeBooksAreAlreadyCached() {
        BookActivityCountReader databaseReader = mock(BookActivityCountReader.class);
        Cache<Long, ActivityCounts> cache = newCache();
        ActivityCounts cachedCounts = new ActivityCounts(7L, 8L);
        ActivityCounts loadedCounts = new ActivityCounts(2L, 3L);
        cache.put(1L, cachedCounts);
        when(databaseReader.getActivityCounts(List.of(2L, 3L)))
                .thenReturn(Map.of(2L, loadedCounts));
        CachedBookSearchActivityCountReader reader = reader(databaseReader, cache);
        Map<Long, ActivityCounts> expected = Map.of(
                1L, cachedCounts,
                2L, loadedCounts,
                3L, ActivityCounts.ZERO
        );

        assertThat(reader.getActivityCounts(List.of(1L, 2L, 3L))).isEqualTo(expected);
        assertThat(reader.getActivityCounts(List.of(1L, 2L, 3L))).isEqualTo(expected);

        verify(databaseReader).getActivityCounts(List.of(2L, 3L));
        assertThat(cache.getIfPresent(3L)).isEqualTo(ActivityCounts.ZERO);
    }

    @Test
    @DisplayName("여러 검색 결과에 공통으로 포함된 책은 재사용하고 새 책만 조회한다")
    void should_ReuseCommonBookAndLoadOnlyNewBook_When_SearchResultsOverlap() {
        BookActivityCountReader databaseReader = mock(BookActivityCountReader.class);
        Cache<Long, ActivityCounts> cache = newCache();
        CachedBookSearchActivityCountReader reader = reader(databaseReader, cache);
        ActivityCounts firstBookCounts = new ActivityCounts(2L, 3L);
        ActivityCounts secondBookCounts = new ActivityCounts(4L, 5L);
        ActivityCounts thirdBookCounts = new ActivityCounts(0L, 0L);
        when(databaseReader.getActivityCounts(List.of(1L, 2L)))
                .thenReturn(Map.of(1L, firstBookCounts, 2L, secondBookCounts));
        when(databaseReader.getActivityCounts(List.of(3L)))
                .thenReturn(Map.of(3L, thirdBookCounts));

        assertThat(reader.getActivityCounts(List.of(1L, 2L)))
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        1L, firstBookCounts,
                        2L, secondBookCounts
                ));
        assertThat(reader.getActivityCounts(List.of(2L, 3L)))
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        2L, secondBookCounts,
                        3L, thirdBookCounts
                ));

        verify(databaseReader).getActivityCounts(List.of(1L, 2L));
        verify(databaseReader).getActivityCounts(List.of(3L));
        verifyNoMoreInteractions(databaseReader);
    }

    @Test
    @DisplayName("TTL이 지나면 캐시 미스로 DB를 다시 조회한다")
    void should_QueryDatabaseAgain_When_CacheEntryExpires() {
        BookActivityCountCacheConfiguration configuration = new BookActivityCountCacheConfiguration();
        AtomicLong tickerNanos = new AtomicLong();
        Cache<Long, ActivityCounts> cache = configuration.createCache(tickerNanos::get);
        BookActivityCountReader databaseReader = mock(BookActivityCountReader.class);
        ActivityCounts firstCounts = new ActivityCounts(1L, 2L);
        ActivityCounts refreshedCounts = new ActivityCounts(3L, 4L);
        AtomicInteger databaseCallCount = new AtomicInteger();
        when(databaseReader.getActivityCounts(List.of(1L)))
                .thenAnswer(invocation -> databaseCallCount.getAndIncrement() == 0
                        ? Map.of(1L, firstCounts)
                        : Map.of(1L, refreshedCounts));
        CachedBookSearchActivityCountReader reader = reader(databaseReader, cache);

        assertThat(reader.getActivityCounts(List.of(1L))).containsEntry(1L, firstCounts);

        tickerNanos.addAndGet(BookActivityCountCacheConfiguration.CACHE_TTL.plusNanos(1).toNanos());

        assertThat(reader.getActivityCounts(List.of(1L))).containsEntry(1L, refreshedCounts);
        verify(databaseReader, org.mockito.Mockito.times(2)).getActivityCounts(List.of(1L));
    }

    @Test
    @DisplayName("DB 조회 후 캐시 저장 전에 무효화되면 오래된 값을 캐시에 남기지 않는다")
    void should_NotCacheStaleCounts_When_CacheIsInvalidatedAfterDatabaseRead() throws Exception {
        // given
        BookActivityCountReader databaseReader = mock(BookActivityCountReader.class);
        Cache<Long, ActivityCounts> cache = newCache();
        BookActivityCountCacheCoordinator coordinator = new BookActivityCountCacheCoordinator(cache);
        CachedBookSearchActivityCountReader reader = new CachedBookSearchActivityCountReader(databaseReader, coordinator);
        BookActivityCountCacheInvalidationListener listener = new BookActivityCountCacheInvalidationListener(coordinator);
        ActivityCounts staleCounts = new ActivityCounts(1L, 2L);
        ActivityCounts updatedCounts = new ActivityCounts(2L, 2L);
        CountDownLatch databaseReadFinished = new CountDownLatch(1);
        CountDownLatch continueSearch = new CountDownLatch(1);
        AtomicInteger databaseCallCount = new AtomicInteger();
        when(databaseReader.getActivityCounts(List.of(42L))).thenAnswer(invocation -> {
            if (databaseCallCount.getAndIncrement() == 0) {
                Map<Long, ActivityCounts> loadedCounts = Map.of(42L, staleCounts);
                databaseReadFinished.countDown();
                if (!continueSearch.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("검색 재개 신호를 받지 못했습니다");
                }
                return loadedCounts;
            }
            return Map.of(42L, updatedCounts);
        });

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // when
            Future<Map<Long, ActivityCounts>> search = executor.submit(
                    () -> reader.getActivityCounts(List.of(42L))
            );
            assertThat(databaseReadFinished.await(5, TimeUnit.SECONDS)).isTrue();
            listener.invalidate(new BookActivityChangedEvent(42L));
            continueSearch.countDown();
            assertThat(search.get(5, TimeUnit.SECONDS)).containsEntry(42L, staleCounts);

            // then
            assertThat(cache.getIfPresent(42L)).isNull();
            assertThat(reader.getActivityCounts(List.of(42L))).containsEntry(42L, updatedCounts);
            verify(databaseReader, org.mockito.Mockito.times(2)).getActivityCounts(List.of(42L));
        } finally {
            continueSearch.countDown();
            executor.shutdownNow();
        }
    }

    private CachedBookSearchActivityCountReader reader(
            BookActivityCountReader databaseReader,
            Cache<Long, ActivityCounts> cache
    ) {
        return new CachedBookSearchActivityCountReader(
                databaseReader,
                new BookActivityCountCacheCoordinator(cache)
        );
    }

    private Cache<Long, ActivityCounts> newCache() {
        return Caffeine.<Long, ActivityCounts>newBuilder()
                .recordStats()
                .build();
    }
}
