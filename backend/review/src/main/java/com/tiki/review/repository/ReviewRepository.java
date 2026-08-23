package com.tiki.review.repository;

import com.tiki.review.entity.ReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
    Page<ReviewEntity> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);
    List<ReviewEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** ✅ BUG 29 FIX: Prevent a user from reviewing the same product twice */
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    /**
     * ✅ BUG 14 FIX: Efficient DB-level average rating — no in-memory loading of all reviews.
     * Always reflects current state, including after edits and deletions.
     */
    @Query("SELECT AVG(r.rating) FROM ReviewEntity r WHERE r.productId = :productId")
    Optional<Double> findAverageRatingByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) FROM ReviewEntity r WHERE r.productId = :productId")
    long countByProductId(@Param("productId") Long productId);
}
