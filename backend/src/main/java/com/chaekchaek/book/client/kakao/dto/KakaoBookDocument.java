package com.chaekchaek.book.client.kakao.dto;

import java.util.List;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record KakaoBookDocument(
        String title,
        String thumbnail,
        List<String> authors,
        List<String> translators,
        String publisher,
        String datetime,
        String isbn
) {
    public KakaoBookDocument {
        authors = authors == null ? List.of() : List.copyOf(authors);
        translators = translators == null ? List.of() : List.copyOf(translators);
    }
}
