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
    private final OrderMapper orderMapper;

    public OrderDto cancelOrder(Integer orderId) {
        return updateStatus(orderId, OrderEntity.OrderStatus.CANCELLED);
    }

    public OrderDto requestReturn(Integer orderId){
        return updateStatus(orderId, OrderEntity.OrderStatus.RETURN_REQUESTED);
    }

    public OrderDto refundOrder(Integer orderId){
        return updateStatus(orderId, OrderEntity.OrderStatus.REFUNDED);
    }

    public OrderDto updateStatus(Integer orderId, OrderEntity.OrderStatus newStatus) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order không tồn tại"));

        // ✅ BUG 16 FIX: Validate trạng thái chuyển đổi hợp lệ
        validateStatusTransition(order.getStatus(), newStatus, orderId);

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
        }

        return orderMapper.toDto(saved);
    }

    /**
     * ✅ BUG 16 FIX: Validate trạng thái chuyển đổi hợp lệ
     * Ngăn chặn các thao tác vô nghĩa như cancel đơn đã giao.
     */
    private void validateStatusTransition(OrderEntity.OrderStatus current, OrderEntity.OrderStatus target, Integer orderId) {
        if (current == target) {
            return; // Idempotent, cho phép
        }

        // Trạng thái cuối cùng (terminal states) - không cho phép thay đổi thêm
        if (current == OrderEntity.OrderStatus.CANCELLED) {
            throw new IllegalStateException("Đơn hàng #" + orderId + " đã bị hủy, không thể cập nhật.");
        }
        if (current == OrderEntity.OrderStatus.REFUNDED) {
            throw new IllegalStateException("Đơn hàng #" + orderId + " đã hoàn tiền, không thể cập nhật.");
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
}
