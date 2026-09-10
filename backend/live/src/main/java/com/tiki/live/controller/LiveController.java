package com.tiki.live.controller;

import com.tiki.live.dto.*;
import com.tiki.live.service.LiveChatService;
import com.tiki.live.service.LiveSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/live")
@RequiredArgsConstructor
public class LiveController {

    private final LiveSessionService liveSessionService;
    private final LiveChatService liveChatService;

    @PostMapping("/sessions")
    public ResponseEntity<LiveSessionResponseDto> createSession(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long sellerId,
            @Valid @RequestBody LiveSessionCreateRequest req) {
        return ResponseEntity.ok(liveSessionService.createSession(sellerId, req));
    }

    @PostMapping("/sessions/{id}/start")
    public ResponseEntity<LiveSessionResponseDto> startLive(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long sellerId) {
        return ResponseEntity.ok(liveSessionService.startLive(id, sellerId));
    }

    @PostMapping("/sessions/{id}/end")
    public ResponseEntity<LiveSessionResponseDto> endLive(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long sellerId) {
        return ResponseEntity.ok(liveSessionService.endLive(id, sellerId));
    }

    @GetMapping("/sessions/active")
    public ResponseEntity<List<LiveSessionResponseDto>> getActiveSessions() {
        return ResponseEntity.ok(liveSessionService.getActiveSessions());
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<LiveSessionResponseDto> getSessionById(@PathVariable Long id) {
        return ResponseEntity.ok(liveSessionService.getSessionById(id));
    }

    @PostMapping("/sessions/{id}/pin-product")
    public ResponseEntity<LiveProductPinDto> pinProduct(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long sellerId,
            @Valid @RequestBody LivePinProductRequest req) {
        return ResponseEntity.ok(liveSessionService.pinProduct(id, sellerId, req));
    }

    @DeleteMapping("/sessions/{id}/pin-product/{productId}")
    public ResponseEntity<Void> unpinProduct(
            @PathVariable Long id,
            @PathVariable Long productId,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long sellerId) {
        liveSessionService.unpinProduct(id, productId, sellerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sessions/{id}/quick-buy")
    public ResponseEntity<LiveQuickBuyResponseDto> quickBuy(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @Valid @RequestBody LiveQuickBuyRequest req) {
        return ResponseEntity.ok(liveSessionService.quickBuy(id, req.getProductId(), userId, req.getQuantity()));
    }

    @GetMapping("/sessions/{id}/products")
    public ResponseEntity<List<LiveProductPinDto>> getSessionProducts(@PathVariable Long id) {
        return ResponseEntity.ok(liveSessionService.getSessionProducts(id));
    }

    @GetMapping("/sessions/{id}/analytics")
    public ResponseEntity<LiveAnalyticsDto> getSessionAnalytics(@PathVariable Long id) {
        return ResponseEntity.ok(liveSessionService.getSessionAnalytics(id));
    }

    @PostMapping("/sessions/{id}/like")
    public ResponseEntity<Void> likeSession(@PathVariable Long id) {
        liveSessionService.likeSession(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sessions/{id}/viewers")
    public ResponseEntity<Void> updateViewers(
            @PathVariable Long id,
            @RequestParam int count) {
        liveSessionService.updateViewerCount(id, count);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sessions/{id}/chat")
    public ResponseEntity<LiveChatMessageDto> postMessage(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "User") String userName,
            @Valid @RequestBody LiveChatMessageDto dto) {
        return ResponseEntity.ok(liveChatService.postMessage(
                id, userId, userName, dto.getMessage(), dto.getType()));
    }

    @GetMapping("/sessions/{id}/chat")
    public ResponseEntity<Page<LiveChatMessageDto>> getChatMessages(
            @PathVariable Long id,
            @PageableDefault(size = 30) Pageable pageable) {
        return ResponseEntity.ok(liveChatService.getMessages(id, pageable));
    }
}
