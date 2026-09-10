package com.tiki.order.repository;

import com.tiki.order.entity.RmaItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RmaItemRepository extends JpaRepository<RmaItemEntity, Long> {
    List<RmaItemEntity> findByRmaId(Long rmaId);
}
