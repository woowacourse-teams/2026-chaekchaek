package com.chaekchaek.library.controller;

import com.chaekchaek.common.auth.CurrentMemberIdProvider;
import com.chaekchaek.library.domain.LibrarySort;
import com.chaekchaek.library.domain.ReadingStatus;
import com.chaekchaek.library.dto.LibraryCollectionResponse;
import com.chaekchaek.library.dto.LibraryDetailResponse;
import com.chaekchaek.library.dto.LibraryListResponse;
import com.chaekchaek.library.dto.PublicLibraryListResponse;
import com.chaekchaek.library.service.LibraryQueryService;
import com.chaekchaek.library.service.LibraryService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class LibraryV2Controller {

    private final LibraryService libraryService;
    private final LibraryQueryService libraryQueryService;
    private final CurrentMemberIdProvider currentMemberIdProvider;

    @GetMapping("/library")
    public ResponseEntity<LibraryListResponse> getLibrary(
            @RequestParam @Positive int page,
            @RequestParam(required = false) ReadingStatus status,
            @RequestParam(required = false, defaultValue = "RECENT") LibrarySort sort
    ) {
        long memberId = currentMemberIdProvider.getCurrentMemberId();
        return ResponseEntity.ok(libraryService.getLibrary(memberId, page, status, sort));
    }

    @GetMapping("/members/{memberId}/library")
    public ResponseEntity<PublicLibraryListResponse> getPublicLibrary(
            @PathVariable @Positive long memberId,
            @RequestParam @Positive int page,
            @RequestParam(required = false) ReadingStatus status,
            @RequestParam(required = false, defaultValue = "RECENT") LibrarySort sort
    ) {
        return ResponseEntity.ok(libraryService.getPublicLibrary(memberId, page, status, sort));
    }

    @GetMapping("/libraries")
    public ResponseEntity<LibraryCollectionResponse> findLibraries(@RequestParam @Positive long memberId) {
        return ResponseEntity.ok(libraryQueryService.findLibraries(memberId));
    }

    @GetMapping("/libraries/{libraryId}")
    public ResponseEntity<LibraryDetailResponse> findLibrary(@PathVariable @Positive long libraryId) {
        return ResponseEntity.ok(libraryQueryService.findLibrary(libraryId));
    }
}
