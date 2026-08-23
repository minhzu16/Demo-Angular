package com.tiki.product.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ProductDetailDTO {
    private Integer id;

    @NotBlank
    @Size(max = 50)
    private String sku;

    @NotBlank
    @Size(max = 255)
    private String name;

    private String description;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal price;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal listPrice;

    @Size(max = 100)
    private String brand;

    private Integer categoryId;

    @Size(max = 255)
    private String thumbnailUrl;

    private String attributesJson;
    private List<ProductImageDTO> images;
    private Double averageRating;
    private Integer reviewCount;
    private Long sellerId;
    private Long shopId;
    private Integer stock;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getListPrice() { return listPrice; }
    public void setListPrice(BigDecimal listPrice) { this.listPrice = listPrice; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public String getAttributesJson() { return attributesJson; }
    public void setAttributesJson(String attributesJson) { this.attributesJson = attributesJson; }
    public List<ProductImageDTO> getImages() { return images; }
    public void setImages(List<ProductImageDTO> images) { this.images = images; }
    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    private String status;
}
