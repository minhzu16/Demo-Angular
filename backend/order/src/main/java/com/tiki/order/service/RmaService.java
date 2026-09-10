package com.tiki.order.service;

import com.tiki.order.client.PaymentClient;
import com.tiki.order.client.UserClient;
import com.tiki.order.dto.RmaActionDto;
import com.tiki.order.dto.RmaCreateRequest;
import com.tiki.order.dto.RmaItemDto;
import com.tiki.order.dto.RmaResponseDto;
import com.tiki.order.entity.OrderEntity;
import com.tiki.order.entity.RmaEntity;
import com.tiki.order.entity.RmaItemEntity;
import com.tiki.order.repository.OrderRepository;
import com.tiki.order.repository.RmaItemRepository;
import com.tiki.order.repository.RmaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RmaService {

    public static final int MAX_ACTIVE_RMAS_PER_USER = 3;
    private static final Set<RmaEntity.RmaStatus> ACTIVE_STATUSES = Set.of(
            RmaEntity.RmaStatus.RMA_REQUESTED,
            RmaEntity.RmaStatus.RMA_APPROVED,
            RmaEntity.RmaStatus.ITEM_SHIPPED_BACK,
            RmaEntity.RmaStatus.ITEM_RECEIVED,
            RmaEntity.RmaStatus.INSPECTION_PASSED,
            RmaEntity.RmaStatus.RMA_DISPUTED
    );

    private final RmaRepository rmaRepository;
    private final RmaItemRepository rmaItemRepository;
    private final OrderRepository orderRepository;
    private final PaymentClient paymentClient;
    private final UserClient userClient;

    @Transactional
    public RmaResponseDto createRma(Long userId, RmaCreateRequest req) {
        // 1. Anti-abuse check: limit active RMAs per user
        long activeCount = rmaRepository.countByUserIdAndStatusIn(userId, ACTIVE_STATUSES);
        if (activeCount >= MAX_ACTIVE_RMAS_PER_USER) {
            throw new IllegalStateException("Bạn có quá nhiều yêu cầu đổi/trả đang xử lý (tối đa " +
                    MAX_ACTIVE_RMAS_PER_USER + "). Vui lòng đợi hoàn tất trước khi tạo thêm.");
        }

        // 2. Validate order
        OrderEntity order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng #" + req.getOrderId()));

        // 3. IDOR Check: user must own order
        if (!order.getUserId().equals(userId)) {
            throw new SecurityException("Bạn không có quyền yêu cầu trả hàng cho đơn hàng của người khác.");
        }

        // 4. Validate order status must be DELIVERED
        if (order.getStatus() != OrderEntity.OrderStatus.DELIVERED) {
            throw new IllegalStateException("Chỉ đơn hàng đã giao thành công mới được yêu cầu đổi/trả.");
        }

        // 5. Generate unique RMA number
        String rmaNumber = "RMA-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        BigDecimal refundAmount = req.getRequestedRefundAmount() != null ?
                req.getRequestedRefundAmount() : order.getTotalAmount();

        RmaEntity rma = RmaEntity.builder()
                .rmaNumber(rmaNumber)
                .orderId(order.getId())
                .userId(userId)
                .shopId(order.getShopId())
                .type(req.getType())
                .reasonCategory(req.getReasonCategory())
                .reason(req.getReason())
                .customerNote(req.getCustomerNote())
                .refundAmount(refundAmount)
                .refundMethod(req.getRefundMethod() != null ? req.getRefundMethod() : RmaEntity.RefundMethod.ORIGINAL_PAYMENT)
                .status(RmaEntity.RmaStatus.RMA_REQUESTED)
                .requestedAt(LocalDateTime.now())
                .build();

        RmaEntity savedRma = rmaRepository.save(rma);

        // 6. Save items
        List<RmaItemEntity> itemEntities = req.getItems().stream().map(i -> RmaItemEntity.builder()
                .rmaId(savedRma.getId())
                .productId(i.getProductId())
                .productName(i.getProductName())
                .quantity(i.getQuantity())
                .returnQuantity(i.getReturnQuantity())
                .unitPrice(i.getUnitPrice())
                .imageUrls(i.getImageUrls())
                .build()
        ).collect(Collectors.toList());

        List<RmaItemEntity> savedItems = rmaItemRepository.saveAll(itemEntities);
        log.info("Created RMA {} for order {}, user {}", rmaNumber, order.getId(), userId);

        return toDto(savedRma, savedItems);
    }

    @Transactional
    public RmaResponseDto sellerApproveRma(Long rmaId, Long sellerId, String note) {
        RmaEntity rma = getRmaEntity(rmaId);
        if (rma.getStatus() != RmaEntity.RmaStatus.RMA_REQUESTED) {
            throw new IllegalStateException("Yêu cầu RMA không ở trạng thái RMA_REQUESTED.");
        }

        rma.setStatus(RmaEntity.RmaStatus.RMA_APPROVED);
        rma.setSellerNote(note);
        rma.setApprovedAt(LocalDateTime.now());
        RmaEntity saved = rmaRepository.save(rma);
        log.info("Seller {} approved RMA {}", sellerId, rma.getRmaNumber());
        return toDto(saved, rmaItemRepository.findByRmaId(rmaId));
    }

    @Transactional
    public RmaResponseDto sellerRejectRma(Long rmaId, Long sellerId, String reason) {
        RmaEntity rma = getRmaEntity(rmaId);
        if (rma.getStatus() != RmaEntity.RmaStatus.RMA_REQUESTED) {
            throw new IllegalStateException("Yêu cầu RMA không ở trạng thái RMA_REQUESTED.");
        }

        rma.setStatus(RmaEntity.RmaStatus.RMA_REJECTED);
        rma.setSellerNote(reason);
        RmaEntity saved = rmaRepository.save(rma);
        log.info("Seller {} rejected RMA {} with reason: {}", sellerId, rma.getRmaNumber(), reason);
        return toDto(saved, rmaItemRepository.findByRmaId(rmaId));
    }

    @Transactional
    public RmaResponseDto updateReturnTracking(Long rmaId, Long userId, String trackingNumber, String carrier) {
        RmaEntity rma = getRmaEntity(rmaId);
        if (!rma.getUserId().equals(userId)) {
            throw new SecurityException("Bạn không có quyền thao tác trên yêu cầu RMA này.");
        }
        if (rma.getStatus() != RmaEntity.RmaStatus.RMA_APPROVED) {
            throw new IllegalStateException("Yêu cầu RMA chưa được người bán duyệt.");
        }

        rma.setReturnTrackingNumber(trackingNumber);
        rma.setReturnCarrier(carrier);
        rma.setStatus(RmaEntity.RmaStatus.ITEM_SHIPPED_BACK);
        RmaEntity saved = rmaRepository.save(rma);
        log.info("User {} updated return tracking for RMA {}: carrier={}, tracking={}",
                userId, rma.getRmaNumber(), carrier, trackingNumber);
        return toDto(saved, rmaItemRepository.findByRmaId(rmaId));
    }

    @Transactional
    public RmaResponseDto confirmItemReceived(Long rmaId, Long staffId, String note) {
        RmaEntity rma = getRmaEntity(rmaId);
        if (rma.getStatus() != RmaEntity.RmaStatus.ITEM_SHIPPED_BACK && rma.getStatus() != RmaEntity.RmaStatus.RMA_APPROVED) {
            throw new IllegalStateException("Hàng chưa được gửi lại từ khách.");
        }

        rma.setStatus(RmaEntity.RmaStatus.ITEM_RECEIVED);
        rma.setReceivedAt(LocalDateTime.now());
        rma.setAdminNote(note);
        RmaEntity saved = rmaRepository.save(rma);
        log.info("Warehouse staff {} confirmed receipt for RMA {}", staffId, rma.getRmaNumber());
        return toDto(saved, rmaItemRepository.findByRmaId(rmaId));
    }

    @Transactional
    public RmaResponseDto submitInspection(Long rmaId, Long staffId, RmaEntity.InspectionResult result, String note) {
        RmaEntity rma = getRmaEntity(rmaId);
        if (rma.getStatus() != RmaEntity.RmaStatus.ITEM_RECEIVED) {
            throw new IllegalStateException("Hàng phải được kho xác nhận đã nhận trước khi kiểm định chất lượng.");
        }

        rma.setInspectionResult(result);
        rma.setInspectionNote(note);
        if (result == RmaEntity.InspectionResult.PASSED) {
            rma.setStatus(RmaEntity.RmaStatus.INSPECTION_PASSED);
        } else {
            rma.setStatus(RmaEntity.RmaStatus.INSPECTION_FAILED);
        }

        RmaEntity saved = rmaRepository.save(rma);
        log.info("Inspection submitted for RMA {}: result={}", rma.getRmaNumber(), result);
        return toDto(saved, rmaItemRepository.findByRmaId(rmaId));
    }

    @Transactional
    public RmaResponseDto processRefund(Long rmaId) {
        RmaEntity rma = getRmaEntity(rmaId);
        if (rma.getStatus() != RmaEntity.RmaStatus.INSPECTION_PASSED && rma.getStatus() != RmaEntity.RmaStatus.RMA_APPROVED) {
            throw new IllegalStateException("Chưa thể hoàn tiền cho RMA chưa qua kiểm định.");
        }

        // 1. Call paymentClient to refund
        try {
            if (paymentClient != null) {
                paymentClient.refundPayment(rma.getOrderId());
            }
        } catch (Exception e) {
            log.error("Failed to trigger payment refund for RMA {}", rma.getRmaNumber(), e);
        }

        // 2. Revoke earned loyalty points
        try {
            if (userClient != null && rma.getRefundAmount() != null) {
                int pointsToRevoke = rma.getRefundAmount().intValue() / 1000;
                if (pointsToRevoke > 0) {
                    userClient.updatePoints(rma.getUserId(), -pointsToRevoke);
                }
            }
        } catch (Exception e) {
            log.error("Failed to revoke loyalty points for RMA {}", rma.getRmaNumber(), e);
        }

        rma.setStatus(RmaEntity.RmaStatus.COMPLETED);
        rma.setCompletedAt(LocalDateTime.now());
        RmaEntity saved = rmaRepository.save(rma);
        log.info("Refund processed and RMA {} marked COMPLETED", rma.getRmaNumber());
        return toDto(saved, rmaItemRepository.findByRmaId(rmaId));
    }

    @Transactional
    public RmaResponseDto processExchange(Long rmaId, Integer newOrderId) {
        RmaEntity rma = getRmaEntity(rmaId);
        if (rma.getStatus() != RmaEntity.RmaStatus.INSPECTION_PASSED) {
            throw new IllegalStateException("Chưa thể đổi hàng cho RMA chưa qua kiểm định.");
        }

        rma.setExchangeOrderId(newOrderId);
        rma.setStatus(RmaEntity.RmaStatus.COMPLETED);
        rma.setCompletedAt(LocalDateTime.now());
        RmaEntity saved = rmaRepository.save(rma);
        log.info("Exchange processed for RMA {} with new order {}", rma.getRmaNumber(), newOrderId);
        return toDto(saved, rmaItemRepository.findByRmaId(rmaId));
    }

    public RmaResponseDto getRmaById(Long rmaId) {
        RmaEntity rma = getRmaEntity(rmaId);
        return toDto(rma, rmaItemRepository.findByRmaId(rmaId));
    }

    public Page<RmaResponseDto> getMyRmas(Long userId, Pageable pageable) {
        return rmaRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(rma -> toDto(rma, rmaItemRepository.findByRmaId(rma.getId())));
    }

    public Page<RmaResponseDto> getShopRmas(Long shopId, Pageable pageable) {
        return rmaRepository.findByShopIdOrderByCreatedAtDesc(shopId, pageable)
                .map(rma -> toDto(rma, rmaItemRepository.findByRmaId(rma.getId())));
    }

    public Page<RmaResponseDto> getAllRmas(Pageable pageable) {
        return rmaRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(rma -> toDto(rma, rmaItemRepository.findByRmaId(rma.getId())));
    }

    private RmaEntity getRmaEntity(Long rmaId) {
        return rmaRepository.findById(rmaId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu RMA id=" + rmaId));
    }

    private RmaResponseDto toDto(RmaEntity entity, List<RmaItemEntity> items) {
        List<RmaItemDto> itemDtos = items.stream().map(i -> RmaItemDto.builder()
                .id(i.getId())
                .rmaId(i.getRmaId())
                .productId(i.getProductId())
                .productName(i.getProductName())
                .quantity(i.getQuantity())
                .returnQuantity(i.getReturnQuantity())
                .unitPrice(i.getUnitPrice())
                .imageUrls(i.getImageUrls())
                .build()
        ).collect(Collectors.toList());

        return RmaResponseDto.builder()
                .id(entity.getId())
                .rmaNumber(entity.getRmaNumber())
                .orderId(entity.getOrderId())
                .userId(entity.getUserId())
                .shopId(entity.getShopId())
                .type(entity.getType())
                .reasonCategory(entity.getReasonCategory())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .customerNote(entity.getCustomerNote())
                .sellerNote(entity.getSellerNote())
                .adminNote(entity.getAdminNote())
                .returnTrackingNumber(entity.getReturnTrackingNumber())
                .returnCarrier(entity.getReturnCarrier())
                .exchangeOrderId(entity.getExchangeOrderId())
                .refundAmount(entity.getRefundAmount())
                .refundMethod(entity.getRefundMethod())
                .inspectionResult(entity.getInspectionResult())
                .inspectionNote(entity.getInspectionNote())
                .requestedAt(entity.getRequestedAt())
                .approvedAt(entity.getApprovedAt())
                .receivedAt(entity.getReceivedAt())
                .completedAt(entity.getCompletedAt())
                .createdAt(entity.getCreatedAt())
                .items(itemDtos)
                .build();
    }
}
