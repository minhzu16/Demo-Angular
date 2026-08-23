package com.tiki.order.service;

import com.tiki.order.dto.OrderDto;
import com.tiki.order.entity.OrderEntity;
import com.tiki.order.entity.OrderItemEntity;
import com.tiki.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Cacheable(cacheNames = "orders", key = "#orderId")
    public OrderDto getOrder(Integer orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order không tồn tại"));
        return orderMapper.toDto(order);
    }

    @Cacheable(cacheNames = "all-orders")
    public List<OrderDto> getAllOrders() {
        List<OrderEntity> entities = orderRepository.findAll();
        List<OrderDto> dtos = new ArrayList<>();
        for (OrderEntity e : entities) {
            dtos.add(orderMapper.toDto(e));
        }
        return dtos;
    }

    @Cacheable(cacheNames = "user-orders", key = "#userId")
    public List<OrderDto> getOrdersByUser(Integer userId) {
        return getOrdersByUserAndStatus(userId, null);
    }

    @Cacheable(cacheNames = "user-orders-status", key = "#userId + '-' + (#status != null ? #status.name() : 'ALL')")
    public List<OrderDto> getOrdersByUserAndStatus(Integer userId, OrderEntity.OrderStatus status) {
        List<OrderEntity> entities;
        if (status != null) {
            entities = orderRepository.findByUserIdAndStatus(userId.longValue(), status);
        } else {
            entities = orderRepository.findByUserId(userId.longValue());
        }
        List<OrderDto> dtos = new ArrayList<>();
        for (OrderEntity e : entities) {
            dtos.add(orderMapper.toDto(e));
        }
        return dtos;
    }

    /**
     * Check if user purchased a product (for reviews verification)
     */
    public Boolean hasUserPurchasedProduct(Long userId, Long productId) {
        List<OrderEntity> orders = orderRepository.findByUserId(userId);
        for (OrderEntity order : orders) {
            if (order.getStatus() == OrderEntity.OrderStatus.DELIVERED) {
                if (order.getItems() != null) {
                    for (OrderItemEntity item : order.getItems()) {
                        if (item.getProductId() != null && item.getProductId().equals(productId)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
