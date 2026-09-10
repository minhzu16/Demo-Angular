package com.tiki.auth.service;

import com.tiki.auth.dto.AdminAuditLogDto;
import com.tiki.auth.dto.CreateAuditLogRequest;
import com.tiki.auth.entity.AdminAuditLog;
import com.tiki.auth.repository.AdminAuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuditLogServiceTest {

    @Mock
    private AdminAuditLogRepository auditLogRepository;

    @InjectMocks
    private AdminAuditLogService auditLogService;

    @Test
    @DisplayName("logAction - successfully records admin modification")
    void testLogAction_Success() {
        CreateAuditLogRequest request = CreateAuditLogRequest.builder()
                .adminId(1L)
                .adminEmail("admin@tiki.vn")
                .action("UPDATE_PRICE")
                .targetType("PRODUCT")
                .targetId("101")
                .detailsJson("{\"oldPrice\": 100000, \"newPrice\": 85000}")
                .ipAddress("192.168.1.50")
                .build();

        AdminAuditLog saved = AdminAuditLog.builder()
                .id(10L)
                .adminId(1L)
                .adminEmail("admin@tiki.vn")
                .action("UPDATE_PRICE")
                .targetType("PRODUCT")
                .targetId("101")
                .detailsJson("{\"oldPrice\": 100000, \"newPrice\": 85000}")
                .ipAddress("192.168.1.50")
                .createdAt(LocalDateTime.now())
                .build();

        when(auditLogRepository.save(any(AdminAuditLog.class))).thenReturn(saved);

        AdminAuditLogDto result = auditLogService.logAction(request);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getAction()).isEqualTo("UPDATE_PRICE");
        assertThat(result.getTargetType()).isEqualTo("PRODUCT");
        assertThat(result.getAdminEmail()).isEqualTo("admin@tiki.vn");
        verify(auditLogRepository).save(any(AdminAuditLog.class));
    }

    @Test
    @DisplayName("getAuditLogs - filter by action and targetType")
    void testGetAuditLogs_FilterActionAndTargetType() {
        Pageable pageable = PageRequest.of(0, 10);
        AdminAuditLog logItem = AdminAuditLog.builder()
                .id(1L)
                .adminId(1L)
                .action("UPDATE_STOCK")
                .targetType("WAREHOUSE")
                .build();

        when(auditLogRepository.findByActionAndTargetType("UPDATE_STOCK", "WAREHOUSE", pageable))
                .thenReturn(new PageImpl<>(List.of(logItem)));

        Page<AdminAuditLogDto> result = auditLogService.getAuditLogs("UPDATE_STOCK", "WAREHOUSE", null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAction()).isEqualTo("UPDATE_STOCK");
        assertThat(result.getContent().get(0).getTargetType()).isEqualTo("WAREHOUSE");
    }

    @Test
    @DisplayName("getAuditLogs - filter by adminId")
    void testGetAuditLogs_FilterByAdminId() {
        Pageable pageable = PageRequest.of(0, 10);
        AdminAuditLog logItem = AdminAuditLog.builder()
                .id(2L)
                .adminId(99L)
                .action("RESOLVE_COMPLAINT")
                .targetType("COMPLAINT")
                .build();

        when(auditLogRepository.findByAdminId(99L, pageable))
                .thenReturn(new PageImpl<>(List.of(logItem)));

        Page<AdminAuditLogDto> result = auditLogService.getAuditLogs(null, null, 99L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAdminId()).isEqualTo(99L);
        assertThat(result.getContent().get(0).getAction()).isEqualTo("RESOLVE_COMPLAINT");
    }
}
