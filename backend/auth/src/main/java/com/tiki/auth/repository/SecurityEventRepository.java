package com.tiki.auth.repository;

import com.tiki.auth.entity.SecurityEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {
    
    Page<SecurityEvent> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    long countByUserIdAndEventTypeAndCreatedAtAfter(
        Long userId, SecurityEvent.EventType eventType, LocalDateTime after);
}
