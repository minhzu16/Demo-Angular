package com.tiki.order.saga;

import com.tiki.order.client.UserClient;
import com.tiki.order.client.WarehouseClient;
import com.tiki.order.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CheckoutSagaOrchestrator {

    private final WarehouseClient warehouseClient;
    private final VoucherService voucherService;
    private final UserClient userClient;

    public void recordStockReservation(CheckoutSagaState state, Long productId, Integer quantity) {
        state.getReservedItems().add(new CheckoutSagaState.ReservedItem(productId, quantity));
    }

    public void recordVoucherApplication(CheckoutSagaState state, String voucherCode) {
        state.setAppliedVoucher(voucherCode);
    }

    public void recordPointsDeduction(CheckoutSagaState state, Long userId, Integer points) {
        state.setPointsUserId(userId);
        state.setDeductedPoints(points);
    }

    public void recordOrderCreated(CheckoutSagaState state, Integer orderId) {
        state.setOrderId(orderId);
    }

    public void compensate(CheckoutSagaState state, String reason) {
        log.warn("Executing Checkout Saga Compensation. Reason: {}", reason);

        // 1. Release reserved inventory
        for (CheckoutSagaState.ReservedItem item : state.getReservedItems()) {
            try {
                warehouseClient.releaseStock(item.productId(), item.quantity());
                log.info("Saga Compensation: Released {} units of stock for product {}", item.quantity(), item.productId());
            } catch (Exception e) {
                log.error("Saga Compensation Error: Failed to release stock for product {}: {}", item.productId(), e.getMessage());
            }
        }

        // 2. Release applied voucher
        if (state.getAppliedVoucher() != null && !state.getAppliedVoucher().isBlank()) {
            try {
                voucherService.releaseVoucher(state.getAppliedVoucher());
                log.info("Saga Compensation: Released voucher {}", state.getAppliedVoucher());
            } catch (Exception e) {
                log.error("Saga Compensation Error: Failed to release voucher {}: {}", state.getAppliedVoucher(), e.getMessage());
            }
        }

        // 3. Refund deducted loyalty points
        if (state.getDeductedPoints() != null && state.getDeductedPoints() > 0 && state.getPointsUserId() != null) {
            try {
                userClient.updatePoints(state.getPointsUserId(), state.getDeductedPoints());
                log.info("Saga Compensation: Refunded {} points back to user {}", state.getDeductedPoints(), state.getPointsUserId());
            } catch (Exception e) {
                log.error("Saga Compensation Error: Failed to refund points to user {}: {}", state.getPointsUserId(), e.getMessage());
            }
        }
    }
}
