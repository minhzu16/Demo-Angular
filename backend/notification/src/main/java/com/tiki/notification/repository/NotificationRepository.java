package com.tiki.notification.repository;

import com.tiki.notification.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    Page<NotificationEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<NotificationEntity> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
}
