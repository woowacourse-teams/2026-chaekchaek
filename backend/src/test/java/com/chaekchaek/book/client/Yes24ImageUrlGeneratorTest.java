package com.chaekchaek.book.client;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

public class Yes24ImageUrlGeneratorTest {

    @Test
    @DisplayName("Yes24 커버 URL에서 책등과 뒷면 URL을 생성한다")
    void should_CreateSpineAndBackImageUrls() {
        String coverImageUrl =
                "https://image.yes24.com/goods/118578901/xL";

        Yes24ImageUrls result =
                Yes24ImageUrlGenerator.fromCoverUrl(coverImageUrl);

        assertThat(result).isEqualTo(new Yes24ImageUrls(
                "https://image.yes24.com/goods/118578901/side",
                "https://image.yes24.com/goods/118578901/back"
        ));
    }

    @Test
    @DisplayName("커버 이미지 경로가 달라도 마지막 경로를 변경한다")
    void should_CreateUrlsRegardlessOfCoverImageType() {
        String coverImageUrl =
                "https://image.yes24.com/goods/118578901/L";

        Yes24ImageUrls result =
                Yes24ImageUrlGenerator.fromCoverUrl(coverImageUrl);

        assertThat(result.spineImageUrl())
                .isEqualTo("https://image.yes24.com/goods/118578901/side");
        assertThat(result.backImageUrl())
                .isEqualTo("https://image.yes24.com/goods/118578901/back");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "https://image.aladin.co.kr/cover.jpg",
            "https://example.com/goods/118578901/xL",
            "https://image.yes24.com/book/118578901/xL",
            "https://image.yes24.com/goods/not-number/xL",
            "not-a-url"
    })
    @DisplayName("Yes24 커버 URL이 아니면 이미지 URL을 생성하지 않는다")
    void should_ReturnEmpty_When_CoverUrlIsNotYes24Url(String coverImageUrl) {
        Yes24ImageUrls result =
                Yes24ImageUrlGenerator.fromCoverUrl(coverImageUrl);

        assertThat(result).isEqualTo(Yes24ImageUrls.empty());
    }
}
