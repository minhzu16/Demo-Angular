package com.tiki.order.repository;

import com.tiki.order.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    List<OutboxEventEntity> findTop50ByStatusOrderByCreatedAtAsc(String status);

    @Query("SELECT o FROM OutboxEventEntity o WHERE o.status = :status AND o.retryCount < :maxRetries ORDER BY o.createdAt ASC")
    List<OutboxEventEntity> findPendingEventsForPublishing(@Param("status") String status, @Param("maxRetries") int maxRetries);
}
