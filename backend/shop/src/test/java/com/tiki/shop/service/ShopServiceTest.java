package com.tiki.shop.service;

import com.tiki.shop.entity.ShopEntity;
import com.tiki.shop.repository.ShopRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShopServiceTest {

    @Mock
    private ShopRepository shopRepository;

    @InjectMocks
    private ShopService shopService;

    @Test
    @DisplayName("getShopById - returns shop if found")
    void getShopById_ReturnsShop() {
        ShopEntity shop = ShopEntity.builder().id(1L).name("My Shop").build();
        when(shopRepository.findById(1L)).thenReturn(Optional.of(shop));

        ShopEntity result = shopService.getShopById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("My Shop");
    }

    @Test
    @DisplayName("getShopBySellerId - returns shop if found")
    void getShopBySellerId_ReturnsShop() {
        ShopEntity shop = ShopEntity.builder().id(2L).sellerId(10L).name("Seller Shop").build();
        when(shopRepository.findBySellerId(10L)).thenReturn(Optional.of(shop));

        ShopEntity result = shopService.getShopBySellerId(10L);

        assertThat(result).isNotNull();
        assertThat(result.getSellerId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("createOrUpdateShop - saves and returns shop")
    void createOrUpdateShop_SavesShop() {
        ShopEntity shop = ShopEntity.builder().sellerId(10L).name("New Shop").build();
        when(shopRepository.save(any(ShopEntity.class))).thenReturn(shop);

        ShopEntity result = shopService.createOrUpdateShop(shop);

        assertThat(result).isNotNull();
        verify(shopRepository).save(shop);
    }

    @Test
    @DisplayName("deactivateShop - sets isActive to false and saves")
    void deactivateShop_DeactivatesShop() {
        ShopEntity shop = ShopEntity.builder().id(1L).isActive(true).build();
        when(shopRepository.findById(1L)).thenReturn(Optional.of(shop));

        shopService.deactivateShop(1L);

        assertThat(shop.isActive()).isFalse();
        verify(shopRepository).save(shop);
    }
}
