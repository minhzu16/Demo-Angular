package com.tiki.product.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign Client for Shop Service
 */
@FeignClient(name = "shop-service", url = "${services.shop.url:http://localhost:8084}")
public interface ShopClient {

    /** Subset of the shop JSON (unknown properties are ignored). */
    record ShopInfo(Long id, String name) {}

    @GetMapping("/api/v1/shops/{shopId}")
    ShopInfo getShop(@PathVariable("shopId") Long shopId);

    /** The shop owned by a seller (shop ids are not user ids). */
    @GetMapping("/api/v1/shops/seller/{sellerId}")
    ShopInfo getShopBySeller(@PathVariable("sellerId") Long sellerId);

    /**
     * Get shop name by ID. The old target (/shops/{id}/name) does not exist, so every lookup 404'd and the
     * search index never had a shop name.
     */
    default String getShopName(Long shopId) {
        ShopInfo shop = getShop(shopId);
        return shop != null ? shop.name() : null;
    }
}
