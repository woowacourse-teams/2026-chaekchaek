package com.chaekchaek.admin.dto;

import java.time.Instant;
import java.util.List;

public record RecommendedBookResponse(long bookId, String isbn13, String title, String coverImageUrl,
                                      List<String> authors, Instant createdAt) {
}
