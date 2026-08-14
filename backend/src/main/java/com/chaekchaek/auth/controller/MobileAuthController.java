package com.chaekchaek.auth.controller;

import com.chaekchaek.auth.dto.MobileGoogleLoginRequest;
import com.chaekchaek.auth.dto.MobileTokenResponse;
import com.chaekchaek.auth.service.MobileGoogleLoginService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/mobile")
public class MobileAuthController {

    private final MobileGoogleLoginService mobileGoogleLoginService;

    public MobileAuthController(MobileGoogleLoginService mobileGoogleLoginService) {
        this.mobileGoogleLoginService = mobileGoogleLoginService;
    }

    @PostMapping("/google")
    public ResponseEntity<MobileTokenResponse> googleLogin(
            @Valid @RequestBody
            MobileGoogleLoginRequest request
    ) {
        MobileTokenResponse response = mobileGoogleLoginService.login(request.idToken());

        return ResponseEntity.ok(response);
    }
}
