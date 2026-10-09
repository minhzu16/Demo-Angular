package com.tiki.order.service;

import com.tiki.order.client.ShopClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Answers "does this seller user own this shop?" — fails closed when shop-service cannot answer. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShopOwnershipService {

    private final ShopClient shopClient;

    public boolean ownsShop(Long sellerUserId, Long shopId) {
        if (sellerUserId == null || shopId == null) {
            return false;
        }
        try {
            ShopClient.ShopRef shop = shopClient.getShopBySeller(sellerUserId);
            return shop != null && shopId.equals(shop.id());
        } catch (Exception e) {
            log.warn("Could not verify shop ownership for seller {} / shop {}: {}", sellerUserId, shopId, e.getMessage());
            return false;
        }
    }
}
