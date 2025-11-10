package com.tiki.cart.controller;

import com.tiki.cart.dto.*;
import com.tiki.cart.service.CartService;
import com.tiki.cart.client.ProductClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Cart Controller - Manages shopping cart operations
 * Optimized with Lombok for cleaner code
 */
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;
    private final ProductClient productClient;

    /** Generate session ID for guest user */
    @PostMapping("/session")
    public SessionResponse createSession(){
        String sessionId = UUID.randomUUID().toString();
        log.debug("Created new session: {}", sessionId);
        return new SessionResponse(sessionId);
    }

    /** Get or create cart */
    @GetMapping
    public CartDto getCart(@RequestParam(required = false) Integer userId,
                           @RequestParam(required = false) String sessionId){
        log.debug("Getting cart for userId: {}, sessionId: {}", userId, sessionId);
        return cartService.getCart(userId, sessionId);
    }

    /** Get cart by user ID */
    @GetMapping("/user/{userId}")
    public ResponseEntity<CartDto> getCartByUserId(@PathVariable Integer userId){
        return ResponseEntity.ok(cartService.getCart(userId, null));
    }

    /** Get cart items count by user ID */
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Integer> getCartCountByUserId(@PathVariable Integer userId){
        return ResponseEntity.ok(cartService.getCart(userId, null).getTotalItems());
    }

    /** Add item to cart */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CartDto addItemSimple(
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
            @RequestBody AddItemRequest req){
        Integer userId = userIdHeader != null ? userIdHeader.intValue() : req.getUserId();
        Integer productId = req.getProductId();
        Integer quantity = req.getQuantity() != null ? req.getQuantity() : 1;
        
        // ✅ SECURITY FIX: Fetch giá từ ProductService
        Double price = fetchProductPrice(productId);
        
        log.info("Adding item to cart - userId: {}, productId: {}, qty: {}, price: {}", userId, productId, quantity, price);
        return cartService.addItem(userId, null, productId, quantity, price);
    }

    /** Add item to cart (legacy) */
    @PostMapping("/items")
    public ResponseEntity<CartDto> addItem(@RequestParam(required = false) Integer userId,
                                           @RequestParam(required = false) String sessionId,
                                           @Valid @RequestBody AddItemRequest req){
        // ✅ SECURITY FIX: Fetch giá từ ProductService
        Double price = fetchProductPrice(req.getProductId());
        log.info("Legacy endpoint: Fetched price {} for product {}", price, req.getProductId());
        return ResponseEntity.ok(cartService.addItem(userId,sessionId,req.getProductId(),req.getQuantity(),price));
    }

    /** Update quantity */
    @PutMapping("/items/{productId}")
    public ResponseEntity<CartDto> updateQty(@RequestParam(required = false) Integer userId,
                                             @RequestParam(required = false) String sessionId,
                                             @PathVariable Integer productId,
                                             @Valid @RequestBody UpdateQtyRequest req){
        return ResponseEntity.ok(cartService.updateQty(userId,sessionId,productId,req.getQuantity()));
    }

    /** Remove item */
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartDto> remove(@RequestParam(required = false) Integer userId,
                                          @RequestParam(required = false) String sessionId,
                                          @PathVariable Integer productId){
        return ResponseEntity.ok(cartService.removeItem(userId,sessionId,productId));
    }

    /** Count items */
    @GetMapping("/count")
    public ResponseEntity<Integer> count(@RequestParam(required = false) Integer userId,
                                         @RequestParam(required = false) String sessionId){
        return ResponseEntity.ok(cartService.getCart(userId,sessionId).getTotalItems());
    }

    /** Total amount */
    @GetMapping("/total")
    public ResponseEntity<Double> total(@RequestParam(required = false) Integer userId,
                                        @RequestParam(required = false) String sessionId){
        return ResponseEntity.ok(cartService.getCart(userId,sessionId).getTotalAmount());
    }

    /** Merge guest cart to user cart */
    @PostMapping("/merge")
    public ResponseEntity<CartDto> merge(@RequestParam Integer userId, @RequestParam String sessionId){
        return ResponseEntity.ok(cartService.merge(userId,sessionId));
    }
    
    /** Get cart summary */
    @GetMapping("/summary")
    public ResponseEntity<java.util.Map<String, Object>> getSummary(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(required = false) String sessionId) {
        CartDto cart = cartService.getCart(userId != null ? userId.intValue() : null, sessionId);
        java.util.Map<String, Object> summary = java.util.Map.of(
            "totalItems", cart.getTotalItems(),
            "totalAmount", cart.getTotalAmount(),
            "itemCount", cart.getTotalItems()
        );
        return ResponseEntity.ok(summary);
    }
    
    /** Validate cart */
    @GetMapping("/validate")
    public ResponseEntity<java.util.Map<String, Object>> validateCart(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(required = false) String sessionId) {
        CartDto cart = cartService.getCart(userId != null ? userId.intValue() : null, sessionId);
        java.util.Map<String, Object> validation = java.util.Map.of(
            "valid", true,
            "totalItems", cart.getTotalItems(),
            "errors", java.util.List.of()
        );
        return ResponseEntity.ok(validation);
    }
    
    /** Clear cart */
    @DeleteMapping("/clear")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> clearCart(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(required = false) String sessionId) {
        Integer userIdInt = userId != null ? userId.intValue() : null;
        log.info("Clearing cart for userId: {}, sessionId: {}", userIdInt, sessionId);
        cartService.clearCart(userIdInt, sessionId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Helper method: Fetch giá sản phẩm từ Product Service
     * ✅ Fix security issue - không trust giá từ client
     */
    private Double fetchProductPrice(Integer productId) {
        try {
            ProductClient.ProductDTO product = productClient.getProduct(productId);
            if (product != null && product.getPrice() != null) {
                Double price = product.getPrice().doubleValue();
                log.debug("Fetched price {} for product {}", price, productId);
                return price;
            }
            log.warn("Product {} not found or has no price, using 0.0", productId);
            return 0.0;
        } catch (Exception e) {
            log.error("Failed to fetch price for product {}: {}", productId, e.getMessage());
            // Fallback to 0.0 if Product Service is down
            return 0.0;
        }
    }
}
