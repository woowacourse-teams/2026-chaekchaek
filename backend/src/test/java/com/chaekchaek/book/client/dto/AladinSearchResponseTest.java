package com.chaekchaek.book.client.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AladinSearchResponseTest {

    @ParameterizedTest
    @CsvSource({
            "21, 2, 10, true",
            "20, 2, 10, false",
            "0, 1, 10, false"
    })
    @DisplayName("현재 페이지 범위와 전체 결과 수를 비교해 다음 페이지를 판정한다")
    void should_DetermineHasNextPage_When_ComparingPageRangeWithTotalResults(
            int totalResults,
            int startIndex,
            int itemsPerPage,
            boolean expected
    ) {
        // given
        AladinSearchResponse response = response(totalResults, startIndex, itemsPerPage);

        // when & then
        assertThat(response.hasNextPage()).isEqualTo(expected);
    }

    @Test
    @DisplayName("페이지 범위 계산이 int 범위를 넘어도 오버플로하지 않는다")
    void should_NotOverflow_When_PageRangeExceedsIntegerRange() {
        // given
        AladinSearchResponse response = response(Integer.MAX_VALUE, 50_000, 50_000);

        // when & then
        assertThat(response.hasNextPage()).isFalse();
    }

    private AladinSearchResponse response(int totalResults, int startIndex, int itemsPerPage) {
        return new AladinSearchResponse(
                null,
                null,
                totalResults,
                startIndex,
                itemsPerPage,
                List.of()
        );
    }
}
