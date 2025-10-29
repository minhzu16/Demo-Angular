package com.tiki.auth.repository;

import com.tiki.auth.entity.OAuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {
    
    Optional<OAuthAccount> findByProviderAndProviderUserId(
        OAuthAccount.Provider provider, 
        String providerUserId
    );
    
    List<OAuthAccount> findByUserId(Long userId);
    
    boolean existsByProviderAndProviderUserId(
        OAuthAccount.Provider provider, 
        String providerUserId
    );
    
    void deleteByUserId(Long userId);
}
