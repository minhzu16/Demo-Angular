package com.tiki.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Resolves which shop a seller (user id) owns. Shop ids are NOT user ids
 * (e.g. shop 1 belongs to seller user 2), so ownership must be looked up in shop-service.
 */
@FeignClient(name = "shop-service", url = "${SHOP_SERVICE_URL:http://localhost:8086}")
public interface ShopClient {

    @GetMapping("/api/v1/shops/seller/{sellerId}")
    ShopRef getShopBySeller(@PathVariable("sellerId") Long sellerId);

    /** Only the field we need; unknown JSON properties are ignored. */
    record ShopRef(Long id) {}
}
