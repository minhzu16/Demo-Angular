package com.tiki.product.repository;

import com.tiki.product.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    @Query("SELECT r FROM Review r WHERE r.product.id = :productId AND (r.isApproved = true OR r.isApproved IS NULL) ORDER BY r.createdAt DESC")
    Page<Review> findByProductIdAndIsApprovedTrueOrderByCreatedAtDesc(@Param("productId") Long productId, Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.product.id = :productId AND (r.isApproved = true OR r.isApproved IS NULL) ORDER BY r.createdAt DESC")
    List<Review> findByProductIdAndIsApprovedTrueOrderByCreatedAtDesc(@Param("productId") Long productId);

    @Query("SELECT r FROM Review r WHERE r.product.id = :productId AND r.userId = :userId")
    Optional<Review> findByProductIdAndUserId(@Param("productId") Long productId, @Param("userId") Long userId);
    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND (r.isApproved = true OR r.isApproved IS NULL)")
    Double getAverageRatingByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId AND (r.isApproved = true OR r.isApproved IS NULL)")
    Long getReviewCountByProductId(@Param("productId") Long productId);

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.product.id = :productId AND (r.isApproved = true OR r.isApproved IS NULL) GROUP BY r.rating ORDER BY r.rating DESC")
    List<Object[]> getRatingDistributionByProductId(@Param("productId") Long productId);
}
