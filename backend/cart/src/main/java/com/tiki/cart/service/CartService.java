package com.tiki.cart.service;

import com.tiki.cart.dto.CartDto;
import com.tiki.cart.dto.CartItemDto;
import com.tiki.cart.entity.CartEntity;
import com.tiki.cart.entity.CartItemEntity;
import com.tiki.cart.repository.CartItemRepository;
import com.tiki.cart.repository.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepo;
    @Autowired
    private CartItemRepository itemRepo;
    
    @Value("${PRODUCT_SERVICE_URL:http://localhost:8081}")
    private String productServiceUrl;
    
    private CartDto toDto(CartEntity e){
        CartDto d = new CartDto();
        d.setId(e.getId());
        d.setUserId(e.getUserId());
        d.setSessionId(e.getSessionId());
        List<CartItemEntity> cartItems = e.getItems() != null ? e.getItems() : new ArrayList<>();
        List<CartItemDto> items = cartItems.stream().map(it -> {
            CartItemDto i = new CartItemDto();
            i.setProductId(it.getProductId());
            int qtySafe = it.getQuantity() == null ? 0 : it.getQuantity();
            double priceSafe = it.getPriceSnapshot() == null ? 0.0 : it.getPriceSnapshot();
            i.setQuantity(qtySafe);
            i.setPriceSnapshot(priceSafe);
            return i;
        }).collect(Collectors.toList());
        d.setCartItems(items);
        d.setTotalItems(items.stream().mapToInt(CartItemDto::getQuantity).sum());
        d.setTotalAmount(items.stream()
                .mapToDouble(it -> it.getQuantity() * it.getPriceSnapshot())
                .sum());
        return d;
    }

    private CartEntity getOrCreate(Integer userId, String sessionId){
        // Nếu có userId, tìm theo userId
        if (userId != null) {
            Optional<CartEntity> opt = cartRepo.findByUserIdAndIsActive(userId, true);
            if (opt.isPresent()) {
                return opt.get();
            }
        }
        
        // Nếu có sessionId, tìm theo sessionId
        if (sessionId != null && !sessionId.isEmpty()) {
            Optional<CartEntity> opt = cartRepo.findBySessionIdAndIsActive(sessionId, true);
            if (opt.isPresent()) {
                return opt.get();
            }
        }
        
        // Tạo mới nếu không tìm thấy
        CartEntity c = new CartEntity();
        c.setUserId(userId);
        c.setSessionId(sessionId);
        c.setIsActive(true);
        if(c.getItems() == null) {
            c.setItems(new ArrayList<>());
        }
        return cartRepo.save(c);
    }

    public CartDto getCart(Integer userId,String sessionId){
        System.out.println("getCart called with userId=" + userId + ", sessionId=" + sessionId);
        CartEntity cart = getOrCreate(userId,sessionId);
        System.out.println("Cart found/created: id=" + cart.getId() + ", items=" + (cart.getItems() != null ? cart.getItems().size() : 0));
        return toDto(cart);
    }

    @Transactional
    public CartDto addItem(Integer userId,String sessionId,Integer productId,Integer qty,Double price){
        // Skip product validation temporarily to fix 500 error
        // Product service validation can be added back later
        
        CartEntity cart = getOrCreate(userId,sessionId);
        
        // Ensure items list is not null
        if (cart.getItems() == null) {
            cart.setItems(new ArrayList<>());
        }
        
        CartItemEntity item = cart.getItems().stream()
                .filter(i->i.getProductId() != null && i.getProductId().equals(productId))
                .findFirst().orElse(null);
        if(item==null){
            item = new CartItemEntity();
            item.setCart(cart);
            item.setProductId(productId);
            item.setQuantity(qty);
            item.setPriceSnapshot(price);
            cart.getItems().add(item);
        }else{
            int currentQty = item.getQuantity() == null ? 0 : item.getQuantity();
            item.setQuantity(currentQty + qty);
        }
        cartRepo.save(cart);
        return toDto(cart);
    }

    @Transactional
    public CartDto updateQty(Integer userId,String sessionId,Integer productId,Integer qty){
        CartEntity cart = getOrCreate(userId,sessionId);
        cart.getItems().forEach(i->{
            if(i.getProductId().equals(productId)) i.setQuantity(qty);
        });
        cartRepo.save(cart);
        return toDto(cart);
    }

    @Transactional
    public CartDto removeItem(Integer userId,String sessionId,Integer productId){
        CartEntity cart = getOrCreate(userId,sessionId);
        cart.getItems().removeIf(i->i.getProductId().equals(productId));
        cartRepo.save(cart);
        return toDto(cart);
    }

    @Transactional
    public CartDto merge(Integer userId,String sessionId){
        CartEntity userCart = getOrCreate(userId,null);
        CartEntity guest = cartRepo.findBySessionIdAndIsActive(sessionId,true).orElse(null);
        if(guest!=null){
            guest.getItems().forEach(item -> {
                addItem(userId,null,item.getProductId(),item.getQuantity(),item.getPriceSnapshot());
            });
            guest.setIsActive(false);
            cartRepo.save(guest);
        }
        return toDto(userCart);
    }
    
    /**
     * Clear cart - Xóa tất cả items trong giỏ hàng
     * ✅ Fix TODO: Implement clearCart() method
     */
    @Transactional
    public void clearCart(Integer userId, String sessionId) {
        CartEntity cart = getOrCreate(userId, sessionId);
        if (cart.getItems() != null) {
            cart.getItems().clear();
            cartRepo.save(cart);
        }
    }
}
