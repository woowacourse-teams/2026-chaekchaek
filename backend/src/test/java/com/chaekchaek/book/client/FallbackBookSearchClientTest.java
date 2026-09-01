package com.chaekchaek.book.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    @DisplayName("YES24 검색 결과가 비어 있어도 같은 결과를 반환하고 알라딘을 호출하지 않는다")
    void should_ReturnEmptyYes24ResultWithoutCallingAladin_When_Yes24ResultIsEmpty(int page) {
        // given
        Yes24BookClient yes24 = mock(Yes24BookClient.class);
        AladinBookClient aladin = mock(AladinBookClient.class);
        FallbackBookSearchClient client = new FallbackBookSearchClient(yes24, aladin);
        BookSearchResult yes24Result = new BookSearchResult(0, null, List.of());
        when(yes24.search("데미안", page)).thenReturn(yes24Result);

        // when
        BookSearchResult result = client.search("데미안", page);

        // then
        assertThat(result).isSameAs(yes24Result);
        assertThat(result.items()).isEmpty();
        verify(yes24).search("데미안", page);
        verifyNoInteractions(aladin);
    }

    @Test
    @DisplayName("1페이지에서 YES24 대체 가능 오류가 발생하면 알라딘 결과를 반환한다")
    void should_ReturnAladinResult_When_Yes24FailureAllowsFallbackOnFirstPage() {
        // given
        Yes24BookClient yes24 = mock(Yes24BookClient.class);
        AladinBookClient aladin = mock(AladinBookClient.class);
        FallbackBookSearchClient client = new FallbackBookSearchClient(yes24, aladin);
        BookSearchResult aladinResult = new BookSearchResult(1, null, List.of());
        when(yes24.search("데미안", 1))
                .thenThrow(Yes24ClientException.fallbackAllowed(new RuntimeException("timeout")));
        when(aladin.search("데미안", 1)).thenReturn(aladinResult);

        // when
        BookSearchResult result = client.search("데미안", 1);

        // then
        assertThat(result).isSameAs(aladinResult);
        verify(yes24).search("데미안", 1);
        verify(aladin).search("데미안", 1);
    }

    @Test
    @DisplayName("2페이지 이후 YES24 대체 가능 오류가 발생하면 알라딘 없이 재시도 결과를 반환한다")
    void should_ReturnRetriedYes24ResultWithoutCallingAladin_When_Yes24FailsOnLaterPage() {
        // given
        Yes24BookClient yes24 = mock(Yes24BookClient.class);
        AladinBookClient aladin = mock(AladinBookClient.class);
        FallbackBookSearchClient client = new FallbackBookSearchClient(yes24, aladin);
        BookSearchResult retriedYes24Result = new BookSearchResult(1, null, List.of());
        when(yes24.search("데미안", 2))
                .thenThrow(Yes24ClientException.fallbackAllowed(new RuntimeException("timeout")))
                .thenReturn(retriedYes24Result);

        // when
        BookSearchResult result = client.search("데미안", 2);

        // then
        assertThat(result).isSameAs(retriedYes24Result);
        verify(yes24, times(2)).search("데미안", 2);
        verifyNoInteractions(aladin);
    }

    @Test
    @DisplayName("2페이지 이후 YES24 재시도가 실패하면 해당 오류를 전파하고 알라딘을 호출하지 않는다")
    void should_RethrowRetryExceptionWithoutCallingAladin_When_RetriedYes24FailsOnLaterPage() {
        // given
        Yes24BookClient yes24 = mock(Yes24BookClient.class);
        AladinBookClient aladin = mock(AladinBookClient.class);
        FallbackBookSearchClient client = new FallbackBookSearchClient(yes24, aladin);
        Yes24ClientException retryException = Yes24ClientException.fallbackAllowed(
                new RuntimeException("retry timeout")
        );
        when(yes24.search("데미안", 2))
                .thenThrow(Yes24ClientException.fallbackAllowed(new RuntimeException("timeout")))
                .thenThrow(retryException);

        // when & then
        assertThatThrownBy(() -> client.search("데미안", 2)).isSameAs(retryException);
        verify(yes24, times(2)).search("데미안", 2);
        verifyNoInteractions(aladin);
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

    @Test
    @DisplayName("2페이지 이후 YES24 대체 불가능 오류가 발생하면 즉시 오류를 전파한다")
    void should_RethrowSameExceptionWithoutRetrying_When_Yes24FailureDisallowsFallbackOnLaterPage() {
        // given
        Yes24BookClient yes24 = mock(Yes24BookClient.class);
        AladinBookClient aladin = mock(AladinBookClient.class);
        FallbackBookSearchClient client = new FallbackBookSearchClient(yes24, aladin);
        Yes24ClientException exception = Yes24ClientException.notFallbackAllowed(
                "YES24 authentication failed", new RuntimeException()
        );
        when(yes24.search("데미안", 2)).thenThrow(exception);

        // when & then
        assertThatThrownBy(() -> client.search("데미안", 2)).isSameAs(exception);
        verify(yes24).search("데미안", 2);
        verifyNoInteractions(aladin);
    }

}
