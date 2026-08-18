package com.chaekchaek.home.service;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.home.dto.PopularBookListResponse;
import com.chaekchaek.home.dto.PopularBookResponse;
import com.chaekchaek.review.repository.ReviewRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;

    @Transactional(readOnly = true)
    public PopularBookListResponse getPopularBooks() {
        List<ReviewRepository.PopularBookCount> popularBookCounts = reviewRepository.findTop10PopularBookCounts();
        List<Long> bookIds = popularBookCounts.stream().map(ReviewRepository.PopularBookCount::getBookId).toList();
        Map<Long, Book> books = booksById(bookIds);
        List<PopularBookResponse> responses = popularBookCounts.stream()
                .map(count -> toPopularBookResponse(books.get(count.getBookId()), count))
                .filter(Objects::nonNull)
                .toList();
        return new PopularBookListResponse(responses);
    }

    private Map<Long, Book> booksById(List<Long> bookIds) {
        if (bookIds.isEmpty()) {
            return Map.of();
        }
        return bookRepository.findAllWithAuthorsByIdIn(bookIds).stream()
                .collect(java.util.stream.Collectors.toMap(Book::getId, book -> book));
    }

    private PopularBookResponse toPopularBookResponse(Book book, ReviewRepository.PopularBookCount count) {
        if (book == null) {
            return null;
        }
        long bookId = book.getId();
        return new PopularBookResponse(bookId, book.getTitle(), book.getCoverImageUrl(), book.getAuthors(),
                count.getReviewCount(), count.getReplyCount());
    }
}
