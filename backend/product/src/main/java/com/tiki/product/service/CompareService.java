package com.tiki.product.service;

import com.tiki.product.dto.CompareDto;
import com.tiki.product.entity.CompareEntity;
import com.tiki.product.entity.ProductEntity;
import com.tiki.product.repository.CompareRepository;
import com.tiki.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompareService {

    public static final int MAX_COMPARE_ITEMS = 4;

    private final CompareRepository compareRepo;
    private final ProductRepository productRepo;

    @Transactional(readOnly = true)
    public List<CompareDto> getCompareList(Long userId) {
        List<CompareEntity> list = compareRepo.findByUserIdOrderByCreatedAtDesc(userId);
        if (list.isEmpty()) {
            return List.of();
        }

        List<Integer> productIds = list.stream().map(CompareEntity::getProductId).collect(Collectors.toList());
        Map<Integer, ProductEntity> productMap = productRepo.findAllById(productIds).stream()
                .collect(Collectors.toMap(ProductEntity::getId, p -> p));

        return list.stream().map(c -> {
            ProductEntity p = productMap.get(c.getProductId());
            return CompareDto.builder()
                    .id(c.getId())
                    .userId(c.getUserId())
                    .productId(c.getProductId())
                    .productName(p != null ? p.getName() : null)
                    .price(p != null ? p.getPrice() : null)
                    .thumbnailUrl(p != null ? p.getThumbnailUrl() : null)
                    .brand(p != null ? p.getBrand() : null)
                    .createdAt(c.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public CompareDto addToCompare(Long userId, Integer productId) {
        ProductEntity product = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại: " + productId));

        Optional<CompareEntity> existing = compareRepo.findByUserIdAndProductId(userId, productId);
        if (existing.isPresent()) {
            CompareEntity c = existing.get();
            return CompareDto.builder()
                    .id(c.getId())
                    .userId(c.getUserId())
                    .productId(c.getProductId())
                    .productName(product.getName())
                    .price(product.getPrice())
                    .thumbnailUrl(product.getThumbnailUrl())
                    .brand(product.getBrand())
                    .createdAt(c.getCreatedAt())
                    .build();
        }

        long count = compareRepo.countByUserId(userId);
        if (count >= MAX_COMPARE_ITEMS) {
            throw new IllegalStateException("Chỉ có thể so sánh tối đa " + MAX_COMPARE_ITEMS + " sản phẩm cùng lúc");
        }

        CompareEntity entity = CompareEntity.builder()
                .userId(userId)
                .productId(productId)
                .createdAt(LocalDateTime.now())
                .build();
        entity = compareRepo.save(entity);

        log.info("User {} added product {} to compare list", userId, productId);

        return CompareDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .productId(entity.getProductId())
                .productName(product.getName())
                .price(product.getPrice())
                .thumbnailUrl(product.getThumbnailUrl())
                .brand(product.getBrand())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    @Transactional
    public void removeFromCompare(Long userId, Integer productId) {
        compareRepo.deleteByUserIdAndProductId(userId, productId);
        log.info("User {} removed product {} from compare list", userId, productId);
    }

    @Transactional
    public void clearCompare(Long userId) {
        compareRepo.deleteByUserId(userId);
        log.info("User {} cleared compare list", userId);
    }
}
