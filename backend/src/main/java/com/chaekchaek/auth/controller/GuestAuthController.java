package com.chaekchaek.auth.controller;

import com.chaekchaek.actor.domain.Actor;
import com.chaekchaek.auth.token.guest.GuestTokenService;
import com.chaekchaek.auth.token.guest.IssuedGuestToken;
import com.chaekchaek.common.auth.ActorType;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/guest-token")
@RequiredArgsConstructor
public class GuestAuthController {

    private final GuestTokenService guestTokenService;

    @GetMapping
    public ResponseEntity<GuestInfoResponse> getGuestInfo(
            @RequestHeader(name = "X-Guest-Token", required = false) String guestToken
    ) {
        Actor actor = guestTokenService.findUsableActor(guestToken);
        return ResponseEntity.ok(new GuestInfoResponse(
                actor.getGuestNickname(),
                actor.getExpiresAt(),
                actor.getId(),
                actor.getType()
        ));
    }

    @PostMapping
    public ResponseEntity<GuestTokenResponse> issue() {
        IssuedGuestToken token = guestTokenService.issue();
        return ResponseEntity.status(201).body(new GuestTokenResponse(
                token.value(), token.nickname(), token.expiresAt(), token.actorId(), token.actorType()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<GuestTokenResponse> refresh(
            @RequestHeader(name = "X-Guest-Token", required = false) String guestToken
    ) {
        IssuedGuestToken token = guestTokenService.refresh(guestToken);
        return ResponseEntity.ok(new GuestTokenResponse(
                token.value(), token.nickname(), token.expiresAt(), token.actorId(), token.actorType()));
    }

    public record GuestTokenResponse(
            String guestToken,
            String nickname,
            LocalDateTime expiresAt,
            long actorId,
            ActorType actorType
    ) {
    }

    public record GuestInfoResponse(
            String nickname,
            LocalDateTime expiresAt,
            long actorId,
            ActorType actorType
    ) {
    }
}
