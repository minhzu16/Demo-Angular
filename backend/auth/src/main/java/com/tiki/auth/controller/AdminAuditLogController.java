package com.tiki.auth.controller;

import com.tiki.auth.dto.AdminAuditLogDto;
import com.tiki.auth.dto.CreateAuditLogRequest;
import com.tiki.auth.service.AdminAuditLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@Slf4j
public class AdminAuditLogController {

    private final AdminAuditLogService auditLogService;

    @PostMapping
    public ResponseEntity<AdminAuditLogDto> logAction(
            @Valid @RequestBody CreateAuditLogRequest request) {
        log.info("Admin audit log recorded: action={}, targetType={}", request.getAction(), request.getTargetType());
        return ResponseEntity.status(HttpStatus.CREATED).body(auditLogService.logAction(request));
    }

    @GetMapping
    public ResponseEntity<Page<AdminAuditLogDto>> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long adminId,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("Querying admin audit logs: action={}, targetType={}, adminId={}", action, targetType, adminId);
        return ResponseEntity.ok(auditLogService.getAuditLogs(action, targetType, adminId, pageable));
    }
}
