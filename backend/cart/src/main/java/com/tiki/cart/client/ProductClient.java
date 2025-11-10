package com.tiki.cart.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

/**
 * Feign Client để gọi Product Service
 * Lấy thông tin sản phẩm và giá
 */
@FeignClient(name = "product-service", url = "${product.service.url:http://localhost:8084}")
public interface ProductClient {
    
    /**
     * Lấy thông tin sản phẩm
     */
    @GetMapping("/api/v1/products/{id}")
    ProductDTO getProduct(@PathVariable("id") Integer id);
    
    /**
     * DTO cho Product response
     */
    class ProductDTO {
        private Integer id;
        private String name;
        private BigDecimal price;
        private String thumbnailUrl;
        private Boolean isActive;
        
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        
        public String getThumbnailUrl() { return thumbnailUrl; }
        public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
        
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    }
}
