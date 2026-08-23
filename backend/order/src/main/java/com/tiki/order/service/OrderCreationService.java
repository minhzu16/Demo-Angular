package com.tiki.order.service;

import com.tiki.common.dto.UserDto;
import com.tiki.common.event.OrderCreatedEvent;
import com.tiki.order.client.UserClient;
import com.tiki.order.client.WarehouseClient;
import com.tiki.order.dto.CreateOrderRequest;
import com.tiki.order.dto.OrderDto;
import com.tiki.order.dto.ValidateVoucherRequest;
import com.tiki.order.dto.VoucherValidationResponse;
import com.tiki.order.entity.OrderEntity;
import com.tiki.order.entity.OrderItemEntity;
import com.tiki.order.entity.OrderTrackingEntity;
import com.tiki.order.enums.PaymentMethod;
import com.tiki.order.enums.PaymentStatus;
import com.tiki.order.repository.OrderRepository;
import com.tiki.order.repository.OrderTrackingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderCreationService {

    private final UserClient userClient;
    private final OrderRepository orderRepository;
    private final OrderTrackingRepository orderTrackingRepository;
    private final WarehouseClient warehouseClient;
    private final RabbitTemplate rabbitTemplate;
    private final VoucherService voucherService;
    private final OrderMapper orderMapper;

    @CacheEvict(cacheNames = {"all-orders", "user-orders", "user-orders-status"}, allEntries = true)
    public OrderDto createOrder(CreateOrderRequest request) {
        UserDto userProfile = null;
        if (request.getUserId() != null) {
            try {
                userProfile = userClient.getUser(request.getUserId().longValue());
            } catch (Exception ex) {
                throw new IllegalArgumentException("ID người dùng không hợp lệ: " + request.getUserId());
            }
        }

        OrderEntity order = new OrderEntity();
        if (request.getUserId() != null) {
            order.setUserId(request.getUserId().longValue());
        }

        CreateOrderRequest.ShippingAddressDto addr = request.getShippingAddress();
        if (addr != null) {
            order.setCustomerName(addr.getFullName());
            order.setCustomerPhone(addr.getPhoneNumber());
            order.setShippingProvince(addr.getProvince());
            order.setShippingDistrict(addr.getDistrict());
            order.setShippingAddress(addr.getStreet());
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        if (request.getItems() != null) {
            for (CreateOrderRequest.OrderItemDto item : request.getItems()) {
                if (item.getUnitPrice() != null && item.getQuantity() != null) {
                    subtotal = subtotal.add(item.getUnitPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity())));
                }

                OrderItemEntity itemEntity = new OrderItemEntity();
                Long productId = item.getProductId() != null ? item.getProductId().longValue() : 0L;
                int quantity = item.getQuantity() != null ? item.getQuantity() : 1;
                itemEntity.setProductId(productId);
                itemEntity.setProductName(item.getProductName());
                itemEntity.setImageUrl(item.getImageUrl());
                itemEntity.setPrice(item.getUnitPrice());
                itemEntity.setQuantity(quantity);
                order.addItem(itemEntity);

                // Kiểm tra tồn kho nghiêm ngặt — fail nếu không đủ hàng
                try {
                    warehouseClient.reserveStock(productId, quantity);
                } catch (Exception e) {
                    log.error("Stock reservation failed for product {}: {}", productId, e.getMessage());
                    throw new IllegalStateException(
                        "Sản phẩm '" + item.getProductName() + "' không đủ số lượng tồn kho. Vui lòng giảm số lượng hoặc chọn sản phẩm khác.");
                }
            }
        }
        order.setSubtotal(subtotal.max(BigDecimal.ZERO));

        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            ValidateVoucherRequest vreq = new ValidateVoucherRequest();
            vreq.setCode(request.getVoucherCode());
            vreq.setOrderTotal(subtotal);
            VoucherValidationResponse vres = voucherService.validateVoucher(vreq);
            if (vres.getValid()) {
                order.setVoucherCode(request.getVoucherCode());
                order.setVoucherDiscount(vres.getDiscountAmount());
                voucherService.applyVoucher(request.getVoucherCode());
            } else {
                order.setVoucherDiscount(BigDecimal.ZERO);
            }
        } else {
            order.setVoucherDiscount(BigDecimal.ZERO);
        }

        if (request.getUsePoints() != null && request.getUsePoints() > 0) {
            if (userProfile == null) {
                throw new IllegalArgumentException("Cần đăng nhập để sử dụng điểm thưởng");
            }
            int userPoints = userProfile.getLoyaltyPoints() != null ? userProfile.getLoyaltyPoints() : 0;
            if (request.getUsePoints() > userPoints) {
                throw new IllegalArgumentException("Số dư điểm không đủ. Bạn có " + userPoints + " điểm.");
            }

            BigDecimal pointsDiscount = BigDecimal.valueOf(request.getUsePoints());
            order.setUsePoints(request.getUsePoints());
            order.setPointsDiscount(pointsDiscount);

            try {
                userClient.updatePoints(userProfile.getId(), -request.getUsePoints());
                log.info("Deducted {} points from user {}", request.getUsePoints(), userProfile.getId());
            } catch (Exception e) {
                log.error("Failed to deduct points for user {}", userProfile.getId(), e);
                throw new RuntimeException("Lỗi khi khấu trừ điểm thưởng. Vui lòng thử lại.");
            }
        }

        BigDecimal shippingFee = BigDecimal.ZERO;
        if (subtotal.compareTo(BigDecimal.ZERO) > 0 && subtotal.compareTo(new BigDecimal("500000")) < 0) {
            shippingFee = new BigDecimal("30000");
        }
        order.setShippingFee(shippingFee);
        order.calculateTotal();

        if (request.getPaymentMethod() != null) {
            order.setPaymentMethod(request.getPaymentMethod());
        } else {
            order.setPaymentMethod(PaymentMethod.COD);
        }
        order.setPaymentStatus(PaymentStatus.PENDING);

        OrderEntity saved = orderRepository.save(order);

        orderTrackingRepository.save(new OrderTrackingEntity(saved.getId(), saved.getStatus(), "Đơn hàng đã được tạo"));

        List<com.tiki.common.event.OrderItemDto> eventItems = saved.getItems().stream()
                .map(item -> com.tiki.common.event.OrderItemDto.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .imageUrl(item.getImageUrl())
                        .build())
                .collect(Collectors.toList());

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(saved.getId())
                .userId(saved.getUserId().intValue())
                .totalAmount(saved.getTotalAmount())
                .status(saved.getStatus().name())
                .paymentMethod(saved.getPaymentMethod().name())
                .createdAt(saved.getCreatedAt())
                .items(eventItems)
                .build();

        rabbitTemplate.convertAndSend("tiki.events", "order.created", event);

        return orderMapper.toDto(saved);
    }
}
