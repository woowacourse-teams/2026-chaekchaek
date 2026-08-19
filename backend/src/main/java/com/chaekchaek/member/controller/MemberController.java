package com.chaekchaek.member.controller;

import com.chaekchaek.auth.token.cookie.AuthCookieProvider;
import com.chaekchaek.member.dto.MemberResponse;
import com.chaekchaek.member.dto.UpdateAnonymityRequest;
import com.chaekchaek.member.dto.UpdateNicknameRequest;
import com.chaekchaek.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members/me")
public class MemberController {

    private final MemberService memberService;
    private final AuthCookieProvider authCookieProvider;

    @GetMapping()
    public ResponseEntity<MemberResponse> getMyInfo(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long memberId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok(memberService.getMyInfo(memberId));
    }

    @PatchMapping("/nickname")
    public ResponseEntity<MemberResponse> updateNickname(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateNicknameRequest request
    ) {
        return ResponseEntity.ok(memberService.updateNickname(
                Long.valueOf(jwt.getSubject()),
                request.nickname()
        ));
    }

    @PatchMapping("/anonymity")
    public ResponseEntity<MemberResponse> updateAnonymity(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateAnonymityRequest request
    ) {
        return ResponseEntity.ok(memberService.updateAnonymity(
                Long.valueOf(jwt.getSubject()),
                request.displayAnonymous()
        ));
    }

    @DeleteMapping()
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long memberId = Long.valueOf(jwt.getSubject());
        memberService.withdraw(memberId);

        return ResponseEntity.noContent()
                .header("Set-Cookie", authCookieProvider.deleteAccessTokenCookie().toString())
                .header("Set-Cookie", authCookieProvider.deleteRefreshTokenCookie().toString())
                .build();
    }
}
