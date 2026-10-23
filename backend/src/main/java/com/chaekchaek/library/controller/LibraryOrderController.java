package com.chaekchaek.library.controller;

import com.chaekchaek.common.auth.CurrentMemberIdProvider;
import com.chaekchaek.library.dto.BookOrderRequest;
import com.chaekchaek.library.dto.LibraryOrderRequest;
import com.chaekchaek.library.service.LibraryOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/libraries")
@RequiredArgsConstructor
public class LibraryOrderController {

    private final LibraryOrderService libraryOrderService;
    private final CurrentMemberIdProvider currentMemberIdProvider;

    @PatchMapping("/order")
    public ResponseEntity<Void> reorderLibraries(@Valid @RequestBody LibraryOrderRequest request) {
        libraryOrderService.reorderLibraries(currentMemberIdProvider.getCurrentMemberIdForWrite(), request.libraryIds());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{libraryId}/books/order")
    public ResponseEntity<Void> reorderBooks(
            @PathVariable @Positive long libraryId,
            @Valid @RequestBody BookOrderRequest request
    ) {
        libraryOrderService.reorderBooks(currentMemberIdProvider.getCurrentMemberIdForWrite(), libraryId, request.bookIds());
        return ResponseEntity.noContent().build();
    }
}
