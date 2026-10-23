package com.chaekchaek.library.controller;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.library.dto.LibraryCollectionResponse;
import com.chaekchaek.library.dto.LibraryDetailResponse;
import com.chaekchaek.library.service.LibraryQueryService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/libraries")
public class LibraryQueryController {

    private final LibraryQueryService libraryQueryService;

    @GetMapping
    public ResponseEntity<LibraryCollectionResponse> findLibraries(@RequestParam @Positive long memberId) {
        return ResponseEntity.ok(libraryQueryService.findLibraries(memberId));
    }

    @GetMapping("/{libraryId}")
    public ResponseEntity<LibraryDetailResponse> findLibrary(
            @PathVariable @Positive long libraryId,
            @RequestParam(required = false) String page
    ) {
        return ResponseEntity.ok(libraryQueryService.findLibrary(libraryId, pageNumber(page)));
    }

    private int pageNumber(String page) {
        if (page == null) {
            return 1;
        }
        if (!page.matches("[0-9]+")) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        try {
            int value = Integer.parseInt(page);
            if (value > 0) {
                return value;
            }
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        throw new BusinessException(ErrorCode.INVALID_REQUEST);
    }
}
