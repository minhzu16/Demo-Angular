package com.tiki.order.repository;

import com.tiki.order.entity.InvoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, Long> {
    Optional<InvoiceEntity> findByOrderId(Integer orderId);
    List<InvoiceEntity> findByOrderIdIn(List<Integer> orderIds);
}
