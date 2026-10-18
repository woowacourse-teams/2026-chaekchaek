package com.chaekchaek.admin.controller;

import com.chaekchaek.admin.dto.AddRecommendedBookRequest;
import com.chaekchaek.admin.dto.RecommendedBookListResponse;
import com.chaekchaek.admin.dto.RecommendedBookResponse;
import com.chaekchaek.admin.service.AdminService;
import com.chaekchaek.book.domain.Isbn13;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommended-books")
@RequiredArgsConstructor
public class RecommendedBookController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<RecommendedBookListResponse> getRecommendedBooks() {
        return ResponseEntity.ok(adminService.getRecommendedBooks());
    }

    /** 관리자 전용 API */
    @PostMapping
    public ResponseEntity<RecommendedBookResponse> addRecommendedBook(
            @Valid @RequestBody AddRecommendedBookRequest request
    ) {
        RecommendedBookResponse response = adminService.addRecommendedBookByIsbn13(new Isbn13(request.isbn13()));
        return ResponseEntity.created(URI.create("/api/v1/recommended-books/" + response.bookId()))
                .body(response);
    }

    /** 관리자 전용 API */
    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> deleteRecommendedBook(@PathVariable long bookId) {
        adminService.deleteRecommendedBook(bookId);
        return ResponseEntity.noContent().build();
    }
}
