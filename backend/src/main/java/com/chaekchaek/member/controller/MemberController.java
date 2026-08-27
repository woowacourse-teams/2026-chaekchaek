package com.chaekchaek.member.controller;

import com.chaekchaek.auth.token.cookie.AuthCookieProvider;
import com.chaekchaek.member.dto.MemberResponse;
import com.chaekchaek.member.dto.UpdateAnonymityRequest;
import com.chaekchaek.member.dto.UpdateNicknameRequest;
import com.chaekchaek.member.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
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

    private static final Logger log = LoggerFactory.getLogger(MemberController.class);

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
            @Valid @RequestBody UpdateNicknameRequest request,
            HttpServletRequest httpRequest
    ) {
        Long memberId = Long.valueOf(jwt.getSubject());
        String authSource = httpRequest.getHeader(HttpHeaders.AUTHORIZATION) == null
                ? "ACCESS_TOKEN_COOKIE"
                : "AUTHORIZATION_HEADER";
        String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);
        log.info("Nickname update requested: memberId={}, authSource={}, userAgent={}",
                memberId, authSource, userAgent);

        MemberResponse response = memberService.updateNickname(memberId, request.nickname());
        log.info("Nickname update committed: memberId={}, authSource={}, userAgent={}",
                memberId, authSource, userAgent);

        return ResponseEntity.ok(response);
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
