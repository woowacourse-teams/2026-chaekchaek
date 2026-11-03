package com.chaekchaek.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.client.BookSearchClient;
import com.chaekchaek.book.client.BookSearchItem;
import com.chaekchaek.book.client.BookSearchResult;
import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.common.auth.CurrentMemberIdProvider;
import com.chaekchaek.library.domain.Library;
import com.chaekchaek.library.domain.LibraryBook;
import com.chaekchaek.library.domain.LibraryItem;
import com.chaekchaek.library.domain.ReadingStatus;
import com.chaekchaek.library.repository.LibraryBookRepository;
import com.chaekchaek.library.repository.LibraryItemRepository;
import com.chaekchaek.library.repository.LibraryRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
@ActiveProfiles("test")
@Import(BookSearchService.class)
class BookSearchPlacementIntegrationTest {

    @Autowired
    private BookSearchService bookSearchService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private LibraryBookRepository libraryBookRepository;

    @Autowired
    private LibraryItemRepository libraryItemRepository;

    @MockitoBean
    private BookSearchClient bookSearchClient;

    @MockitoBean
    private BookSearchActivityCountReader searchActivityCountReader;

    @MockitoBean
    private CurrentMemberIdProvider currentMemberIdProvider;

    @Test
    @DisplayName("도서 등록 여부는 별점 기록과 다른 회원의 배치가 아닌 내 책장의 실제 배치로 판단한다")
    void should_UseOwnedPlacements_When_DeterminingLibraryRegistration() {
        // given
        Instant now = Instant.parse("2026-10-23T04:00:00Z");
        Book book = bookRepository.saveAndFlush(Book.create(
                new Isbn13("9788936433598"),
                "책",
                "https://example.com/cover.jpg",
                null,
                null,
                null,
                List.of("작가"),
                List.of(),
                "출판사",
                "소설",
                null,
                300,
                null,
                null
        ));
        LibraryItem ratingOnly = LibraryItem.create(1L, book.getId(), ReadingStatus.READING, 300, now);
        ratingOnly.clearReadingState();
        ratingOnly.rate(new BigDecimal("4.5"), now);
        libraryItemRepository.saveAndFlush(ratingOnly);
        Library mine = libraryRepository.create(Library.create(1L, "Mine", now));
        Library another = libraryRepository.create(Library.create(1L, "Another", now));
        Library foreign = libraryRepository.create(Library.create(2L, "Foreign", now));
        libraryItemRepository.saveAndFlush(LibraryItem.create(2L, book.getId(), ReadingStatus.READING, 300, now));
        libraryBookRepository.create(LibraryBook.create(foreign.getId(), book.getId(), 1L));
        when(currentMemberIdProvider.findCurrentMemberId()).thenReturn(OptionalLong.of(1L));
        when(searchActivityCountReader.getActivityCounts(anyCollection())).thenReturn(Map.of());
        when(bookSearchClient.search("책", 1)).thenReturn(new BookSearchResult(
                1,
                null,
                List.of(new BookSearchItem(
                        "책",
                        "https://example.com/cover.jpg",
                        List.of("작가"),
                        List.of(),
                        null,
                        book.getIsbn13().value(),
                        "소설",
                        "출판사"
                ))
        ));

        // when & then
        assertThat(isRegistered()).isFalse();
        ratingOnly.startReading(ReadingStatus.READING, 300, now);
        LibraryBook first = libraryBookRepository.create(LibraryBook.create(mine.getId(), book.getId(), 1L));
        LibraryBook second = libraryBookRepository.create(LibraryBook.create(another.getId(), book.getId(), 1L));
        assertThat(isRegistered()).isTrue();
        assertThat(libraryBookRepository.findPlacedBookIdsByMemberIdAndBookIdIn(1L, List.of(book.getId())))
                .containsExactly(book.getId());

        libraryBookRepository.deleteAll(List.of(first, second));
        libraryBookRepository.flush();
        assertThat(isRegistered()).isFalse();
        assertThat(libraryItemRepository.findByMemberIdAndBookId(1L, book.getId()))
                .get()
                .satisfies(item -> assertThat(item.getRating()).isEqualByComparingTo("4.5"));
    }

    private Boolean isRegistered() {
        return bookSearchService.search("책", 1).items().getFirst().isRegisteredInMyLibrary();
    }
}
