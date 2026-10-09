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
    @DisplayName("createShopForSeller - owner comes from the caller; id/status/sellerId in the body are ignored")
    void createShopForSeller_ignoresMassAssignedFields() {
        when(shopRepository.findBySellerId(10L)).thenReturn(Optional.empty());
        when(shopRepository.save(any(ShopEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        ShopEntity body = ShopEntity.builder().id(99L).sellerId(1L).status("SUSPENDED").isActive(false)
                .name("Mine").description("d").build();

        ShopEntity saved = shopService.createShopForSeller(10L, body);

        assertThat(saved.getSellerId()).isEqualTo(10L);
        assertThat(saved.getId()).isNull();
        assertThat(saved.getStatus()).isEqualTo("ACTIVE");
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getName()).isEqualTo("Mine");
    }

    @Test
    @DisplayName("createShopForSeller - a seller can only have one shop")
    void createShopForSeller_rejectsSecondShop() {
        when(shopRepository.findBySellerId(10L)).thenReturn(Optional.of(ShopEntity.builder().id(3L).sellerId(10L).build()));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> shopService.createShopForSeller(10L, ShopEntity.builder().name("Another").build()));
    }

    @Test
    @DisplayName("updateShopProfile - owner edits profile only; cannot take over (sellerId) or reactivate")
    void updateShopProfile_ownerOnly_profileFieldsOnly() {
        ShopEntity existing = ShopEntity.builder().id(1L).sellerId(10L).name("Old").status("ACTIVE").isActive(true).build();
        when(shopRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(shopRepository.save(any(ShopEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        ShopEntity body = ShopEntity.builder().sellerId(777L).status("BANNED").isActive(false).name("New name").build();

        ShopEntity updated = shopService.updateShopProfile(1L, 10L, false, body);

        assertThat(updated.getName()).isEqualTo("New name");
        assertThat(updated.getSellerId()).isEqualTo(10L);
        assertThat(updated.getStatus()).isEqualTo("ACTIVE");
        assertThat(updated.isActive()).isTrue();
    }

    @Test
    @DisplayName("updateShopProfile - another user is rejected, admin allowed, missing shop -> null")
    void updateShopProfile_authorization() {
        ShopEntity existing = ShopEntity.builder().id(1L).sellerId(10L).name("Old").build();
        when(shopRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(shopRepository.findById(2L)).thenReturn(Optional.empty());
        when(shopRepository.save(any(ShopEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        org.junit.jupiter.api.Assertions.assertThrows(SecurityException.class,
                () -> shopService.updateShopProfile(1L, 11L, false, ShopEntity.builder().name("Hijack").build()));
        assertThat(existing.getName()).isEqualTo("Old");
        assertThat(shopService.updateShopProfile(1L, 1L, true, ShopEntity.builder().name("Admin edit").build()).getName())
                .isEqualTo("Admin edit");
        assertThat(shopService.updateShopProfile(2L, 10L, false, ShopEntity.builder().build())).isNull();
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
