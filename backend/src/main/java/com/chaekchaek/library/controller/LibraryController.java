package com.chaekchaek.library.controller;

import com.chaekchaek.common.auth.CurrentMemberIdProvider;
import com.chaekchaek.library.domain.LibrarySort;
import com.chaekchaek.library.domain.ReadingStatus;
import com.chaekchaek.library.dto.AddLibraryItemRequest;
import com.chaekchaek.library.dto.BulkDeleteLibraryItemsRequest;
import com.chaekchaek.library.dto.BulkUpdateLibraryStatusRequest;
import com.chaekchaek.library.dto.LibraryItemResponse;
import com.chaekchaek.library.dto.LibraryListResponse;
import com.chaekchaek.library.dto.RateBookRequest;
import com.chaekchaek.library.dto.RatingComparisonResponse;
import com.chaekchaek.library.dto.UpdateLibraryItemRequest;
import com.chaekchaek.library.service.LibraryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LibraryController {

    private final LibraryService libraryService;
    private final CurrentMemberIdProvider currentMemberIdProvider;

    @GetMapping("/library")
    public ResponseEntity<LibraryListResponse> getLibrary(
            @RequestParam @Positive int page,
            @RequestParam(required = false) ReadingStatus status,
            @RequestParam(required = false, defaultValue = "RECENT") LibrarySort sort
    ) {
        return ResponseEntity.ok(libraryService.getLibrary(memberId(), page, status, sort));
    }

    @PostMapping("/library")
    public ResponseEntity<LibraryItemResponse> add(@Valid @RequestBody AddLibraryItemRequest request) {
        LibraryItemResponse response = libraryService.addByIsbn13(memberId(), request.isbn13(),
                request.status(), request.totalPages());
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{bookId}").buildAndExpand(response.bookId()).toUri())
                .body(response);
    }

    @PatchMapping("/library/{bookId}")
    public ResponseEntity<LibraryItemResponse> update(@PathVariable long bookId,
                                                      @Valid @RequestBody UpdateLibraryItemRequest request) {
        return ResponseEntity.ok(libraryService.update(memberId(), bookId, request.status(),
                request.currentPage(), request.totalPages()));
    }

    @DeleteMapping("/library/{bookId}")
    public ResponseEntity<Void> delete(@PathVariable long bookId) {
        libraryService.delete(memberId(), bookId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/library/bulk-delete")
    public ResponseEntity<Void> bulkDelete(@Valid @RequestBody BulkDeleteLibraryItemsRequest request) {
        if (request.hasDuplicateBookIds()) {
            throw new IllegalArgumentException("Book IDs must not be duplicated");
        }
        libraryService.bulkDelete(memberId(), request.bookIds());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/library/bulk-status")
    public ResponseEntity<Void> bulkChangeStatus(
            @Valid @RequestBody BulkUpdateLibraryStatusRequest request
    ) {
        if (request.hasDuplicateBookIds()) {
            throw new IllegalArgumentException("Book IDs must not be duplicated");
        }
        libraryService.bulkChangeStatus(memberId(), request.bookIds(), request.status());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/library/{bookId}/rating")
    public ResponseEntity<LibraryItemResponse> rate(@PathVariable long bookId,
                                                    @Valid @RequestBody RateBookRequest request) {
        return ResponseEntity.ok(libraryService.rate(memberId(), bookId, request.rating()));
    }

    @DeleteMapping("/library/{bookId}/rating")
    public ResponseEntity<Void> removeRating(@PathVariable long bookId) {
        libraryService.removeRating(memberId(), bookId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/members/me/ratings/comparison")
    public ResponseEntity<RatingComparisonResponse> compareRatings(
            @RequestParam @NotBlank String isbn13,
            @RequestParam @NotNull @DecimalMin("0.1") @DecimalMax("5.0") @Digits(integer = 1, fraction = 1)
            java.math.BigDecimal criterion
    ) {
        return ResponseEntity.ok(libraryService.compareRatingsByIsbn13(memberId(), isbn13, criterion));
    }

    private long memberId() {
        return currentMemberIdProvider.getCurrentMemberId();
    }
}
