package com.chaekchaek.home.dto;

import java.util.List;

public record PopularBookResponse(long bookId, String title, String coverImageUrl, List<String> authors,
                                  long reviewCount, long replyCount) {
}
