package com.chaekchaek.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.home.dto.PopularBookResponse;
import com.chaekchaek.review.repository.ReviewRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HomeServiceTest {

    @Test
    @DisplayName("집계된 인기 순서에 맞춰 책 정보를 반환한다")
    void should_ReturnBooksInPopularOrder_When_FindingPopularBooks() {
        // given
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        BookRepository bookRepository = mock(BookRepository.class);
        HomeService homeService = new HomeService(reviewRepository, bookRepository);
        List<ReviewRepository.PopularBookCount> popularBookCounts = List.of(
                popularBookCount(3L, 2L, 8L), popularBookCount(2L, 5L, 5L), popularBookCount(1L, 8L, 1L)
        );
        List<Book> books = List.of(
                book(1L, "첫 번째 책"), book(2L, "두 번째 책"), book(3L, "세 번째 책")
        );
        when(reviewRepository.findTop10PopularBookCounts()).thenReturn(popularBookCounts);
        when(bookRepository.findAllWithAuthorsByIdIn(List.of(3L, 2L, 1L))).thenReturn(books);

        // when
        List<PopularBookResponse> result = homeService.getPopularBooks().books();

        // then
        assertThat(result).extracting(PopularBookResponse::bookId).containsExactly(3L, 2L, 1L);
        assertThat(result).extracting(PopularBookResponse::reviewCount).containsExactly(2L, 5L, 8L);
        assertThat(result).extracting(PopularBookResponse::replyCount).containsExactly(8L, 5L, 1L);
    }

    private static ReviewRepository.PopularBookCount popularBookCount(long bookId, long reviewCount, long replyCount) {
        ReviewRepository.PopularBookCount countProjection = mock(ReviewRepository.PopularBookCount.class);
        when(countProjection.getBookId()).thenReturn(bookId);
        when(countProjection.getReviewCount()).thenReturn(reviewCount);
        when(countProjection.getReplyCount()).thenReturn(replyCount);
        return countProjection;
    }

    private static Book book(long id, String title) {
        Book book = mock(Book.class);
        when(book.getId()).thenReturn(id);
        when(book.getTitle()).thenReturn(title);
        when(book.getCoverImageUrl()).thenReturn("https://example.com/" + id + ".jpg");
        when(book.getAuthors()).thenReturn(List.of("저자 " + id));
        return book;
    }
}
