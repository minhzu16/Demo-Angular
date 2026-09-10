package com.tiki.order.service;

import com.tiki.order.client.UserClient;
import com.tiki.order.dto.OrderDto;
import com.tiki.order.entity.OrderEntity;
import com.tiki.order.entity.OrderTrackingEntity;
import com.tiki.order.repository.OrderRepository;
import com.tiki.order.repository.OrderTrackingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderStatusService {

    private final OrderRepository orderRepository;
    private final OrderTrackingRepository orderTrackingRepository;
    private final UserClient userClient;
    private final com.tiki.order.client.WarehouseClient warehouseClient;
    private final VoucherService voucherService;
    private final OrderMapper orderMapper;
    private final com.tiki.order.client.PaymentClient paymentClient;

    public OrderDto cancelOrder(Integer orderId) {
        return updateStatus(orderId, OrderEntity.OrderStatus.CANCELLED);
    }

    public OrderDto requestReturn(Integer orderId){
        return updateStatus(orderId, OrderEntity.OrderStatus.RETURN_REQUESTED);
    }

    public OrderDto refundOrder(Integer orderId){
        return updateStatus(orderId, OrderEntity.OrderStatus.REFUNDED);
    }

    public OrderDto rejectReturn(Integer orderId, String reason) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order không tồn tại"));
        if (order.getStatus() != OrderEntity.OrderStatus.RETURN_REQUESTED) {
            throw new IllegalStateException("Đơn hàng #" + orderId + " không ở trạng thái yêu cầu trả hàng.");
        }
        order.setStatus(OrderEntity.OrderStatus.DELIVERED);
        OrderEntity saved = orderRepository.save(order);
        String note = "Yêu cầu trả hàng bị từ chối" + (reason != null && !reason.isBlank() ? ": " + reason : "");
        orderTrackingRepository.save(new OrderTrackingEntity(saved.getId(), OrderEntity.OrderStatus.DELIVERED, note));
        return orderMapper.toDto(saved);
    }

    public OrderDto updateStatus(Integer orderId, OrderEntity.OrderStatus newStatus) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order không tồn tại"));

        // ✅ BUG 16 FIX: Validate trạng thái chuyển đổi hợp lệ
        validateStatusTransition(order.getStatus(), newStatus, orderId);

        OrderEntity.OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        OrderEntity saved = orderRepository.save(order);

        // Track status update
        orderTrackingRepository.save(new OrderTrackingEntity(saved.getId(), newStatus, "Cập nhật trạng thái thành " + newStatus.name()));

        // Loyalty Points: Nếu đơn hàng được giao, cộng điểm cho khách hàng
        if (newStatus == OrderEntity.OrderStatus.DELIVERED && saved.getUserId() != null) {
            int earnedPoints = saved.getTotalAmount() != null ? saved.getTotalAmount().intValue() / 1000 : 0;
            if (earnedPoints > 0) {
                try {
                    userClient.updatePoints(saved.getUserId(), earnedPoints);
                    log.info("Granted {} loyalty points to user {} for order {}", earnedPoints, saved.getUserId(), orderId);
                } catch (Exception e) {
                    log.error("Failed to grant loyalty points to user {}", saved.getUserId(), e);
                }
            }
        }
        
        // ✅ BUG 17 FIX: Đồng bộ tồn kho khi thay đổi trạng thái đơn hàng
        if (newStatus == OrderEntity.OrderStatus.DELIVERED) {
            saved.getItems().forEach(item -> {
                try {
                    warehouseClient.confirmOrder(item.getProductId(), item.getQuantity());
                } catch (Exception e) {
                    log.error("Failed to confirm stock for product {} on delivery", item.getProductId(), e);
                }
            });
        } else if (newStatus == OrderEntity.OrderStatus.CANCELLED || newStatus == OrderEntity.OrderStatus.REFUNDED) {
            saved.getItems().forEach(item -> {
                try {
                    warehouseClient.releaseStock(item.getProductId(), item.getQuantity());
                } catch (Exception e) {
                    log.error("Failed to release stock for product {} on cancel", item.getProductId(), e);
                }
            });
            
            // Hoàn lại điểm thưởng đã dùng nếu đơn hàng bị hủy
            if (saved.getUsePoints() != null && saved.getUsePoints() > 0 && saved.getUserId() != null) {
                try {
                    userClient.updatePoints(saved.getUserId(), saved.getUsePoints());
                    log.info("Refunded {} spent loyalty points back to user {} for cancelled order {}", 
                            saved.getUsePoints(), saved.getUserId(), orderId);
                } catch (Exception e) {
                    log.error("Failed to refund spent loyalty points to user {}", saved.getUserId(), e);
                }
            }

            // Hoàn lại lượt dùng voucher nếu có
            if (saved.getVoucherCode() != null && !saved.getVoucherCode().isBlank()) {
                try {
                    voucherService.releaseVoucher(saved.getVoucherCode());
                    log.info("Released voucher {} for cancelled order {}", saved.getVoucherCode(), orderId);
                } catch (Exception e) {
                    log.error("Failed to release voucher {} for order {}", saved.getVoucherCode(), orderId, e);
                }
            }

            // Thu hồi điểm thưởng đã cộng nếu đơn bị hoàn trả sau khi đã giao thành công
            if (oldStatus == OrderEntity.OrderStatus.DELIVERED || oldStatus == OrderEntity.OrderStatus.RETURN_REQUESTED) {
                if (saved.getUserId() != null) {
                    int pointsToRevoke = saved.getTotalAmount() != null ? saved.getTotalAmount().intValue() / 1000 : 0;
                    if (pointsToRevoke > 0) {
                        try {
                            userClient.updatePoints(saved.getUserId(), -pointsToRevoke);
                            log.info("Revoked {} loyalty points from user {} due to order refund", pointsToRevoke, saved.getUserId());
                        } catch (Exception e) {
                            log.error("Failed to revoke loyalty points from user {}", saved.getUserId(), e);
                        }
                    }
                }
            }

            // Đồng bộ trạng thái hoàn tiền sang payment-service
            try {
                if (paymentClient != null) {
                    paymentClient.refundPayment(orderId);
                    log.info("Triggered payment refund for order {}", orderId);
                }
            } catch (Exception e) {
                log.error("Failed to notify payment-service to refund order {}: {}", orderId, e.getMessage());
            }
        }

        return orderMapper.toDto(saved);
    }

    /**
     * ✅ BUG 16 FIX: Validate trạng thái chuyển đổi hợp lệ
     * Ngăn chặn các thao tác vô nghĩa như cancel đơn đã giao.
     */
    private void validateStatusTransition(OrderEntity.OrderStatus current, OrderEntity.OrderStatus target, Integer orderId) {
        // Trạng thái cuối cùng (terminal states) - không cho phép thay đổi thêm
        if (current == OrderEntity.OrderStatus.CANCELLED) {
            throw new IllegalStateException("Đơn hàng #" + orderId + " đã bị hủy, không thể cập nhật.");
        }
        if (current == OrderEntity.OrderStatus.REFUNDED) {
            throw new IllegalStateException("Đơn hàng #" + orderId + " đã hoàn tiền, không thể cập nhật.");
        }

        if (current == target) {
            return; // Idempotent, cho phép
        }

        // Không cho cancel đơn đã giao hoặc đã hoàn thành
        if (target == OrderEntity.OrderStatus.CANCELLED) {
            if (current == OrderEntity.OrderStatus.DELIVERED) {
                throw new IllegalStateException(
                    "Không thể hủy đơn hàng #" + orderId + " vì đơn đã ở trạng thái " + current.name() +
                    ". Vui lòng sử dụng chức năng Trả hàng/Hoàn tiền.");
            }
        }

        log.info("Order {} status transition: {} -> {}", orderId, current, target);
    }

    public List<OrderTrackingEntity> getOrderStatusHistory(Integer orderId) {
        return orderTrackingRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
    }

    /**
     * ✅ Q4: Admin reviews fraud flagged order (APPROVE or CANCEL)
     */
    public OrderDto reviewFraudOrder(Integer orderId, String action, String reason) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderId));
        if ("APPROVE".equalsIgnoreCase(action)) {
            order.setFraudRiskLevel("LOW");
            order.setFraudReason("Đã được Admin xác nhận an toàn" + (reason != null && !reason.isBlank() ? ": " + reason : ""));
            OrderEntity saved = orderRepository.save(order);
            log.info("Order {} marked safe after fraud review", orderId);
            return orderMapper.toDto(saved);
        } else if ("REJECT_FRAUD".equalsIgnoreCase(action) || "CANCEL".equalsIgnoreCase(action)) {
            log.warn("Order {} cancelled due to fraud confirmation: {}", orderId, reason);
            order.setFraudReason("Bị hủy do vi phạm gian lận: " + (reason != null ? reason : ""));
            orderRepository.save(order);
            return updateStatus(orderId, OrderEntity.OrderStatus.CANCELLED);
        } else {
            throw new IllegalArgumentException("Hành động xem xét không hợp lệ: " + action);
        }
    }
}
