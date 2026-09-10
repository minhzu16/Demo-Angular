package com.tiki.auth.service;

import com.tiki.auth.dto.AdminAuditLogDto;
import com.tiki.auth.dto.CreateAuditLogRequest;
import com.tiki.auth.entity.AdminAuditLog;
import com.tiki.auth.repository.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAuditLogService {

    private final AdminAuditLogRepository auditLogRepository;

    @Transactional
    public AdminAuditLogDto logAction(CreateAuditLogRequest request) {
        log.info("Recording admin audit log: adminId={}, action={}, targetType={}, targetId={}",
                request.getAdminId(), request.getAction(), request.getTargetType(), request.getTargetId());

        AdminAuditLog entity = AdminAuditLog.builder()
                .adminId(request.getAdminId())
                .adminEmail(request.getAdminEmail())
                .action(request.getAction().trim().toUpperCase())
                .targetType(request.getTargetType().trim().toUpperCase())
                .targetId(request.getTargetId())
                .detailsJson(request.getDetailsJson())
                .ipAddress(request.getIpAddress())
                .build();

        AdminAuditLog saved = auditLogRepository.save(entity);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<AdminAuditLogDto> getAuditLogs(String action, String targetType, Long adminId, Pageable pageable) {
        Page<AdminAuditLog> page;

        if (action != null && !action.isBlank() && targetType != null && !targetType.isBlank()) {
            page = auditLogRepository.findByActionAndTargetType(action.trim().toUpperCase(), targetType.trim().toUpperCase(), pageable);
        } else if (action != null && !action.isBlank()) {
            page = auditLogRepository.findByAction(action.trim().toUpperCase(), pageable);
        } else if (targetType != null && !targetType.isBlank()) {
            page = auditLogRepository.findByTargetType(targetType.trim().toUpperCase(), pageable);
        } else if (adminId != null) {
            page = auditLogRepository.findByAdminId(adminId, pageable);
        } else {
            page = auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return page.map(this::toDto);
    }

    private AdminAuditLogDto toDto(AdminAuditLog entity) {
        return AdminAuditLogDto.builder()
                .id(entity.getId())
                .adminId(entity.getAdminId())
                .adminEmail(entity.getAdminEmail())
                .action(entity.getAction())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .detailsJson(entity.getDetailsJson())
                .ipAddress(entity.getIpAddress())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
