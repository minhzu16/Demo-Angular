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
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
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

    /**
     * The caller's identity comes ONLY from the gateway-validated X-User-Id header.
     * A userId in the query string or body used to be honoured when the header was absent, which let an
     * anonymous caller read or modify any user's cart. Guests are identified by their sessionId instead.
     */
    private static Integer callerId(Long userIdHeader) {
        return userIdHeader != null ? userIdHeader.intValue() : null;
    }

    private static void requireSelf(Long userIdHeader, Integer pathUserId) {
        if (userIdHeader == null || !userIdHeader.equals(pathUserId.longValue())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn chỉ có thể xem giỏ hàng của chính mình");
        }
    }

    /** Get or create cart */
    @GetMapping
    public CartDto getCart(@RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
                           @RequestParam(required = false) Integer userId,
                           @RequestParam(required = false) String sessionId){
        Integer finalUserId = callerId(userIdHeader);
        log.debug("Getting cart for userId: {}, sessionId: {}", finalUserId, sessionId);
        return cartService.getCart(finalUserId, sessionId);
    }

    /** Get cart by user ID (own cart only) */
    @GetMapping("/user/{userId}")
    public ResponseEntity<CartDto> getCartByUserId(@RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
                                                   @PathVariable Integer userId){
        requireSelf(userIdHeader, userId);
        return ResponseEntity.ok(cartService.getCart(userId, null));
    }

    /** Get cart items count by user ID (own cart only) */
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Integer> getCartCountByUserId(@RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
                                                        @PathVariable Integer userId){
        requireSelf(userIdHeader, userId);
        return ResponseEntity.ok(cartService.getCart(userId, null).getTotalItems());
    }

    /** Add item to cart */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CartDto addItemSimple(
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
            @Valid @RequestBody AddItemRequest req){
        Integer userId = callerId(userIdHeader);
        Integer productId = req.getProductId();
        Integer quantity = req.getQuantity() != null ? req.getQuantity() : 1;
        
        // ✅ SECURITY FIX: Fetch giá từ ProductService
        BigDecimal price = fetchProductPrice(productId);
        
        log.info("Adding item to cart - userId: {}, productId: {}, qty: {}, price: {}", userId, productId, quantity, price);
        return cartService.addItem(userId, null, productId, quantity, price);
    }

    /** Add item to cart (legacy) */
    @PostMapping("/items")
    public ResponseEntity<CartDto> addItem(@RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
                                           @RequestParam(required = false) Integer userId,
                                           @RequestParam(required = false) String sessionId,
                                           @Valid @RequestBody AddItemRequest req){
        Integer finalUserId = callerId(userIdHeader);
        // ✅ SECURITY FIX: Fetch giá từ ProductService
        BigDecimal price = fetchProductPrice(req.getProductId());
        log.info("Legacy endpoint: Fetched price {} for product {}", price, req.getProductId());
        return ResponseEntity.ok(cartService.addItem(finalUserId,sessionId,req.getProductId(),req.getQuantity(),price));
    }

    /** Update quantity */
    @PutMapping("/items/{productId}")
    public ResponseEntity<CartDto> updateQty(@RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
                                             @RequestParam(required = false) Integer userId,
                                             @RequestParam(required = false) String sessionId,
                                             @PathVariable Integer productId,
                                             @Valid @RequestBody UpdateQtyRequest req){
        Integer finalUserId = callerId(userIdHeader);
        return ResponseEntity.ok(cartService.updateQty(finalUserId,sessionId,productId,req.getQuantity()));
    }

    /** Remove item */
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartDto> remove(@RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
                                          @RequestParam(required = false) Integer userId,
                                          @RequestParam(required = false) String sessionId,
                                          @PathVariable Integer productId){
        Integer finalUserId = callerId(userIdHeader);
        return ResponseEntity.ok(cartService.removeItem(finalUserId,sessionId,productId));
    }

    /** Count items */
    @GetMapping("/count")
    public ResponseEntity<Integer> count(@RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
                                         @RequestParam(required = false) Integer userId,
                                         @RequestParam(required = false) String sessionId){
        Integer finalUserId = callerId(userIdHeader);
        return ResponseEntity.ok(cartService.getCart(finalUserId,sessionId).getTotalItems());
    }

    /** Total amount */
    @GetMapping("/total")
    public ResponseEntity<BigDecimal> total(@RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
                                            @RequestParam(required = false) Integer userId,
                                            @RequestParam(required = false) String sessionId){
        Integer finalUserId = callerId(userIdHeader);
        return ResponseEntity.ok(cartService.getCart(finalUserId,sessionId).getTotalAmount());
    }

    /** Merge the guest cart (by sessionId) into the signed-in caller's cart. */
    @PostMapping("/merge")
    public ResponseEntity<CartDto> merge(@RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
                                         @RequestParam(required = false) Integer userId,
                                         @RequestParam String sessionId){
        if (userIdHeader == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cần đăng nhập để gộp giỏ hàng");
        }
        return ResponseEntity.ok(cartService.merge(userIdHeader.intValue(), sessionId));
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
    private BigDecimal fetchProductPrice(Integer productId) {
        try {
            ProductClient.ProductDTO product = productClient.getProduct(productId);
            if (product != null && product.getPrice() != null && product.getPrice().signum() > 0) {
                BigDecimal price = product.getPrice();
                log.debug("Fetched price {} for product {}", price, productId);
                return price;
            }
        } catch (Exception e) {
            log.error("Failed to fetch price for product {}: {}", productId, e.getMessage());
        }
        // Never store a zero price: it showed "0 ₫" items in the cart and hid that the product service
        // was down (the Feign fallback also returns price 0 for unknown products).
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Không thể xác minh giá sản phẩm lúc này. Vui lòng thử lại sau.");
    }
}
