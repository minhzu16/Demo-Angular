package com.tiki.live.repository;

import com.tiki.live.entity.LiveProductPinEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LiveProductPinRepository extends JpaRepository<LiveProductPinEntity, Long> {

    List<LiveProductPinEntity> findBySessionIdOrderByDisplayOrderAsc(Long sessionId);

    Optional<LiveProductPinEntity> findBySessionIdAndIsCurrentlyPinnedTrue(Long sessionId);

    /**
     * Row-locked: quickBuy does read-modify-write on soldCount, and without the lock two buyers could both pass
     * the stock-limit check and oversell the livestream quota. Callers are all @Transactional.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LiveProductPinEntity> findBySessionIdAndProductId(Long sessionId, Long productId);
}
