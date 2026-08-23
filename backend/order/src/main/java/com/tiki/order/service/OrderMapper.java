package com.tiki.order.service;

import com.tiki.order.dto.OrderDto;
import com.tiki.order.entity.OrderEntity;
import com.tiki.order.entity.OrderItemEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OrderMapper {

    public OrderDto toDto(OrderEntity order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        if (order.getUserId() != null) {
            dto.setUserId(order.getUserId().intValue());
        }
        dto.setOrderNumber(order.getOrderNumber());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setSubtotal(order.getSubtotal());
        dto.setShippingFee(order.getShippingFee());
        dto.setStatus(order.getStatus() != null ? order.getStatus().name() : null);
        dto.setCreatedAt(order.getCreatedAt());
        dto.setPaidAt(order.getPaidAt());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setPaymentStatus(order.getPaymentStatus());
        dto.setUsePoints(order.getUsePoints());
        dto.setPointsDiscount(order.getPointsDiscount());
        dto.setVoucherCode(order.getVoucherCode());
        dto.setVoucherDiscount(order.getVoucherDiscount());

        OrderDto.ShippingAddress sa = new OrderDto.ShippingAddress();
        sa.setFullName(order.getCustomerName());
        sa.setPhoneNumber(order.getCustomerPhone());
        sa.setProvince(order.getShippingProvince());
        sa.setDistrict(order.getShippingDistrict());
        sa.setWard("");
        sa.setStreet(order.getShippingAddress());
        dto.setShippingAddress(sa);

        List<OrderDto.Item> items = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItemEntity orderItem : order.getItems()) {
                OrderDto.Item item = new OrderDto.Item();
                item.setProductId(orderItem.getProductId().intValue());
                item.setProductName(orderItem.getProductName());
                item.setQuantity(orderItem.getQuantity());
                item.setUnitPrice(orderItem.getPrice());
                item.setImageUrl(orderItem.getImageUrl());
                items.add(item);
            }
        }
        dto.setItems(items);
        return dto;
    }
}
