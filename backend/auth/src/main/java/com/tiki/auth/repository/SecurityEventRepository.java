package com.tiki.auth.repository;

import com.tiki.auth.entity.SecurityEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {
    
    Page<SecurityEvent> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    List<SecurityEvent> findByUserIdAndEventTypeAndCreatedAtAfter(
        Long userId, 
        SecurityEvent.EventType eventType, 
        LocalDateTime after
    );
    
    long countByUserIdAndEventTypeAndCreatedAtAfter(
        Long userId, 
        SecurityEvent.EventType eventType, 
        LocalDateTime after
    );
    
    void deleteByCreatedAtBefore(LocalDateTime dateTime);
}
