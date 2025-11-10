package com.tiki.cart.repository;

import com.tiki.cart.entity.CartEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<CartEntity, Integer> {
    
    @Query("SELECT c FROM CartEntity c WHERE c.userId = :userId AND c.isActive = :isActive ORDER BY c.createdAt DESC LIMIT 1")
    Optional<CartEntity> findByUserIdAndIsActive(@Param("userId") Integer userId, @Param("isActive") Boolean isActive);
    
    @Query("SELECT c FROM CartEntity c WHERE c.sessionId = :sessionId AND c.isActive = :isActive ORDER BY c.createdAt DESC LIMIT 1")
    Optional<CartEntity> findBySessionIdAndIsActive(@Param("sessionId") String sessionId, @Param("isActive") Boolean isActive);
}
