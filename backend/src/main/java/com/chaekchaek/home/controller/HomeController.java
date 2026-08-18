package com.chaekchaek.home.controller;

import com.chaekchaek.home.dto.PopularBookListResponse;
import com.chaekchaek.home.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/popular-books")
    public ResponseEntity<PopularBookListResponse> getPopularBooks() {
        return ResponseEntity.ok(homeService.getPopularBooks());
    }
}
