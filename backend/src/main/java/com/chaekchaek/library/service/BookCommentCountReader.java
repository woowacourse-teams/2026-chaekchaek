package com.chaekchaek.library.service;

import java.util.Collection;
import java.util.Map;

/**
 * Reads review and reply counts without coupling the Library feature to Review persistence.
 */
public interface BookCommentCountReader {

    Map<Long, Long> getCommentCounts(Collection<Long> bookIds);
}
