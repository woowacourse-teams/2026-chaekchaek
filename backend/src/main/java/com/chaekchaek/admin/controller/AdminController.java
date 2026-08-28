package com.chaekchaek.admin.controller;

import com.chaekchaek.admin.dto.AddRecommendedBookRequest;
import com.chaekchaek.admin.dto.RecommendedBookListResponse;
import com.chaekchaek.admin.dto.RecommendedBookResponse;
import com.chaekchaek.admin.service.AdminService;
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
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/recommended-books")
    public ResponseEntity<RecommendedBookListResponse> getRecommendedBooks() {
        return ResponseEntity.ok(adminService.getRecommendedBooks());
    }

    @PostMapping("/recommended-books")
    public ResponseEntity<RecommendedBookResponse> addRecommendedBook(
            @Valid @RequestBody AddRecommendedBookRequest request
    ) {
        RecommendedBookResponse response = adminService.addRecommendedBookByIsbn13(request.isbn13());
        return ResponseEntity.created(URI.create("/api/v1/admin/recommended-books/" + response.bookId()))
                .body(response);
    }

    @DeleteMapping("/recommended-books/{bookId}")
    public ResponseEntity<Void> deleteRecommendedBook(@PathVariable long bookId) {
        adminService.deleteRecommendedBook(bookId);
        return ResponseEntity.noContent().build();
    }
}
