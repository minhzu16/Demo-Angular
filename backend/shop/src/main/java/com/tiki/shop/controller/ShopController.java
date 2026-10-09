package com.tiki.shop.controller;

import com.tiki.shop.entity.ShopEntity;
import com.tiki.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
        if (sellerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        log.info("Creating a new shop with name: {} for seller {}", shop.getName(), sellerId);
        try {
            return ResponseEntity.ok(shopService.createShopForSeller(sellerId, shop));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShopEntity> update(
            @PathVariable Long id,
            @RequestBody ShopEntity shop,
            @RequestHeader(value = "X-User-Id", required = false) Long callerId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (callerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        boolean admin = role != null && role.toUpperCase().contains("ADMIN");
        try {
            ShopEntity updated = shopService.updateShopProfile(id, callerId, admin, shop);
            return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }
}
