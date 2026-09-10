package com.tiki.order.repository;

import com.tiki.order.entity.RmaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RmaRepository extends JpaRepository<RmaEntity, Long> {

    Optional<RmaEntity> findByRmaNumber(String rmaNumber);

    List<RmaEntity> findByOrderId(Integer orderId);

    Page<RmaEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<RmaEntity> findByShopIdOrderByCreatedAtDesc(Long shopId, Pageable pageable);

    Page<RmaEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT count(r) FROM RmaEntity r WHERE r.userId = :userId AND r.status IN :statuses")
    long countByUserIdAndStatusIn(
            @Param("userId") Long userId,
            @Param("statuses") Collection<RmaEntity.RmaStatus> statuses);
}
