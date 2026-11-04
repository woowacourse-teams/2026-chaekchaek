package com.chaekchaek.book.cache;

import com.chaekchaek.book.service.BookSearchActivityCountReader;
import com.chaekchaek.library.service.BookActivityCountReader;
import com.chaekchaek.library.service.BookActivityCountReader.ActivityCounts;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CachedBookSearchActivityCountReader implements BookSearchActivityCountReader {

    private final BookActivityCountReader databaseReader;
    private final BookActivityCountCacheCoordinator cacheCoordinator;

    @Override
    public Map<Long, ActivityCounts> getActivityCounts(Collection<Long> bookIds) {
        if (bookIds.isEmpty()) {
            return Map.of();
        }

        Set<Long> uniqueBookIds = new LinkedHashSet<>(bookIds);
        Map<Long, ActivityCounts> result = new HashMap<>();
        List<Long> cacheMissBookIds = new ArrayList<>();

        for (Long uniqueBookId : uniqueBookIds) {
            ActivityCounts cached = cacheCoordinator.getIfPresent(uniqueBookId);
            if (cached == null) {
                cacheMissBookIds.add(uniqueBookId);
            } else {
                result.put(uniqueBookId, cached);
            }
        }

        if (!cacheMissBookIds.isEmpty()) {
            long generation = cacheCoordinator.generation();
            Map<Long, ActivityCounts> loadedCounts = databaseReader.getActivityCounts(cacheMissBookIds);
            Map<Long, ActivityCounts> countsToCache = new HashMap<>();

            for (Long cacheMissBookId : cacheMissBookIds) {
                ActivityCounts counts = loadedCounts.getOrDefault(
                        cacheMissBookId,
                        ActivityCounts.ZERO
                );
                countsToCache.put(cacheMissBookId, counts);
                result.put(cacheMissBookId, counts);
            }
            cacheCoordinator.putIfUnchanged(generation, countsToCache);
        }

        return Map.copyOf(result);
    }
}
