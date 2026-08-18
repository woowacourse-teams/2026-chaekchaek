package com.chaekchaek.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.common.auth.CurrentMemberIdProvider;
import com.chaekchaek.library.domain.LibraryItem;
import com.chaekchaek.library.domain.ReadingStatus;
import com.chaekchaek.library.repository.LibraryItemRepository;
import com.chaekchaek.library.service.BookCommentCountReader;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BookDetailAssemblerTest {

    @Test
    @DisplayName("책 상세에 댓글 수와 반올림한 평균 별점 및 내 기록을 결합한다")
    void should_CombineBookStatisticsAndMyRecord_When_AssemblingDetail() {
        // given
        Book book = book();
        BookCommentCountReader commentCountReader = mock(BookCommentCountReader.class);
        CurrentMemberIdProvider currentMemberIdProvider = mock(CurrentMemberIdProvider.class);
        LibraryItemRepository libraryItemRepository = mock(LibraryItemRepository.class);
        LibraryItemRepository.RatingStatistics ratingStatistics = mock(
                LibraryItemRepository.RatingStatistics.class);
        LibraryItem libraryItem = mock(LibraryItem.class);
        BookDetailAssembler assembler = new BookDetailAssembler(
                commentCountReader, currentMemberIdProvider, libraryItemRepository);
        when(commentCountReader.getCommentCounts(List.of(1L))).thenReturn(Map.of(1L, 4L));
        when(libraryItemRepository.findRatingStatisticsByBookIdIn(List.of(1L)))
                .thenReturn(List.of(ratingStatistics));
        when(ratingStatistics.getAverageRating()).thenReturn(4.24);
        when(ratingStatistics.getRatingCount()).thenReturn(3L);
        when(currentMemberIdProvider.findCurrentMemberId()).thenReturn(OptionalLong.of(10L));
        when(libraryItemRepository.findByMemberIdAndBookId(10L, 1L)).thenReturn(Optional.of(libraryItem));
        when(libraryItem.getStatus()).thenReturn(ReadingStatus.READING);
        when(libraryItem.getCurrentPage()).thenReturn(120);
        when(libraryItem.getRating()).thenReturn(new java.math.BigDecimal("4.2"));

        // when
        var response = assembler.assemble(book);

        // then
        assertThat(response.commentCount()).isEqualTo(4);
        assertThat(response.averageRating()).isEqualByComparingTo("4.2");
        assertThat(response.ratingCount()).isEqualTo(3);
        assertThat(response.myRecord()).extracting("status", "currentPage", "myRating")
                .containsExactly("READING", 120, new java.math.BigDecimal("4.2"));
    }

    @Test
    @DisplayName("로그인했지만 서재 기록이 없으면 책 상세의 내 기록은 null이다")
    void should_ReturnNullMyRecord_When_AuthenticatedMemberHasNoLibraryItem() {
        // given
        Book book = book();
        BookCommentCountReader commentCountReader = mock(BookCommentCountReader.class);
        CurrentMemberIdProvider currentMemberIdProvider = mock(CurrentMemberIdProvider.class);
        LibraryItemRepository libraryItemRepository = mock(LibraryItemRepository.class);
        BookDetailAssembler assembler = new BookDetailAssembler(
                commentCountReader, currentMemberIdProvider, libraryItemRepository);
        when(commentCountReader.getCommentCounts(List.of(1L))).thenReturn(Map.of(1L, 0L));
        when(libraryItemRepository.findRatingStatisticsByBookIdIn(List.of(1L))).thenReturn(List.of());
        when(currentMemberIdProvider.findCurrentMemberId()).thenReturn(OptionalLong.of(10L));
        when(libraryItemRepository.findByMemberIdAndBookId(10L, 1L)).thenReturn(Optional.empty());

        // when
        var response = assembler.assemble(book);

        // then
        assertThat(response.myRecord()).isNull();
    }

    private Book book() {
        Book book = mock(Book.class);
        when(book.getId()).thenReturn(1L);
        when(book.getIsbn13()).thenReturn("9788925568683");
        when(book.getTitle()).thenReturn("마션");
        when(book.getCoverImageUrl()).thenReturn("https://image.example/martian.jpg");
        when(book.getAuthors()).thenReturn(List.of("앤디 위어"));
        when(book.getTranslators()).thenReturn(List.of("박아람"));
        when(book.getPublisher()).thenReturn("알에이치코리아");
        when(book.getCategory()).thenReturn("SF");
        when(book.getPublishedDate()).thenReturn(LocalDate.of(2026, 1, 1));
        when(book.getTotalPages()).thenReturn(308);
        return book;
    }
}
