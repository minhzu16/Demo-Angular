package com.tiki.payment.controller;

import com.tiki.payment.dto.StoreCreditOperationRequest;
import com.tiki.payment.dto.StoreCreditResponseDto;
import com.tiki.payment.dto.StoreCreditTransactionDto;
import com.tiki.payment.entity.StoreCreditTransactionEntity;
import com.tiki.payment.service.StoreCreditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/store-credit")
@RequiredArgsConstructor
public class StoreCreditController {

    private final StoreCreditService storeCreditService;

    @GetMapping("/balance")
    public ResponseEntity<StoreCreditResponseDto> getBalance(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(storeCreditService.getBalance(userId));
    }

    @GetMapping("/user/{userId}/balance")
    public ResponseEntity<StoreCreditResponseDto> getUserBalance(@PathVariable Long userId) {
        return ResponseEntity.ok(storeCreditService.getBalance(userId));
    }

    @PostMapping("/add")
    public ResponseEntity<StoreCreditTransactionDto> addCredit(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody StoreCreditOperationRequest req) {
        StoreCreditTransactionEntity.TransactionType type = StoreCreditTransactionEntity.TransactionType.ADMIN_ADJUSTMENT;
        if (req.getType() != null) {
            try {
                type = StoreCreditTransactionEntity.TransactionType.valueOf(req.getType());
            } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(storeCreditService.addCredit(
                userId, req.getAmount(), type, req.getReferenceId(), req.getNote()));
    }

    @PostMapping("/deduct")
    public ResponseEntity<StoreCreditTransactionDto> deductCredit(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody StoreCreditOperationRequest req) {
        return ResponseEntity.ok(storeCreditService.deductCredit(
                userId, req.getAmount(), req.getReferenceId(), req.getNote()));
    }

    @GetMapping("/history")
    public ResponseEntity<Page<StoreCreditTransactionDto>> getHistory(
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(storeCreditService.getTransactionHistory(userId, pageable));
    }
}
