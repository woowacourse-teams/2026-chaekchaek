package com.chaekchaek.book.service;

import com.chaekchaek.library.service.BookActivityCountReader.ActivityCounts;
import java.util.Collection;
import java.util.Map;

public interface BookSearchActivityCountReader {

    Map<Long, ActivityCounts> getActivityCounts(Collection<Long> bookIds);
}
