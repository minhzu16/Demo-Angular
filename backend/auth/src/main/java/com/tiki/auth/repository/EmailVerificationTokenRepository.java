package com.tiki.auth.repository;

import com.tiki.auth.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    
    Optional<EmailVerificationToken> findByToken(String token);
    
    Optional<EmailVerificationToken> findByUserIdAndVerifiedAtIsNull(Long userId);
    
    void deleteByUserId(Long userId);
    
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
