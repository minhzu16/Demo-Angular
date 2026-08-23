package com.tiki.auth.repository;

import com.tiki.auth.entity.LoyaltyTransaction;
import com.tiki.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Long> {
    
    List<LoyaltyTransaction> findByUserOrderByCreatedAtDesc(User user);
    
    Page<LoyaltyTransaction> findByUserId(Long userId, Pageable pageable);

    Page<LoyaltyTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
