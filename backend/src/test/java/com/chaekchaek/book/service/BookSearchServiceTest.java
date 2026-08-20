package com.chaekchaek.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.client.AladinBookClient;
import com.chaekchaek.book.client.dto.AladinBookItem;
import com.chaekchaek.book.client.dto.AladinSearchResponse;
import com.chaekchaek.book.client.dto.AladinBookSubInfo;
import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.BookSearchSort;
import com.chaekchaek.book.dto.BookItem;
import com.chaekchaek.book.dto.BookSearchResponse;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.library.service.BookActivityCountReader;
import com.chaekchaek.library.service.BookActivityCountReader.ActivityCounts;
import java.util.List;
import java.util.Map;
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
        BookRepository bookRepository = mock(BookRepository.class);
        BookActivityCountReader activityCountReader = mock(BookActivityCountReader.class);
        BookSearchService service = new BookSearchService(bookClient, bookRepository, activityCountReader);
        AladinSearchResponse aladinResponse = new AladinSearchResponse(
                null, null, totalResults, responseStartIndex, itemsPerPage, List.of()
        );
        when(bookClient.searchBooks("마션", requestPage)).thenReturn(aladinResponse);
        when(bookRepository.findAllByIsbn13In(org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(List.of());
        when(activityCountReader.getActivityCounts(org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(Map.of());

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
        BookRepository bookRepository = mock(BookRepository.class);
        BookActivityCountReader activityCountReader = mock(BookActivityCountReader.class);
        BookSearchService service = new BookSearchService(bookClient, bookRepository, activityCountReader);
        AladinSearchResponse aladinResponse = new AladinSearchResponse(
                null, null, 21, 1, 10, List.of()
        );
        when(bookClient.searchBooks("마션", 1)).thenReturn(aladinResponse);
        when(bookRepository.findAllByIsbn13In(org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(List.of());
        when(activityCountReader.getActivityCounts(org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(Map.of());

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
        BookRepository bookRepository = mock(BookRepository.class);
        BookActivityCountReader activityCountReader = mock(BookActivityCountReader.class);
        BookSearchService service = new BookSearchService(bookClient, bookRepository, activityCountReader);
        AladinBookItem aladinBookItem = new AladinBookItem(
                "클린 코드",
                "https://image.aladin.co.kr/cover.jpg",
                "로버트 C. 마틴 (지은이), 박산호 (옮긴이)",
                null,
                "2008-08-01",
                "9788966260959",
                "국내도서>컴퓨터/모바일>프로그래밍",
                "인사이트",
                new AladinBookSubInfo(464)
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
        when(bookRepository.findAllByIsbn13In(org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(List.of());
        when(activityCountReader.getActivityCounts(org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(Map.of());

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

    @Test
    @DisplayName("등록된 도서를 검색하면 책 ID와 감상·답글 수를 반환한다")
    void should_ReturnBookIdAndCommentCount_When_SearchResultIsRegistered() {
        // given
        AladinBookClient bookClient = mock(AladinBookClient.class);
        BookRepository bookRepository = mock(BookRepository.class);
        BookActivityCountReader activityCountReader = mock(BookActivityCountReader.class);
        BookSearchService service = new BookSearchService(bookClient, bookRepository, activityCountReader);
        AladinBookItem aladinBookItem = new AladinBookItem(
                "마션", "https://image.example/martian.jpg", "앤디 위어 (지은이)",
                null,
                "2026-01-01", "9788925568683", "SF", "알에이치코리아",
                new AladinBookSubInfo(308)
        );
        Book registeredBook = mock(Book.class);
        when(registeredBook.getId()).thenReturn(42L);
        when(registeredBook.getIsbn13()).thenReturn("9788925568683");
        when(bookClient.searchBooks("마션", 1)).thenReturn(new AladinSearchResponse(
                null, null, 1, 1, 10, List.of(aladinBookItem)));
        when(bookRepository.findAllByIsbn13In(List.of("9788925568683")))
                .thenReturn(List.of(registeredBook));
        when(activityCountReader.getActivityCounts(List.of(42L)))
                .thenReturn(Map.of(42L, new ActivityCounts(2L, 5L)));

        // when
        BookItem item = service.search("마션", 1).items().getFirst();

        // then
        assertThat(item.bookId()).isEqualTo(42L);
        assertThat(item.reviewCount()).isEqualTo(2);
        assertThat(item.replyCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("최신순으로 검색하면 출판일이 최신인 도서부터 반환한다")
    void should_SortByPublishedDateDescending_When_SortIsLatest() {
        // given
        BookSearchService service = serviceWith(
                new AladinSearchResponse(
                        null,
                        null,
                        3,
                        1,
                        10,
                        List.of(
                                aladinBook("오래된 책", "2021-01-01", "9780000000001"),
                                aladinBook("최신 책", "2026-01-01", "9780000000002"),
                                aladinBook("중간 책", "2024-01-01", "9780000000003")
                        )
                ),
                Map.of()
        );

        // when
        BookSearchResponse response = service.search("책", 1, BookSearchSort.LATEST);

        // then
        assertThat(response.items()).extracting(BookItem::title)
                .containsExactly("최신 책", "중간 책", "오래된 책");
    }

    @Test
    @DisplayName("댓글순으로 검색하면 댓글 수가 많은 도서부터 반환한다")
    void should_SortByCommentCountDescending_When_SortIsComment() {
        // given
        AladinBookItem oldestBook = aladinBook("댓글 적은 책", "2021-01-01", "9780000000001");
        AladinBookItem mostCommentedBook = aladinBook("댓글 많은 책", "2024-01-01", "9780000000002");
        AladinBookItem middleBook = aladinBook("댓글 중간 책", "2026-01-01", "9780000000003");
        AladinBookItem unregisteredBook = aladinBook("미등록 책", "2025-01-01", "9780000000004");
        BookSearchService service = serviceWith(
                new AladinSearchResponse(null, null, 4, 1, 10,
                        List.of(oldestBook, mostCommentedBook, middleBook, unregisteredBook)),
                Map.of(1L, new ActivityCounts(1L, 0L),
                        2L, new ActivityCounts(6L, 4L),
                        3L, new ActivityCounts(2L, 3L)),
                registeredBook(1L, oldestBook.isbn13()),
                registeredBook(2L, mostCommentedBook.isbn13()),
                registeredBook(3L, middleBook.isbn13())
        );

        // when
        BookSearchResponse response = service.search("책", 1, BookSearchSort.COMMENT);

        // then
        assertThat(response.items()).extracting(BookItem::title)
                .containsExactly("댓글 많은 책", "댓글 중간 책", "댓글 적은 책", "미등록 책");
    }

    private BookSearchService serviceWith(
            AladinSearchResponse response,
            Map<Long, ActivityCounts> activityCounts,
            Book... registeredBooks
    ) {
        AladinBookClient bookClient = mock(AladinBookClient.class);
        BookRepository bookRepository = mock(BookRepository.class);
        BookActivityCountReader activityCountReader = mock(BookActivityCountReader.class);
        when(bookClient.searchBooks("책", 1)).thenReturn(response);
        when(bookRepository.findAllByIsbn13In(org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(List.of(registeredBooks));
        when(activityCountReader.getActivityCounts(org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(activityCounts);
        return new BookSearchService(bookClient, bookRepository, activityCountReader);
    }

    private AladinBookItem aladinBook(String title, String publishedDate, String isbn13) {
        return new AladinBookItem(title, "https://image.example/cover.jpg", "작가", null, publishedDate,
                isbn13, "소설", "출판사", new AladinBookSubInfo(200));
    }

    private Book registeredBook(Long id, String isbn13) {
        Book book = mock(Book.class);
        when(book.getId()).thenReturn(id);
        when(book.getIsbn13()).thenReturn(isbn13);
        return book;
    }
}
