package com.tiki.auth.controller;

import com.tiki.auth.dto.AuthResponse;
import com.tiki.auth.dto.OAuthAccountDto;
import com.tiki.auth.dto.OAuthLoginRequest;
import com.tiki.auth.entity.OAuthAccount;
import com.tiki.auth.service.OAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth/oauth")
@RequiredArgsConstructor
@Slf4j
public class OAuthController {

    private final OAuthService oauthService;

    /**
     * Public endpoint: Login or register with OAuth provider (Google, Facebook)
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginWithOAuth(@Valid @RequestBody OAuthLoginRequest request) {
        log.info("Received OAuth login request for provider: {}", request.getProvider());
        return ResponseEntity.ok(oauthService.loginOrRegister(request));
    }

    /**
     * Authenticated endpoint: Link OAuth account to current authenticated user
     */
    @PostMapping("/link")
    public ResponseEntity<OAuthAccountDto> linkOAuth(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody OAuthLoginRequest request) {
        log.info("Received OAuth link request for userId={}, provider={}", userId, request.getProvider());
        return ResponseEntity.ok(oauthService.linkOAuth(userId, request));
    }

    /**
     * Authenticated endpoint: Get list of linked OAuth accounts for current user
     */
    @GetMapping("/accounts")
    public ResponseEntity<List<OAuthAccountDto>> getLinkedAccounts(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(oauthService.getLinkedAccounts(userId));
    }

    /**
     * Authenticated endpoint: Unlink OAuth provider from current user
     */
    @DeleteMapping("/accounts/{provider}")
    public ResponseEntity<Void> unlinkOAuth(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable OAuthAccount.Provider provider) {
        log.info("Received OAuth unlink request for userId={}, provider={}", userId, provider);
        oauthService.unlinkOAuth(userId, provider);
        return ResponseEntity.noContent().build();
    }
}
