package com.tiki.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiki.common.dto.UserDto;
import com.tiki.common.event.OrderCreatedEvent;
import com.tiki.order.client.ProductClient;
import com.tiki.order.client.UserClient;
import com.tiki.order.client.WarehouseClient;
import com.tiki.order.dto.CreateOrderRequest;
import com.tiki.order.dto.OrderDto;
import com.tiki.order.dto.ProductPricingDto;
import com.tiki.order.dto.ValidateVoucherRequest;
import com.tiki.order.dto.VoucherValidationResponse;
import com.tiki.order.entity.OrderEntity;
import com.tiki.order.entity.OrderIdempotencyEntity;
import com.tiki.order.entity.OrderItemEntity;
import com.tiki.order.entity.OrderTrackingEntity;
import com.tiki.order.entity.OutboxEventEntity;
import com.tiki.order.enums.PaymentMethod;
import com.tiki.order.enums.PaymentStatus;
import com.tiki.order.repository.OrderIdempotencyRepository;
import com.tiki.order.repository.OrderRepository;
import com.tiki.order.repository.OrderTrackingRepository;
import com.tiki.order.repository.OutboxEventRepository;
import com.tiki.order.saga.CheckoutSagaOrchestrator;
import com.tiki.order.saga.CheckoutSagaState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final FraudDetectionService fraudDetectionService;
    private final com.tiki.order.client.PaymentClient paymentClient;
    private final ProductClient productClient;
    private final CheckoutSagaOrchestrator sagaOrchestrator;
    private final OrderIdempotencyRepository orderIdempotencyRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${order.shipping.free-threshold:500000}")
    private BigDecimal freeShippingThreshold = new BigDecimal("500000");

    @org.springframework.beans.factory.annotation.Value("${order.shipping.standard-fee:30000}")
    private BigDecimal standardShippingFee = new BigDecimal("30000");

    @CacheEvict(cacheNames = {"all-orders", "user-orders", "user-orders-status"}, allEntries = true)
    public OrderDto createOrder(CreateOrderRequest request) {
        String idempKey = request.getIdempotencyKey();
        OrderIdempotencyEntity idempotencyEntity = null;

        // --- IDEMPOTENCY CHECK ---
        if (idempKey != null && !idempKey.isBlank()) {
            Optional<OrderIdempotencyEntity> existingOpt = orderIdempotencyRepository.findByIdempotencyKey(idempKey);
            if (existingOpt.isPresent()) {
                OrderIdempotencyEntity existing = existingOpt.get();
                if ("COMPLETED".equals(existing.getStatus())) {
                    log.info("Idempotent hit for key: {}. Returning cached order response.", idempKey);
                    if (existing.getResponseBody() != null && !existing.getResponseBody().isBlank()) {
                        try {
                            return objectMapper.readValue(existing.getResponseBody(), OrderDto.class);
                        } catch (Exception e) {
                            log.warn("Failed to deserialize cached response body for key: {}. Falling back to DB lookup.", idempKey);
                        }
                    }
                    if (existing.getOrderId() != null) {
                        return orderRepository.findById(existing.getOrderId())
                                .map(orderMapper::toDto)
                                .orElseThrow(() -> new IllegalStateException("Đơn hàng không tồn tại."));
                    }
                } else if ("IN_PROGRESS".equals(existing.getStatus())) {
                    if (existing.getCreatedAt() != null && existing.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(2))) {
                        throw new IllegalStateException("Yêu cầu đặt hàng đang được xử lý. Vui lòng không gửi lặp lại (Conflict).");
                    }
                }
            }

            idempotencyEntity = existingOpt.orElseGet(() -> OrderIdempotencyEntity.builder()
                    .idempotencyKey(idempKey)
                    .requestHash(calculateRequestHash(request))
                    .status("IN_PROGRESS")
                    .expiresAt(LocalDateTime.now().plusDays(1))
                    .build());
            idempotencyEntity.setStatus("IN_PROGRESS");
            idempotencyEntity.setExpiresAt(LocalDateTime.now().plusDays(1));
            orderIdempotencyRepository.save(idempotencyEntity);
        }

        // --- CHECKOUT SAGA EXECUTION WITH COMPENSATING TRANSACTIONS ---
        CheckoutSagaState sagaState = new CheckoutSagaState();
        try {
            OrderDto result = executeCheckoutSaga(request, sagaState);

            // Record completed idempotency
            if (idempotencyEntity != null) {
                idempotencyEntity.setStatus("COMPLETED");
                idempotencyEntity.setOrderId(result.getId());
                try {
                    idempotencyEntity.setResponseBody(objectMapper.writeValueAsString(result));
                } catch (Exception ignored) {}
                orderIdempotencyRepository.save(idempotencyEntity);
            }

            return result;
        } catch (Exception ex) {
            log.error("Checkout execution failed, executing Saga compensation: {}", ex.getMessage());
            sagaOrchestrator.compensate(sagaState, ex.getMessage());

            if (idempotencyEntity != null) {
                idempotencyEntity.setStatus("FAILED");
                orderIdempotencyRepository.save(idempotencyEntity);
            }
            throw ex;
        }
    }

    private OrderDto executeCheckoutSaga(CreateOrderRequest request, CheckoutSagaState sagaState) {
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

        // SECURITY: Authoritative Server-side catalog and price lookup
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng phải có ít nhất một sản phẩm.");
        }

        List<Integer> productIds = request.getItems().stream()
                .map(CreateOrderRequest.OrderItemDto::getProductId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        List<ProductPricingDto> productPricings;
        try {
            productPricings = productClient.getBatchPricing(productIds);
        } catch (Exception e) {
            log.error("Failed to fetch product pricing from product-service: {}", e.getMessage());
            throw new IllegalStateException("Không thể kiểm tra giá sản phẩm từ hệ thống. Vui lòng thử lại sau.");
        }

        Map<Integer, ProductPricingDto> pricingMap = productPricings != null
                ? productPricings.stream().collect(Collectors.toMap(ProductPricingDto::getProductId, p -> p, (p1, p2) -> p1))
                : Collections.emptyMap();

        BigDecimal subtotal = BigDecimal.ZERO;

        // Step 1: Inventory Reservation (with Saga tracking)
        for (CreateOrderRequest.OrderItemDto item : request.getItems()) {
            Long productId = item.getProductId() != null ? item.getProductId().longValue() : 0L;
            int quantity = item.getQuantity() != null ? item.getQuantity() : 1;

            if (quantity <= 0) {
                throw new IllegalArgumentException("Số lượng của sản phẩm (ID: " + productId + ") không hợp lệ.");
            }

            ProductPricingDto serverProduct = pricingMap.get(item.getProductId());
            if (serverProduct == null) {
                throw new IllegalArgumentException("Sản phẩm ID " + productId + " không tồn tại hoặc đã ngừng kinh doanh.");
            }

            BigDecimal unitPrice = serverProduct.getPrice() != null ? serverProduct.getPrice() : BigDecimal.ZERO;
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));

            OrderItemEntity itemEntity = new OrderItemEntity();
            itemEntity.setProductId(productId);
            Long shopId = serverProduct.getShopId() != null && serverProduct.getShopId() > 0
                    ? serverProduct.getShopId()
                    : (item.getShopId() != null ? item.getShopId() : request.getShopId());
            itemEntity.setShopId(shopId);
            if (order.getShopId() == null && shopId != null) {
                order.setShopId(shopId);
            }
            itemEntity.setProductName(serverProduct.getName());
            itemEntity.setImageUrl(serverProduct.getThumbnailUrl());
            itemEntity.setPrice(unitPrice);
            itemEntity.setQuantity(quantity);
            order.addItem(itemEntity);

            // Reserve stock via warehouse service
            try {
                Boolean reserved = warehouseClient.reserveStock(productId, quantity);
                if (Boolean.FALSE.equals(reserved)) {
                    throw new IllegalStateException("Hết hàng tồn kho");
                }
                sagaOrchestrator.recordStockReservation(sagaState, productId, quantity);
            } catch (Exception e) {
                log.error("Stock reservation failed for product {}: {}", productId, e.getMessage());
                throw new IllegalStateException(
                        "Sản phẩm '" + serverProduct.getName() + "' không đủ số lượng tồn kho. Vui lòng giảm số lượng hoặc chọn sản phẩm khác.");
            }
        }

        if (order.getShopId() == null && request.getShopId() != null) {
            order.setShopId(request.getShopId());
        }
        order.setSubtotal(subtotal.max(BigDecimal.ZERO));

        // Shipping fee calculation
        BigDecimal shippingFee = BigDecimal.ZERO;
        if (subtotal.compareTo(BigDecimal.ZERO) > 0 && subtotal.compareTo(freeShippingThreshold) < 0) {
            shippingFee = standardShippingFee;
        }

        // Membership Freeship Benefit
        if (shippingFee.compareTo(BigDecimal.ZERO) > 0 && request.getUserId() != null) {
            try {
                Long uid = request.getUserId().longValue();
                com.tiki.order.dto.MembershipBenefitCheckDto benefits = userClient.checkMemberBenefits(uid);
                if (benefits != null && benefits.isMember() && benefits.isFreeShipping() && benefits.getFreeShipRemaining() > 0) {
                    shippingFee = BigDecimal.ZERO;
                    userClient.useFreeShip(uid);
                    log.info("Applied membership free-shipping for user {}", uid);
                }
            } catch (Exception e) {
                log.warn("Could not verify membership freeship for user {}: {}", request.getUserId(), e.getMessage());
            }
        }
        order.setShippingFee(shippingFee);

        // Step 2: Voucher Application (with Saga tracking)
        BigDecimal voucherDiscount = BigDecimal.ZERO;
        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            ValidateVoucherRequest vreq = new ValidateVoucherRequest();
            vreq.setCode(request.getVoucherCode());
            vreq.setOrderTotal(subtotal);
            vreq.setShopId(order.getShopId());
            VoucherValidationResponse vres = voucherService.validateVoucher(vreq);
            if (vres.getValid()) {
                voucherDiscount = vres.getDiscountAmount();
                order.setVoucherCode(request.getVoucherCode());
                order.setVoucherDiscount(voucherDiscount);
                voucherService.applyVoucher(request.getVoucherCode());
                sagaOrchestrator.recordVoucherApplication(sagaState, request.getVoucherCode());
            } else {
                throw new IllegalArgumentException(vres.getMessage() != null ? vres.getMessage() : "Voucher không hợp lệ hoặc đã hết hạn: " + request.getVoucherCode());
            }
        } else {
            order.setVoucherDiscount(BigDecimal.ZERO);
        }

        // Step 3: Loyalty points deduction (with Saga tracking)
        if (request.getUsePoints() != null && request.getUsePoints() > 0) {
            if (userProfile == null) {
                throw new IllegalArgumentException("Cần đăng nhập để sử dụng điểm thưởng");
            }
            int userPoints = userProfile.getLoyaltyPoints() != null ? userProfile.getLoyaltyPoints() : 0;
            if (request.getUsePoints() > userPoints) {
                throw new IllegalArgumentException("Số dư điểm không đủ. Bạn có " + userPoints + " điểm.");
            }

            BigDecimal currentTotal = subtotal.add(shippingFee).subtract(voucherDiscount);
            if (BigDecimal.valueOf(request.getUsePoints()).compareTo(currentTotal) > 0) {
                throw new IllegalArgumentException("Số điểm sử dụng không được vượt quá tổng giá trị đơn hàng.");
            }

            BigDecimal pointsDiscount = BigDecimal.valueOf(request.getUsePoints());
            order.setUsePoints(request.getUsePoints());
            order.setPointsDiscount(pointsDiscount);

            try {
                userClient.updatePoints(userProfile.getId(), -request.getUsePoints());
                sagaOrchestrator.recordPointsDeduction(sagaState, userProfile.getId(), request.getUsePoints());
                log.info("Deducted {} points from user {}", request.getUsePoints(), userProfile.getId());
            } catch (Exception e) {
                log.error("Failed to deduct points for user {}", userProfile.getId(), e);
                throw new RuntimeException("Lỗi khi khấu trừ điểm thưởng. Vui lòng thử lại.");
            }
        }

        order.calculateTotal();

        // Optional Gift Card & Store Credit integration
        if (request.getGiftCardCode() != null && !request.getGiftCardCode().isBlank() && paymentClient != null) {
            try {
                paymentClient.applyGiftCard(Map.of(
                        "code", request.getGiftCardCode(),
                        "amount", order.getTotalAmount(),
                        "orderId", order.getId() != null ? order.getId() : 0
                ));
            } catch (Exception e) {
                log.warn("Gift card application call failed: {}", e.getMessage());
            }
        }
        if (Boolean.TRUE.equals(request.getUseStoreCredit()) && paymentClient != null && request.getUserId() != null) {
            try {
                BigDecimal creditToDeduct = request.getStoreCreditAmount() != null ?
                        request.getStoreCreditAmount() : order.getTotalAmount();
                paymentClient.deductStoreCredit(
                        request.getUserId().longValue(),
                        Map.of(
                                "amount", creditToDeduct,
                                "referenceId", "CHECKOUT-" + System.currentTimeMillis(),
                                "note", "Thanh toán đơn hàng"
                        )
                );
            } catch (Exception e) {
                log.warn("Store credit deduction call failed: {}", e.getMessage());
            }
        }

        if (request.getPaymentMethod() != null) {
            order.setPaymentMethod(request.getPaymentMethod());
        } else {
            order.setPaymentMethod(PaymentMethod.COD);
        }
        order.setPaymentStatus(PaymentStatus.PENDING);

        // Fraud Detection Signal Assessment
        if (fraudDetectionService != null) {
            FraudDetectionService.FraudAssessment fraudAssessment = fraudDetectionService.assessOrderRisk(order);
            if (fraudAssessment != null) {
                order.setFraudScore(fraudAssessment.getScore());
                order.setFraudRiskLevel(fraudAssessment.getRiskLevel());
                order.setFraudReason(fraudAssessment.getReason());
            }
        }

        // Step 4: Atomic DB save & Transactional Outbox write
        OrderEntity saved = saveOrderAndOutbox(order);
        sagaOrchestrator.recordOrderCreated(sagaState, saved.getId());

        return orderMapper.toDto(saved);
    }

    @Transactional
    public OrderEntity saveOrderAndOutbox(OrderEntity order) {
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

        String payloadJson = "{}";
        try {
            payloadJson = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            log.error("Failed to serialize OrderCreatedEvent to JSON", e);
        }

        OutboxEventEntity outboxEvent = OutboxEventEntity.builder()
                .aggregateType("ORDER")
                .aggregateId(saved.getId())
                .eventType("order.created")
                .exchange("tiki.events")
                .routingKey("order.created")
                .payload(payloadJson)
                .status("PENDING")
                .build();

        outboxEventRepository.save(outboxEvent);

        // Optimistic inline RabbitMQ delivery with automatic Outbox fallback
        try {
            rabbitTemplate.convertAndSend("tiki.events", "order.created", event);
            outboxEvent.setStatus("PUBLISHED");
            outboxEvent.setProcessedAt(LocalDateTime.now());
            outboxEventRepository.save(outboxEvent);
            log.info("OrderCreatedEvent dispatched immediately inline for order {}", saved.getId());
        } catch (Exception e) {
            log.warn("Inline RabbitMQ delivery failed for order {}. Event safely queued in Transactional Outbox: {}",
                    saved.getId(), e.getMessage());
        }

        return saved;
    }

    private String calculateRequestHash(CreateOrderRequest request) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            sb.append(request.getUserId()).append(":");
            if (request.getItems() != null) {
                for (CreateOrderRequest.OrderItemDto item : request.getItems()) {
                    sb.append(item.getProductId()).append("x").append(item.getQuantity()).append(";");
                }
            }
            sb.append(request.getVoucherCode()).append(":");
            sb.append(request.getUsePoints()).append(":");
            sb.append(request.getPaymentMethod());
            byte[] hash = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "HASH_" + System.currentTimeMillis();
        }
    }
}
