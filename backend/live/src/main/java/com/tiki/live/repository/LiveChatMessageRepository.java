package com.tiki.live.repository;

import com.tiki.live.entity.LiveChatMessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LiveChatMessageRepository extends JpaRepository<LiveChatMessageEntity, Long> {
    Page<LiveChatMessageEntity> findBySessionIdOrderByCreatedAtDesc(Long sessionId, Pageable pageable);
}
