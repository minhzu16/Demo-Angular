package com.tiki.shop.service;

import com.tiki.shop.entity.ShopEntity;
import com.tiki.shop.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopService {
    private final ShopRepository shopRepository;

    public ShopEntity getShopById(Long id) {
        return shopRepository.findById(id).orElse(null);
    }

    public ShopEntity getShopBySellerId(Long sellerId) {
        return shopRepository.findBySellerId(sellerId).orElse(null);
    }

    @Transactional
    public ShopEntity createOrUpdateShop(ShopEntity shop) {
        log.info("Saving shop: {}, for seller: {}", shop.getName(), shop.getSellerId());
        return shopRepository.save(shop);
    }

    /**
     * Seller self-service creation: ownership comes from the caller, never from the request body, and only
     * profile fields are copied (id / status / isActive / sellerId in the body used to be saved verbatim).
     */
    @Transactional
    public ShopEntity createShopForSeller(Long sellerId, ShopEntity input) {
        if (shopRepository.findBySellerId(sellerId).isPresent()) {
            throw new IllegalStateException("Bạn đã có cửa hàng");
        }
        ShopEntity shop = ShopEntity.builder().sellerId(sellerId).isActive(true).build();
        copyProfileFields(input, shop);
        return shopRepository.save(shop);
    }

    /** Updates profile fields of a shop the caller owns (or any shop for an admin); null if the shop does not exist. */
    @Transactional
    public ShopEntity updateShopProfile(Long shopId, Long callerId, boolean admin, ShopEntity input) {
        ShopEntity existing = shopRepository.findById(shopId).orElse(null);
        if (existing == null) {
            return null;
        }
        if (!admin && (callerId == null || !callerId.equals(existing.getSellerId()))) {
            throw new SecurityException("Bạn không phải chủ cửa hàng này");
        }
        copyProfileFields(input, existing);
        return shopRepository.save(existing);
    }

    private static void copyProfileFields(ShopEntity from, ShopEntity to) {
        if (from.getName() != null) to.setName(from.getName());
        if (from.getDescription() != null) to.setDescription(from.getDescription());
        if (from.getLogoUrl() != null) to.setLogoUrl(from.getLogoUrl());
        if (from.getBannerUrl() != null) to.setBannerUrl(from.getBannerUrl());
        if (from.getAddress() != null) to.setAddress(from.getAddress());
    }

    @Transactional
    public void deactivateShop(Long id) {
        shopRepository.findById(id).ifPresent(s -> {
            s.setActive(false);
            shopRepository.save(s);
        });
    }
}
