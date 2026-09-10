package com.tiki.auth.repository;

import com.tiki.auth.entity.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
    Page<AdminAuditLog> findByAction(String action, Pageable pageable);
    Page<AdminAuditLog> findByTargetType(String targetType, Pageable pageable);
    Page<AdminAuditLog> findByAdminId(Long adminId, Pageable pageable);
    Page<AdminAuditLog> findByActionAndTargetType(String action, String targetType, Pageable pageable);
    Page<AdminAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
