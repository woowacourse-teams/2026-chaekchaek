package com.chaekchaek.auth.controller;

import com.chaekchaek.auth.token.guest.GuestTokenService;
import com.chaekchaek.auth.token.guest.IssuedGuestToken;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/guest-token")
@RequiredArgsConstructor
public class GuestAuthController {

    private final GuestTokenService guestTokenService;

    @PostMapping
    public ResponseEntity<GuestTokenResponse> issue() {
        IssuedGuestToken token = guestTokenService.issue();
        return ResponseEntity.status(201).body(new GuestTokenResponse(
                token.value(), token.nickname(), token.expiresAt()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<GuestTokenResponse> refresh(
            @RequestHeader(name = "X-Guest-Token", required = false) String guestToken
    ) {
        IssuedGuestToken token = guestTokenService.refresh(guestToken);
        return ResponseEntity.ok(new GuestTokenResponse(
                token.value(), token.nickname(), token.expiresAt()));
    }

    public record GuestTokenResponse(String guestToken, String nickname, LocalDateTime expiresAt) {
    }
}
