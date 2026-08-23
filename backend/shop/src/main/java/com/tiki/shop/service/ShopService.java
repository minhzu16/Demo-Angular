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

    @Transactional
    public void deactivateShop(Long id) {
        shopRepository.findById(id).ifPresent(s -> {
            s.setActive(false);
            shopRepository.save(s);
        });
    }
}
