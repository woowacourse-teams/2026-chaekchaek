package com.chaekchaek.book.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FallbackBookSearchClientTest {

    @Test
    @DisplayName("YES24 검색 결과가 있으면 같은 결과를 반환하고 알라딘을 호출하지 않는다")
    void should_ReturnYes24ResultWithoutCallingAladin_When_Yes24ResultIsNotEmpty() {
        // given
        Yes24BookClient yes24 = mock(Yes24BookClient.class);
        AladinBookClient aladin = mock(AladinBookClient.class);
        FallbackBookSearchClient client = new FallbackBookSearchClient(yes24, aladin);
        BookSearchResult yes24Result = new BookSearchResult(
                1, null, List.of(mock(BookSearchItem.class))
        );
        when(yes24.search("데미안", 1)).thenReturn(yes24Result);

        // when
        BookSearchResult result = client.search("데미안", 1);

        // then
        assertThat(result).isSameAs(yes24Result);
        verify(yes24).search("데미안", 1);
        verifyNoInteractions(aladin);
    }

    @Test
    @DisplayName("YES24 첫 페이지 검색 결과가 비어 있으면 알라딘 결과를 반환한다")
    void should_ReturnAladinResult_When_Yes24FirstPageIsEmpty() {
        // given
        Yes24BookClient yes24 = mock(Yes24BookClient.class);
        AladinBookClient aladin = mock(AladinBookClient.class);
        FallbackBookSearchClient client = new FallbackBookSearchClient(yes24, aladin);
        BookSearchResult aladinResult = new BookSearchResult(1, null, List.of());
        when(yes24.search("데미안", 1)).thenReturn(new BookSearchResult(0, null, List.of()));
        when(aladin.search("데미안", 1)).thenReturn(aladinResult);

        // when
        BookSearchResult result = client.search("데미안", 1);

        // then
        assertThat(result).isSameAs(aladinResult);
        verify(yes24).search("데미안", 1);
        verify(aladin).search("데미안", 1);
    }

    @Test
    @DisplayName("YES24 두 번째 이후 페이지 검색 결과가 비어 있으면 알라딘을 호출하지 않는다")
    void should_ReturnEmptyYes24ResultWithoutCallingAladin_When_LaterPageIsEmpty() {
        // given
        Yes24BookClient yes24 = mock(Yes24BookClient.class);
        AladinBookClient aladin = mock(AladinBookClient.class);
        FallbackBookSearchClient client = new FallbackBookSearchClient(yes24, aladin);
        BookSearchResult yes24Result = new BookSearchResult(0, null, List.of());
        when(yes24.search("데미안", 2)).thenReturn(yes24Result);

        // when
        BookSearchResult result = client.search("데미안", 2);

        // then
        assertThat(result).isSameAs(yes24Result);
        assertThat(result.items()).isEmpty();
        verify(yes24).search("데미안", 2);
        verifyNoInteractions(aladin);
    }

    @Test
    @DisplayName("YES24 대체 가능 오류가 발생하면 같은 페이지의 알라딘 결과를 반환한다")
    void should_ReturnSamePageAladinResult_When_Yes24FailureAllowsFallback() {
        // given
        Yes24BookClient yes24 = mock(Yes24BookClient.class);
        AladinBookClient aladin = mock(AladinBookClient.class);
        FallbackBookSearchClient client = new FallbackBookSearchClient(yes24, aladin);
        BookSearchResult aladinResult = new BookSearchResult(1, null, List.of());
        when(yes24.search("데미안", 3))
                .thenThrow(Yes24ClientException.fallbackAllowed(new RuntimeException("timeout")));
        when(aladin.search("데미안", 3)).thenReturn(aladinResult);

        // when
        BookSearchResult result = client.search("데미안", 3);

        // then
        assertThat(result).isSameAs(aladinResult);
        verify(yes24).search("데미안", 3);
        verify(aladin).search("데미안", 3);
    }

    @Test
    @DisplayName("YES24 대체 불가능 오류가 발생하면 같은 예외를 던지고 알라딘을 호출하지 않는다")
    void should_RethrowSameExceptionWithoutCallingAladin_When_Yes24FailureDisallowsFallback() {
        // given
        Yes24BookClient yes24 = mock(Yes24BookClient.class);
        AladinBookClient aladin = mock(AladinBookClient.class);
        FallbackBookSearchClient client = new FallbackBookSearchClient(yes24, aladin);
        Yes24ClientException exception = Yes24ClientException.notFallbackAllowed(
                "YES24 authentication failed", new RuntimeException()
        );
        when(yes24.search("데미안", 1)).thenThrow(exception);

        // when & then
        assertThatThrownBy(() -> client.search("데미안", 1)).isSameAs(exception);
        verify(yes24).search("데미안", 1);
        verifyNoInteractions(aladin);
    }

}
