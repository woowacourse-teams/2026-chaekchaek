package com.chaekchaek.book.fixture;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

public final class BookFixture {

    private BookFixture() {
    }

    public static Book book(Isbn13 isbn13, String title, String coverImageUrl, List<String> authors) {
        return Book.create(
                isbn13,
                title,
                coverImageUrl,
                null,
                null,
                null,
                authors,
                List.of(),
                "출판사",
                null,
                null,
                null,
                null,
                null
        );
    }

    public static Book martian(Isbn13 isbn13, Integer totalPages) {
        return Book.create(
                isbn13,
                "마션",
                "https://example.com/cover.jpg",
                "https://example.com/cover.jpg/side",
                "https://example.com/cover.jpg/back",
                null,
                List.of("앤디 위어"),
                List.of(),
                "알에이치코리아",
                "SF",
                null,
                totalPages,
                null,
                null
        );
    }

    public static Book savedBook(long id, Book book) {
        ReflectionTestUtils.setField(book, "id", id);
        return book;
    }
}
