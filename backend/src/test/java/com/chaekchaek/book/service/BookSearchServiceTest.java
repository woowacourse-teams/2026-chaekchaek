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

class BookSearchServiceTest {

    @Test
    @DisplayName("알라딘 응답을 변환하면 다음 페이지 판정 결과를 검색 응답에 반영한다")
    void should_ReflectHasNextPage_When_ConvertingAladinResponse() {
        // given
        AladinBookClient bookClient = mock(AladinBookClient.class);
        BookSearchService service = new BookSearchService(bookClient);

        AladinSearchResponse aladinResponse = new AladinSearchResponse(
                null,
                null,
                21,
                2,
                10,
                List.of()
        );

        when(bookClient.searchBooks("마션", 2)).thenReturn(aladinResponse);

        // when
        BookSearchResponse response = service.search("마션", 2);

        // then
        assertThat(response.hasNext()).isTrue();
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
                "로버트 C. 마틴",
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
        assertThat(item.author()).isEqualTo("로버트 C. 마틴");
        assertThat(item.publishedDate()).isEqualTo("2008-08-01");
        assertThat(item.isbn13()).isEqualTo("9788966260959");
        assertThat(item.category()).isEqualTo("국내도서>컴퓨터/모바일>프로그래밍");
        assertThat(item.publisher()).isEqualTo("인사이트");
    }
}
