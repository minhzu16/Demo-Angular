package com.tiki.order.saga;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class CheckoutSagaState {

    public record ReservedItem(Long productId, Integer quantity) {}

    private final List<ReservedItem> reservedItems = new ArrayList<>();
    private String appliedVoucher;
    private Integer deductedPoints;
    private Long pointsUserId;
    private Integer orderId;
}
