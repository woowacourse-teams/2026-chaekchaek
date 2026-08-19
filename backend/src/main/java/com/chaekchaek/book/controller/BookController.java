package com.chaekchaek.book.controller;

import com.chaekchaek.book.dto.BookDetailResponse;
import com.chaekchaek.book.dto.BookSearchResponse;
import com.chaekchaek.book.service.BookService;
import com.chaekchaek.book.service.BookSearchService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class BookController {

    private final BookSearchService bookSearchService;
    private final BookService bookService;

    @GetMapping("/api/v1/books")
    public ResponseEntity<BookSearchResponse> search(
            @RequestParam @NotBlank String query,
            @RequestParam @Positive int page
    ) {
        return ResponseEntity.ok(bookSearchService.search(query, page));
    }

    @GetMapping("/api/v1/books/by-isbn/{isbn13}")
    public ResponseEntity<BookDetailResponse> getDetail(@PathVariable String isbn13) {
        return ResponseEntity.ok(bookService.getDetail(isbn13));
    }
}
