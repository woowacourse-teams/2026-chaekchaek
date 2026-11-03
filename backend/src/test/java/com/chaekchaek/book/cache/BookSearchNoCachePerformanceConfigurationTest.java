package com.chaekchaek.book.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.service.BookSearchActivityCountReader;
import com.chaekchaek.library.service.BookActivityCountReader;
import com.chaekchaek.library.service.BookActivityCountReader.ActivityCounts;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class BookSearchNoCachePerformanceConfigurationTest {

    @Test
    @DisplayName("측정용 프로필에서는 검색 활동 수를 DB에서 직접 조회한다")
    void should_UseDatabaseReader_When_NoCacheProfileIsActive() {
        // given
        BookActivityCountReader databaseReader = mock(BookActivityCountReader.class);
        ActivityCounts counts = new ActivityCounts(2L, 3L);
        when(databaseReader.getActivityCounts(List.of(42L))).thenReturn(Map.of(42L, counts));

        try (AnnotationConfigApplicationContext context = context(databaseReader, true)) {
            BookSearchActivityCountReader reader = context.getBean(BookSearchActivityCountReader.class);

            // when
            assertThat(reader.getActivityCounts(List.of(42L))).containsEntry(42L, counts);
            assertThat(reader.getActivityCounts(List.of(42L))).containsEntry(42L, counts);

            // then
            verify(databaseReader, org.mockito.Mockito.times(2)).getActivityCounts(List.of(42L));
        }
    }

    @Test
    @DisplayName("기본 프로필에서는 검색 활동 수 캐시를 사용한다")
    void should_UseCachedReader_When_NoCacheProfileIsInactive() {
        // given
        BookActivityCountReader databaseReader = mock(BookActivityCountReader.class);
        ActivityCounts counts = new ActivityCounts(2L, 3L);
        when(databaseReader.getActivityCounts(List.of(42L))).thenReturn(Map.of(42L, counts));

        try (AnnotationConfigApplicationContext context = context(databaseReader, false)) {
            BookSearchActivityCountReader reader = context.getBean(BookSearchActivityCountReader.class);

            // when
            assertThat(reader.getActivityCounts(List.of(42L))).containsEntry(42L, counts);
            assertThat(reader.getActivityCounts(List.of(42L))).containsEntry(42L, counts);

            // then
            verify(databaseReader).getActivityCounts(List.of(42L));
        }
    }

    private AnnotationConfigApplicationContext context(BookActivityCountReader databaseReader, boolean noCache) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        if (noCache) {
            context.getEnvironment().setActiveProfiles("perf-no-cache");
        }
        context.registerBean(BookActivityCountReader.class, () -> databaseReader);
        context.registerBean(BookActivityCountCacheCoordinator.class,
                () -> new BookActivityCountCacheCoordinator(Caffeine.newBuilder().build()));
        context.register(CachedBookSearchActivityCountReader.class,
                BookSearchNoCachePerformanceConfiguration.class);
        context.refresh();
        return context;
    }
}
