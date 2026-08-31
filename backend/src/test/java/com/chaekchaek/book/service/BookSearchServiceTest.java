package com.chaekchaek.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.client.BookSearchClient;
import com.chaekchaek.book.client.BookSearchItem;
import com.chaekchaek.book.client.BookSearchResult;
import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.BookSearchSort;
import com.chaekchaek.book.dto.BookItem;
import com.chaekchaek.book.dto.BookSearchResponse;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.common.auth.CurrentMemberIdProvider;
import com.chaekchaek.library.domain.LibraryItem;
import com.chaekchaek.library.repository.LibraryItemRepository;
import com.chaekchaek.library.service.BookActivityCountReader;
import com.chaekchaek.library.service.BookActivityCountReader.ActivityCounts;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BookSearchServiceTest {

    @Test
    @DisplayName("검색 결과에 다음 페이지가 있으면 다음 요청에 사용할 페이지를 반환한다")
    void should_ReturnNextPage_When_SearchResultHasNextPage() {
        // given
        BookSearchClient bookClient = mock(BookSearchClient.class);
        BookRepository bookRepository = mock(BookRepository.class);
        BookActivityCountReader activityCountReader = mock(BookActivityCountReader.class);
        BookSearchService service = guestService(bookClient, bookRepository, activityCountReader);
        BookSearchResult searchResult = new BookSearchResult(21, 2, List.of());
        when(bookClient.search("마션", 1)).thenReturn(searchResult);
        when(bookRepository.findAllByIsbn13In(org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(List.of());
        when(activityCountReader.getActivityCounts(org.mockito.ArgumentMatchers.anyCollection()))
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
        BookSearchClient bookClient = mock(BookSearchClient.class);
        BookRepository bookRepository = mock(BookRepository.class);
        BookActivityCountReader activityCountReader = mock(BookActivityCountReader.class);
        BookSearchService service = guestService(bookClient, bookRepository, activityCountReader);
        BookSearchResult searchResult = new BookSearchResult(21, null, List.of());
        when(bookClient.search("마션", 1)).thenReturn(searchResult);
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
    @DisplayName("검색된 도서 항목을 변환하면 모든 필드를 검색 응답에 반영한다")
    void should_MapAllBookFields_When_ConvertingBookSearchItem() {
        // given
        BookSearchClient bookClient = mock(BookSearchClient.class);
        BookRepository bookRepository = mock(BookRepository.class);
        BookActivityCountReader activityCountReader = mock(BookActivityCountReader.class);
        BookSearchService service = guestService(bookClient, bookRepository, activityCountReader);
        BookSearchItem searchedBook = new BookSearchItem(
                "클린 코드",
                "https://image.aladin.co.kr/cover.jpg",
                List.of("로버트 C. 마틴"),
                List.of("박산호"),
                LocalDate.of(2008, 8, 1),
                "9788966260959",
                "국내도서>컴퓨터/모바일>프로그래밍",
                "인사이트"
        );
        BookSearchResult searchResult = new BookSearchResult(1, null, List.of(searchedBook));
        when(bookClient.search("클린 코드", 1)).thenReturn(searchResult);
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
        BookSearchClient bookClient = mock(BookSearchClient.class);
        BookRepository bookRepository = mock(BookRepository.class);
        BookActivityCountReader activityCountReader = mock(BookActivityCountReader.class);
        BookSearchService service = guestService(bookClient, bookRepository, activityCountReader);
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
        Book registeredBook = mock(Book.class);
        when(registeredBook.getId()).thenReturn(42L);
        when(registeredBook.getIsbn13()).thenReturn("9788925568683");
        when(bookClient.search("마션", 1)).thenReturn(new BookSearchResult(
                1, null, List.of(searchedBook)));
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
    @DisplayName("등록된 도서에 감상과 답글이 없으면 각 수를 0으로 반환한다")
    void should_ReturnZeroCounts_When_RegisteredBookHasNoActivity() {
        // given
        BookSearchItem searchedBook = searchedBook("마션", "2026-01-01", "9788925568683");
        BookSearchService service = serviceWith(
                new BookSearchResult(1, null, List.of(searchedBook)),
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
        BookSearchClient bookClient = mock(BookSearchClient.class);
        BookRepository bookRepository = mock(BookRepository.class);
        BookActivityCountReader activityCountReader = mock(BookActivityCountReader.class);
        CurrentMemberIdProvider currentMemberIdProvider = mock(CurrentMemberIdProvider.class);
        LibraryItemRepository libraryItemRepository = mock(LibraryItemRepository.class);
        BookSearchService service = new BookSearchService(
                bookClient, bookRepository, activityCountReader, currentMemberIdProvider, libraryItemRepository);
        BookSearchItem searchedBook = searchedBook("마션", "2026-01-01", "9788925568683");
        Book registeredBook = registeredBook(42L, searchedBook.isbn13());
        LibraryItem libraryItem = mock(LibraryItem.class);
        when(libraryItem.getBookId()).thenReturn(42L);
        when(bookClient.search("마션", 1)).thenReturn(new BookSearchResult(
                1, null, List.of(searchedBook)));
        when(bookRepository.findAllByIsbn13In(List.of("9788925568683"))).thenReturn(List.of(registeredBook));
        when(activityCountReader.getActivityCounts(List.of(42L))).thenReturn(Map.of());
        when(currentMemberIdProvider.findCurrentMemberId()).thenReturn(OptionalLong.of(1L));
        when(libraryItemRepository.findAllByMemberIdAndBookIdIn(1L, List.of(42L)))
                .thenReturn(List.of(libraryItem));

        // when
        BookItem item = service.search("마션", 1).items().getFirst();

        // then
        assertThat(item.isRegisteredInMyLibrary()).isTrue();
    }

    @Test
    @DisplayName("로그인한 회원의 서재에 없는 도서를 검색하면 내 서재 등록 여부로 false를 반환한다")
    void should_ReturnFalse_When_SearchResultIsNotInAuthenticatedMembersLibrary() {
        // given
        BookItem item = searchRegisteredBook(OptionalLong.of(1L), List.of());

        // when & then
        assertThat(item.isRegisteredInMyLibrary()).isFalse();
    }

    @Test
    @DisplayName("비로그인으로 도서를 검색하면 내 서재 등록 여부로 null을 반환한다")
    void should_ReturnNull_When_SearchingWithoutAuthentication() {
        // given
        BookItem item = searchRegisteredBook(OptionalLong.empty(), List.of());

        // when & then
        assertThat(item.isRegisteredInMyLibrary()).isNull();
    }

    @Test
    @DisplayName("최신순으로 검색하면 출판일이 최신인 도서부터 반환한다")
    void should_SortByPublishedDateDescending_When_SortIsLatest() {
        // given
        BookSearchService service = serviceWith(
                new BookSearchResult(
                        3,
                        null,
                        List.of(
                                searchedBook("오래된 책", "2021-01-01", "9780000000001"),
                                searchedBook("최신 책", "2026-01-01", "9780000000002"),
                                searchedBook("중간 책", "2024-01-01", "9780000000003")
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
        BookSearchItem oldestBook = searchedBook("댓글 적은 책", "2021-01-01", "9780000000001");
        BookSearchItem mostCommentedBook = searchedBook("댓글 많은 책", "2024-01-01", "9780000000002");
        BookSearchItem middleBook = searchedBook("댓글 중간 책", "2026-01-01", "9780000000003");
        BookSearchItem unregisteredBook = searchedBook("미등록 책", "2025-01-01", "9780000000004");
        BookSearchService service = serviceWith(
                new BookSearchResult(4, null,
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
            BookSearchResult response,
            Map<Long, ActivityCounts> activityCounts,
            Book... registeredBooks
    ) {
        BookSearchClient bookClient = mock(BookSearchClient.class);
        BookRepository bookRepository = mock(BookRepository.class);
        BookActivityCountReader activityCountReader = mock(BookActivityCountReader.class);
        when(bookClient.search("책", 1)).thenReturn(response);
        when(bookRepository.findAllByIsbn13In(org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(List.of(registeredBooks));
        when(activityCountReader.getActivityCounts(org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(activityCounts);
        return guestService(bookClient, bookRepository, activityCountReader);
    }

    private BookSearchService guestService(
            BookSearchClient bookClient,
            BookRepository bookRepository,
            BookActivityCountReader activityCountReader
    ) {
        CurrentMemberIdProvider currentMemberIdProvider = mock(CurrentMemberIdProvider.class);
        when(currentMemberIdProvider.findCurrentMemberId()).thenReturn(OptionalLong.empty());
        return new BookSearchService(
                bookClient,
                bookRepository,
                activityCountReader,
                currentMemberIdProvider,
                mock(LibraryItemRepository.class)
        );
    }

    private BookItem searchRegisteredBook(OptionalLong memberId, List<LibraryItem> libraryItems) {
        BookSearchClient bookClient = mock(BookSearchClient.class);
        BookRepository bookRepository = mock(BookRepository.class);
        BookActivityCountReader activityCountReader = mock(BookActivityCountReader.class);
        CurrentMemberIdProvider currentMemberIdProvider = mock(CurrentMemberIdProvider.class);
        LibraryItemRepository libraryItemRepository = mock(LibraryItemRepository.class);
        BookSearchService service = new BookSearchService(
                bookClient, bookRepository, activityCountReader, currentMemberIdProvider, libraryItemRepository);
        BookSearchItem searchedBook = searchedBook("마션", "2026-01-01", "9788925568683");
        Book registeredBook = registeredBook(42L, searchedBook.isbn13());
        when(bookClient.search("마션", 1)).thenReturn(new BookSearchResult(
                1, null, List.of(searchedBook)));
        when(bookRepository.findAllByIsbn13In(List.of("9788925568683"))).thenReturn(List.of(registeredBook));
        when(activityCountReader.getActivityCounts(List.of(42L))).thenReturn(Map.of());
        when(currentMemberIdProvider.findCurrentMemberId()).thenReturn(memberId);
        if (memberId.isPresent()) {
            when(libraryItemRepository.findAllByMemberIdAndBookIdIn(memberId.getAsLong(), List.of(42L)))
                    .thenReturn(libraryItems);
        }

        return service.search("마션", 1).items().getFirst();
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
        Book book = mock(Book.class);
        when(book.getId()).thenReturn(id);
        when(book.getIsbn13()).thenReturn(isbn13);
        return book;
    }
}
