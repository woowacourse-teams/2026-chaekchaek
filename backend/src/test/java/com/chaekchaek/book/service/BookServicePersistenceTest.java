package com.chaekchaek.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.common.auth.CurrentMemberIdProvider;
import com.chaekchaek.library.domain.LibraryItem;
import com.chaekchaek.library.domain.ReadingStatus;
import com.chaekchaek.library.repository.LibraryItemRepository;
import com.chaekchaek.library.service.BookActivityCountReader;
import com.chaekchaek.library.service.BookActivityCountReader.ActivityCounts;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import({BookDetailAssembler.class, BookService.class})
class BookServicePersistenceTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookService bookService;

    @Autowired
    private LibraryItemRepository libraryItemRepository;

    @MockitoBean
    private BookActivityCountReader activityCountReader;

    @MockitoBean
    private CurrentMemberIdProvider currentMemberIdProvider;

    @MockitoBean
    private BookResolver bookResolver;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("등록된 도서 상세를 조회하면 지연 로딩 컬렉션을 포함해 반환한다")
    void should_ReturnDetailWithContributors_When_GettingStoredBookOutsideTransaction() {
        // given
        Book savedBook = bookRepository.saveAndFlush(Book.create(
                "9788925568683", "마션", "https://image.example/martian.jpg",
                List.of("앤디 위어", "공동 저자"), List.of("박아람", "공동 번역가"),
                "알에이치코리아", "SF",
                LocalDate.of(2026, 1, 1), 308
        ));
        libraryItemRepository.saveAndFlush(ratedItem(1L, savedBook.getId(), "4.2"));
        libraryItemRepository.saveAndFlush(ratedItem(2L, savedBook.getId(), "4.4"));
        when(activityCountReader.getActivityCounts(List.of(savedBook.getId())))
                .thenReturn(Map.of(savedBook.getId(), new ActivityCounts(0L, 0L)));
        when(currentMemberIdProvider.findCurrentMemberId()).thenReturn(OptionalLong.empty());
        when(bookResolver.lookup(savedBook.getIsbn13())).thenReturn(savedBook);

        // when
        var response = bookService.getDetail(savedBook.getIsbn13());

        // then
        assertThat(response.authors()).containsExactly("앤디 위어", "공동 저자");
        assertThat(response.translators()).containsExactly("박아람", "공동 번역가");
        assertThat(response.averageRating()).isEqualByComparingTo("4.3");
        assertThat(response.ratingCount()).isEqualTo(2);
    }

    private LibraryItem ratedItem(long memberId, long bookId, String rating) {
        LibraryItem item = LibraryItem.create(
                memberId, bookId, ReadingStatus.WANT_TO_READ, null, Instant.parse("2026-08-14T00:00:00Z"));
        item.rate(new BigDecimal(rating), Instant.parse("2026-08-14T00:00:00Z"));
        return item;
    }
}
