package com.tiki.review.service;

import com.tiki.review.client.ProductClient;
import com.tiki.review.client.ShopClient;
import com.tiki.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Who may reply to a review in the name of a shop: an admin, or the seller who owns the shop that sells the
 * reviewed product. Fails closed when product-service / shop-service cannot answer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewAccessService {

    private final ReviewRepository reviewRepository;
    private final ProductClient productClient;
    private final ShopClient shopClient;

    public boolean canReply(Long reviewId, Long userId, String role) {
        if (role != null && role.toUpperCase().contains("ADMIN")) {
            return true;
        }
        if (userId == null || role == null || !role.toUpperCase().contains("SELLER")) {
            return false;
        }
        try {
            Long productId = reviewRepository.findById(reviewId).map(r -> r.getProductId()).orElse(null);
            if (productId == null) {
                return false;
            }
            ProductClient.ProductRef product = productClient.getProduct(productId);
            ShopClient.ShopRef shop = shopClient.getShopBySeller(userId);
            return product != null && shop != null && product.shopId() != null && product.shopId().equals(shop.id());
        } catch (Exception e) {
            log.warn("Could not verify reply authorization for review {} / user {}: {}", reviewId, userId, e.getMessage());
            return false;
        }
    }
}
