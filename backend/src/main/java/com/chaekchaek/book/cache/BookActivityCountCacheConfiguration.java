package com.chaekchaek.book.cache;

import com.chaekchaek.library.service.BookActivityCountReader.ActivityCounts;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BookActivityCountCacheConfiguration {

    static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final long MAXIMUM_SIZE = 500;

    @Bean
    public Cache<Long, ActivityCounts> bookActivityCountCache() {
        return createCache(Ticker.systemTicker());
    }

    Cache<Long, ActivityCounts> createCache(Ticker ticker) {
        return Caffeine.newBuilder()
                .maximumSize(MAXIMUM_SIZE)
                .expireAfterWrite(CACHE_TTL)
                .ticker(ticker)
                .recordStats()
                .build();
    }

    @Bean
    public MeterBinder bookActivityCountCacheMetrics(
            Cache<Long, ActivityCounts> bookActivityCountCache
    ) {
        return registry -> CaffeineCacheMetrics.monitor(
                registry,
                bookActivityCountCache,
                "bookActivityCount"
        );
    }
}
