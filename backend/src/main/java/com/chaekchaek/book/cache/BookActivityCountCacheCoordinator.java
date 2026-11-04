package com.chaekchaek.book.cache;

import com.chaekchaek.library.service.BookActivityCountReader.ActivityCounts;
import com.github.benmanes.caffeine.cache.Cache;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookActivityCountCacheCoordinator {

    private final Cache<Long, ActivityCounts> cache;
    private long generation;

    public ActivityCounts getIfPresent(Long bookId) {
        return cache.getIfPresent(bookId);
    }

    public synchronized long generation() {
        return generation;
    }

    public synchronized void putIfUnchanged(long expectedGeneration, Map<Long, ActivityCounts> counts) {
        if (generation != expectedGeneration) {
            return;
        }
        counts.forEach(cache::put);
    }

    public synchronized void invalidate(Long bookId) {
        generation++;
        cache.invalidate(bookId);
    }
}
