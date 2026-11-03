package com.chaekchaek.book.client.kakao.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record KakaoBookMeta(
        int totalCount,
        int pageableCount,
        boolean isEnd
) {
}
