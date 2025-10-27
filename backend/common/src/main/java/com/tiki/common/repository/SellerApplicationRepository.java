package com.tiki.common.repository;

import com.tiki.common.entity.SellerApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SellerApplicationRepository extends JpaRepository<SellerApplication, Long> {
    
    Optional<SellerApplication> findByUserIdAndStatus(Long userId, SellerApplication.Status status);
    
    List<SellerApplication> findByStatus(SellerApplication.Status status);
    
    List<SellerApplication> findByUserId(Long userId);
    
    boolean existsByUserIdAndStatus(Long userId, SellerApplication.Status status);
}
