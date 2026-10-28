package com.chaekchaek.book.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaekchaek.library.service.BookActivityCountReader.ActivityCounts;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Statistic;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BookActivityCountCacheMetricsTest {

    @Test
    @DisplayName("캐시 적중, 미스, 퇴출 통계를 Micrometer 지표로 제공한다")
    void should_ExposeHitMissAndEvictionMetrics() {
        Cache<Long, ActivityCounts> cache = Caffeine.<Long, ActivityCounts>newBuilder()
                .maximumSize(1)
                .recordStats()
                .build();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        new BookActivityCountCacheConfiguration()
                .bookActivityCountCacheMetrics(cache)
                .bindTo(registry);

        cache.getIfPresent(1L);
        cache.put(1L, new ActivityCounts(1L, 2L));
        cache.getIfPresent(1L);
        cache.put(2L, new ActivityCounts(3L, 4L));
        cache.cleanUp();

        assertThat(metricValue(registry, "cache.gets", "hit")).isEqualTo(1.0);
        assertThat(metricValue(registry, "cache.gets", "miss")).isEqualTo(1.0);
        assertThat(metricValue(registry, "cache.evictions", null)).isEqualTo(1.0);
    }

    private double metricValue(SimpleMeterRegistry registry, String metricName, String result) {
        var search = registry.get(metricName).tag("cache", "bookActivityCount");
        if (result != null) {
            search.tag("result", result);
        }
        for (Measurement measurement : search.meter().measure()) {
            if (measurement.getStatistic() == Statistic.COUNT) {
                return measurement.getValue();
            }
        }
        throw new AssertionError("COUNT measurement not found for " + metricName);
    }
}
