package com.tiki.analytics.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Resolves which shop a seller (user id) owns — shop ids are not user ids. */
@FeignClient(name = "shop-service", url = "${SHOP_SERVICE_URL:http://shop:8086}")
public interface ShopClient {

    @GetMapping("/api/v1/shops/seller/{sellerId}")
    ShopRef getShopBySeller(@PathVariable("sellerId") Long sellerId);

    record ShopRef(Long id) {}
}
