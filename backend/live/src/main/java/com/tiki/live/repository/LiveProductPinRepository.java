package com.tiki.live.repository;

import com.tiki.live.entity.LiveProductPinEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LiveProductPinRepository extends JpaRepository<LiveProductPinEntity, Long> {

    List<LiveProductPinEntity> findBySessionIdOrderByDisplayOrderAsc(Long sessionId);

    Optional<LiveProductPinEntity> findBySessionIdAndIsCurrentlyPinnedTrue(Long sessionId);

    Optional<LiveProductPinEntity> findBySessionIdAndProductId(Long sessionId, Long productId);
}
