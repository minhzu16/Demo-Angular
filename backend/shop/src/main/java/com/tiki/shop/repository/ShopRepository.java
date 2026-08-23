package com.tiki.shop.repository;

import com.tiki.shop.entity.ShopEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<ShopEntity, Long> {
    Optional<ShopEntity> findBySellerId(Long sellerId);
    Optional<ShopEntity> findByName(String name);
}
