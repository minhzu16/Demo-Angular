package com.tiki.shop.controller;

import com.tiki.shop.entity.ShopEntity;
import com.tiki.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shops")
@RequiredArgsConstructor
@Slf4j
public class ShopController {
    private final ShopService shopService;

    @GetMapping("/my-shop")
    public ResponseEntity<ShopEntity> getMyShop(@RequestHeader(value = "X-User-Id", required = false) Long sellerId) {
        log.info("Fetching my shop details for sellerId: {}", sellerId);
        ShopEntity shop = shopService.getShopBySellerId(sellerId);
        return shop != null ? ResponseEntity.ok(shop) : ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShopEntity> getById(@PathVariable Long id) {
        log.info("Fetching shop details for id: {}", id);
        ShopEntity shop = shopService.getShopById(id);
        return shop != null ? ResponseEntity.ok(shop) : ResponseEntity.notFound().build();
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<ShopEntity> getBySellerId(@PathVariable Long sellerId) {
        ShopEntity shop = shopService.getShopBySellerId(sellerId);
        return shop != null ? ResponseEntity.ok(shop) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<ShopEntity> create(@RequestBody ShopEntity shop, @RequestHeader(value = "X-User-Id", required = false) Long sellerId) {
        if (sellerId != null && shop.getSellerId() == null) shop.setSellerId(sellerId);
        log.info("Creating a new shop with name: {}", shop.getName());
        return ResponseEntity.ok(shopService.createOrUpdateShop(shop));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShopEntity> update(@PathVariable Long id, @RequestBody ShopEntity shop) {
        shop.setId(id);
        return ResponseEntity.ok(shopService.createOrUpdateShop(shop));
    }
}
