package com.chaekchaek.book.cache;

import com.chaekchaek.library.service.BookActivityCountReader.ActivityCounts;
import com.chaekchaek.review.event.BookActivityChangedEvent;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class BookActivityCountCacheInvalidationListener {

    private final Cache<Long, ActivityCounts> bookActivityCountCache;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void invalidate(BookActivityChangedEvent event) {
        bookActivityCountCache.invalidate(event.bookId());
    }
}
