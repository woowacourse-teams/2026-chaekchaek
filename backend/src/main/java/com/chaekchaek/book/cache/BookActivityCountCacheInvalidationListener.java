package com.chaekchaek.book.cache;

import com.chaekchaek.review.event.BookActivityChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class BookActivityCountCacheInvalidationListener {

    private final BookActivityCountCacheCoordinator cacheCoordinator;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void invalidate(BookActivityChangedEvent event) {
        cacheCoordinator.invalidate(event.bookId());
    }
}
