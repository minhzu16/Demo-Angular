package com.tiki.auth.repository;

import com.tiki.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    Optional<PasswordResetToken> findByToken(String token);
    
    void deleteByUserId(Long userId);
    
    void deleteByExpiresAtBeforeAndUsedAtIsNull(LocalDateTime dateTime);
}
