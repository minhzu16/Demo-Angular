package com.tiki.auth.repository;

import com.tiki.auth.entity.ActiveSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActiveSessionRepository extends JpaRepository<ActiveSession, Long> {
    
    List<ActiveSession> findByUserIdAndRevokedAtIsNullAndExpiresAtAfter(
        Long userId, LocalDateTime now);
    
    Optional<ActiveSession> findBySessionToken(String sessionToken);
    
    void deleteByUserIdAndRevokedAtIsNotNull(Long userId);
    
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
