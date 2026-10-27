package com.chaekchaek.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.client.BookSearchClient;
import com.chaekchaek.book.client.BookSearchItem;
import com.chaekchaek.book.client.BookSearchResult;
import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.dto.BookItem;
import com.chaekchaek.book.dto.BookSearchResponse;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.common.auth.CurrentMemberIdProvider;
import com.chaekchaek.library.repository.LibraryBookRepository;
import com.chaekchaek.library.service.BookActivityCountReader;
import com.chaekchaek.library.service.BookActivityCountReader.ActivityCounts;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.test.util.ReflectionTestUtils;

class BookSearchServiceTest {

    private final BookSearchClient bookClient = mock(BookSearchClient.class);
    private final BookRepository bookRepository = mock(BookRepository.class);
    private final BookActivityCountReader activityCountReader = mock(BookActivityCountReader.class);
    private final CurrentMemberIdProvider currentMemberIdProvider = mock(CurrentMemberIdProvider.class);
    private final LibraryBookRepository libraryBookRepository = mock(LibraryBookRepository.class);
    private final BookSearchService service = new BookSearchService(
            bookClient,
            bookRepository,
            activityCountReader,
            currentMemberIdProvider,
            libraryBookRepository
    );

    @Test
    @DisplayName("검색 결과에 다음 페이지가 있으면 다음 요청에 사용할 페이지를 반환한다")
    void should_ReturnNextPage_When_SearchResultHasNextPage() {
        // given
        BookSearchResult searchResult = new BookSearchResult(
                21,
                2,
                List.of()
        );
        when(bookClient.search("마션", 1)).thenReturn(searchResult);
        when(bookRepository.findAllByIsbn13In(ArgumentMatchers.anyCollection()))
                .thenReturn(List.of());
        when(activityCountReader.getActivityCounts(ArgumentMatchers.anyCollection()))
                .thenReturn(Map.of());

        // when
        BookSearchResponse response = service.search("마션", 1);

        // then
        assertThat(response.nextPage()).isEqualTo(2);
    }

    @Test
    @DisplayName("검색 결과의 전체 도서 수를 응답에 반영한다")
    void should_ReflectTotalCount_When_ConvertingSearchResult() {
        // given
        BookSearchResult searchResult = new BookSearchResult(
                21,
                null,
                List.of()
        );
        when(bookClient.search("마션", 1)).thenReturn(searchResult);
        when(bookRepository.findAllByIsbn13In(ArgumentMatchers.anyCollection()))
                .thenReturn(List.of());
        when(activityCountReader.getActivityCounts(ArgumentMatchers.anyCollection()))
                .thenReturn(Map.of());

        // when
        BookSearchResponse response = service.search("마션", 1);

        // then
        assertThat(response.totalCount()).isEqualTo(21);
    }

    @Test
    @DisplayName("검색된 도서 항목을 변환하면 모든 필드를 검색 응답에 반영한다")
    void should_MapAllBookFields_When_ConvertingBookSearchItem() {
        // given
        BookSearchItem searchedBook = new BookSearchItem(
                "클린 코드",
                "https://example.com/books/cover.jpg",
                List.of("로버트 C. 마틴"),
                List.of("박산호"),
                LocalDate.of(2008, 8, 1),
                "9788966260959",
                "국내도서>컴퓨터/모바일>프로그래밍",
                "인사이트"
        );
        BookSearchResult searchResult = new BookSearchResult(
                1,
                null,
                List.of(searchedBook)
        );
        when(bookClient.search("클린 코드", 1)).thenReturn(searchResult);
        when(bookRepository.findAllByIsbn13In(ArgumentMatchers.anyCollection()))
                .thenReturn(List.of());
        when(activityCountReader.getActivityCounts(ArgumentMatchers.anyCollection()))
                .thenReturn(Map.of());

        // when
        BookSearchResponse response = service.search("클린 코드", 1);

        // then
        BookSearchResponse expected = new BookSearchResponse(
                1,
                null,
                List.of(new BookItem(
                        null,
                        "클린 코드",
                        "https://example.com/books/cover.jpg",
                        List.of("로버트 C. 마틴"),
                        List.of("박산호"),
                        "2008-08-01",
                        "9788966260959",
                        "국내도서>컴퓨터/모바일>프로그래밍",
                        "인사이트",
                        null,
                        null,
                        null
                ))
        );
        assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("등록된 도서를 검색하면 책 ID와 감상·답글 수를 반환한다")
    void should_ReturnBookIdAndCommentCount_When_SearchResultIsRegistered() {
        // given
        BookSearchItem searchedBook = new BookSearchItem(
                "마션",
                "https://image.example/martian.jpg",
                List.of("앤디 위어"),
                List.of(),
                LocalDate.of(2026, 1, 1),
                "9788925568683",
                "SF",
                "알에이치코리아"
        );
        Book registeredBook = registeredBook(42L, searchedBook.isbn13());
        when(bookClient.search("마션", 1)).thenReturn(new BookSearchResult(
                1,
                null,
                List.of(searchedBook)
        ));
        when(bookRepository.findAllByIsbn13In(List.of(new Isbn13("9788925568683"))))
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
    @DisplayName("등록된 도서에 감상과 답글이 없으면 각 수를 0으로 반환한다")
    void should_ReturnZeroCounts_When_RegisteredBookHasNoActivity() {
        // given
        BookSearchItem searchedBook = searchedBook("마션", "2026-01-01", "9788925568683");
        stubSearch(
                new BookSearchResult(
                        1,
                        null,
                        List.of(searchedBook)
                ),
                Map.of(),
                registeredBook(42L, searchedBook.isbn13())
        );

        // when
        BookItem item = service.search("책", 1).items().getFirst();

        // then
        assertThat(item.reviewCount()).isZero();
        assertThat(item.replyCount()).isZero();
    }

    @Test
    @DisplayName("로그인한 회원의 서재에 있는 도서를 검색하면 내 서재 등록 여부로 true를 반환한다")
    void should_ReturnTrue_When_SearchResultIsInAuthenticatedMembersLibrary() {
        // given
        BookSearchItem searchedBook = searchedBook("마션", "2026-01-01", "9788925568683");
        Book registeredBook = registeredBook(42L, searchedBook.isbn13());
        when(bookClient.search("마션", 1)).thenReturn(new BookSearchResult(
                1,
                null,
                List.of(searchedBook)
        ));
        when(bookRepository.findAllByIsbn13In(List.of(new Isbn13("9788925568683")))).thenReturn(List.of(registeredBook));
        when(activityCountReader.getActivityCounts(List.of(42L))).thenReturn(Map.of());
        when(currentMemberIdProvider.findCurrentMemberId()).thenReturn(OptionalLong.of(1L));
        when(libraryBookRepository.findPlacedBookIdsByMemberIdAndBookIdIn(1L, List.of(42L)))
                .thenReturn(List.of(42L));

        // when
        BookItem item = service.search("마션", 1).items().getFirst();

        // then
        assertThat(item.isRegisteredInMyLibrary()).isTrue();
    }

    @Test
    @DisplayName("로그인한 회원의 서재에 없는 도서를 검색하면 내 서재 등록 여부로 false를 반환한다")
    void should_ReturnFalse_When_SearchResultIsNotInAuthenticatedMembersLibrary() {
        // given
        stubRegisteredSearch();
        when(currentMemberIdProvider.findCurrentMemberId()).thenReturn(OptionalLong.of(1L));
        when(libraryBookRepository.findPlacedBookIdsByMemberIdAndBookIdIn(1L, List.of(42L)))
                .thenReturn(List.of());

        // when
        BookItem item = service.search("마션", 1).items().getFirst();

        // then
        assertThat(item.isRegisteredInMyLibrary()).isFalse();
    }

    @Test
    @DisplayName("비로그인으로 도서를 검색하면 내 서재 등록 여부로 null을 반환한다")
    void should_ReturnNull_When_SearchingWithoutAuthentication() {
        // given
        stubRegisteredSearch();
        when(currentMemberIdProvider.findCurrentMemberId()).thenReturn(OptionalLong.empty());

        // when
        BookItem item = service.search("마션", 1).items().getFirst();

        // then
        assertThat(item.isRegisteredInMyLibrary()).isNull();
    }

    @Test
    @DisplayName("검색 결과에 도서와 활동 정보를 결합해도 입력 순서를 유지한다")
    void should_PreserveInputOrder_When_EnrichingSearchResults() {
        // given
        BookSearchItem unregistered = searchedBook("나 도서", "2023-01-01", "9780000000002");
        BookSearchItem oldest = searchedBook("다 도서", "2021-01-01", "9780000000019");
        BookSearchItem newest = searchedBook("가 도서", "2026-01-01", "9780000000026");
        BookSearchItem middle = searchedBook("라 도서", "2024-01-01", "9780000000033");
        stubSearch(
                new BookSearchResult(
                        14,
                        2,
                        List.of(unregistered, oldest, newest, middle)
                ),
                Map.of(
                        42L, new ActivityCounts(2L, 7L),
                        7L, new ActivityCounts(8L, 1L),
                        99L, new ActivityCounts(1L, 0L)
                ),
                registeredBook(99L, middle.isbn13()),
                registeredBook(7L, newest.isbn13()),
                registeredBook(42L, oldest.isbn13())
        );

        // when
        BookSearchResponse response = service.search("책", 1);

        // then
        assertThat(response.items())
                .extracting(BookItem::isbn13, BookItem::bookId, BookItem::reviewCount, BookItem::replyCount)
                .containsExactly(
                        tuple(unregistered.isbn13(), null, null, null),
                        tuple(oldest.isbn13(), 42L, 2, 7),
                        tuple(newest.isbn13(), 7L, 8, 1),
                        tuple(middle.isbn13(), 99L, 1, 0)
                );
        assertThat(response.totalCount()).isEqualTo(14);
        assertThat(response.nextPage()).isEqualTo(2);
    }

    private void stubSearch(
            BookSearchResult response,
            Map<Long, ActivityCounts> activityCounts,
            Book... registeredBooks
    ) {
        when(bookClient.search("책", 1)).thenReturn(response);
        when(bookRepository.findAllByIsbn13In(ArgumentMatchers.anyCollection()))
                .thenReturn(List.of(registeredBooks));
        when(activityCountReader.getActivityCounts(ArgumentMatchers.anyCollection()))
                .thenReturn(activityCounts);
    }

    private void stubRegisteredSearch() {
        BookSearchItem searchedBook = searchedBook("마션", "2026-01-01", "9788925568683");
        Book registeredBook = registeredBook(42L, searchedBook.isbn13());
        when(bookClient.search("마션", 1)).thenReturn(new BookSearchResult(
                1,
                null,
                List.of(searchedBook)
        ));
        when(bookRepository.findAllByIsbn13In(List.of(new Isbn13("9788925568683")))).thenReturn(List.of(registeredBook));
        when(activityCountReader.getActivityCounts(List.of(42L))).thenReturn(Map.of());

    }

    private BookSearchItem searchedBook(String title, String publishedDate, String isbn13) {
        return new BookSearchItem(
                title,
                "https://image.example/cover.jpg",
                List.of("작가"),
                List.of(),
                LocalDate.parse(publishedDate),
                isbn13,
                "소설",
                "출판사"
        );
    }

    private Book registeredBook(Long id, String isbn13) {
        Book book = Book.create(
                new Isbn13(isbn13),
                "등록 도서",
                "https://image.example/cover.jpg",
                null,
                null,
                null,
                List.of("작가"),
                List.of(),
                "출판사",
                "소설",
                null,
                null,
                null,
                null
        );
        ReflectionTestUtils.setField(book, "id", id);
        return book;
    }
}
