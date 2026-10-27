package com.chaekchaek.book.cache;

import com.chaekchaek.library.service.BookActivityCountReader.ActivityCounts;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BookActivityCountCacheConfiguration {

    @Bean
    public Cache<Long, ActivityCounts> bookActivityCountCache() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(30))
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
