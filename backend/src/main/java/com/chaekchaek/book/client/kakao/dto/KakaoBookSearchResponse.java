package com.chaekchaek.book.client.kakao.dto;

import java.util.List;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record KakaoBookSearchResponse(
        KakaoBookMeta meta,
        List<KakaoBookDocument> documents
) {
    public KakaoBookSearchResponse {
        documents = documents == null ? List.of() : List.copyOf(documents);
    }
}
