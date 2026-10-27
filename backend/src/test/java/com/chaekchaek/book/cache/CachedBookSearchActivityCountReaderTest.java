package com.chaekchaek.book.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chaekchaek.library.service.BookActivityCountReader;
import com.chaekchaek.library.service.BookActivityCountReader.ActivityCounts;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import java.util.Map;
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

    private CachedBookSearchActivityCountReader reader(
            BookActivityCountReader databaseReader,
            Cache<Long, ActivityCounts> cache
    ) {
        return new CachedBookSearchActivityCountReader(databaseReader, cache);
    }

    private Cache<Long, ActivityCounts> newCache() {
        return Caffeine.<Long, ActivityCounts>newBuilder()
                .recordStats()
                .build();
    }
}
