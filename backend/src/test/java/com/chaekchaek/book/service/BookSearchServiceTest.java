package com.chaekchaek.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.client.AladinBookClient;
import com.chaekchaek.book.client.dto.AladinBookItem;
import com.chaekchaek.book.client.dto.AladinSearchResponse;
import com.chaekchaek.book.dto.BookItem;
import com.chaekchaek.book.dto.BookSearchResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BookSearchServiceTest {

    @ParameterizedTest
    @CsvSource(value = {
            "21, 2, 10, 1, 3",
            "20, 2, 10, 2, null",
            "0, 1, 10, 1, null"
    }, nullValues = "null")
    @DisplayName("알라딘 응답을 변환하면 다음 요청에 사용할 페이지를 반환한다")
    void should_ReturnNextPage_When_ConvertingAladinResponse(
            int totalResults,
            int responseStartIndex,
            int itemsPerPage,
            int requestPage,
            Integer expectedNextPage
    ) {
        // given
        AladinBookClient bookClient = mock(AladinBookClient.class);
        BookSearchService service = new BookSearchService(bookClient);
        AladinSearchResponse aladinResponse = new AladinSearchResponse(
                null, null, totalResults, responseStartIndex, itemsPerPage, List.of()
        );
        when(bookClient.searchBooks("마션", requestPage)).thenReturn(aladinResponse);

        // when
        BookSearchResponse response = service.search("마션", requestPage);

        // then
        assertThat(response.nextPage()).isEqualTo(expectedNextPage);
    }

    @Test
    @DisplayName("알라딘 응답을 변환하면 전체 검색 결과 수를 응답에 반영한다")
    void should_ReflectTotalCount_When_ConvertingAladinResponse() {
        // given
        AladinBookClient bookClient = mock(AladinBookClient.class);
        BookSearchService service = new BookSearchService(bookClient);
        AladinSearchResponse aladinResponse = new AladinSearchResponse(
                null, null, 21, 1, 10, List.of()
        );
        when(bookClient.searchBooks("마션", 1)).thenReturn(aladinResponse);

        // when
        BookSearchResponse response = service.search("마션", 1);

        // then
        assertThat(response.totalCount()).isEqualTo(21);
    }

    @Test
    @DisplayName("알라딘 도서 항목을 변환하면 모든 필드를 검색 응답에 반영한다")
    void should_MapAllBookFields_When_ConvertingAladinBookItem() {
        // given
        AladinBookClient bookClient = mock(AladinBookClient.class);
        BookSearchService service = new BookSearchService(bookClient);
        AladinBookItem aladinBookItem = new AladinBookItem(
                "클린 코드",
                "https://image.aladin.co.kr/cover.jpg",
                "로버트 C. 마틴 (지은이), 박산호 (옮긴이)",
                "2008-08-01",
                "9788966260959",
                "국내도서>컴퓨터/모바일>프로그래밍",
                "인사이트"
        );
        AladinSearchResponse aladinResponse = new AladinSearchResponse(
                null,
                null,
                1,
                1,
                10,
                List.of(aladinBookItem)
        );
        when(bookClient.searchBooks("클린 코드", 1)).thenReturn(aladinResponse);

        // when
        BookSearchResponse response = service.search("클린 코드", 1);

        // then
        BookItem item = response.items().getFirst();
        assertThat(item.title()).isEqualTo("클린 코드");
        assertThat(item.coverImageUrl()).isEqualTo("https://image.aladin.co.kr/cover.jpg");
        assertThat(item.authors()).containsExactly("로버트 C. 마틴");
        assertThat(item.translators()).containsExactly("박산호");
        assertThat(item.publishedDate()).isEqualTo("2008-08-01");
        assertThat(item.isbn13()).isEqualTo("9788966260959");
        assertThat(item.category()).isEqualTo("국내도서>컴퓨터/모바일>프로그래밍");
        assertThat(item.publisher()).isEqualTo("인사이트");
    }
}
