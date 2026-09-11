package com.tiki.product.controller;

import com.tiki.product.dto.CompareDto;
import com.tiki.product.service.CompareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/compare")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class CompareController {

    private final CompareService compareService;

    /**
     * Lấy danh sách sản phẩm đang so sánh của người dùng
     * GET /api/v1/compare
     */
    @GetMapping
    public ResponseEntity<List<CompareDto>> getCompareList(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(value = "userId", required = false) Long paramUserId) {
        Long targetUserId = (userId != null) ? userId : paramUserId;
        if (targetUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(compareService.getCompareList(targetUserId));
    }

    /**
     * Thêm một sản phẩm vào danh sách so sánh (tối đa 4 sản phẩm)
     * POST /api/v1/compare/{productId}
     */
    @PostMapping("/{productId}")
    public ResponseEntity<?> addToCompare(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(value = "userId", required = false) Long paramUserId,
            @PathVariable Integer productId) {
        Long targetUserId = (userId != null) ? userId : paramUserId;
        if (targetUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Chưa đăng nhập"));
        }
        try {
            CompareDto dto = compareService.addToCompare(targetUserId, productId);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Xóa một sản phẩm khỏi danh sách so sánh
     * DELETE /api/v1/compare/{productId}
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeFromCompare(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(value = "userId", required = false) Long paramUserId,
            @PathVariable Integer productId) {
        Long targetUserId = (userId != null) ? userId : paramUserId;
        if (targetUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        compareService.removeFromCompare(targetUserId, productId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Xóa toàn bộ danh sách so sánh
     * DELETE /api/v1/compare
     */
    @DeleteMapping
    public ResponseEntity<Void> clearCompare(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(value = "userId", required = false) Long paramUserId) {
        Long targetUserId = (userId != null) ? userId : paramUserId;
        if (targetUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        compareService.clearCompare(targetUserId);
        return ResponseEntity.noContent().build();
    }
}
