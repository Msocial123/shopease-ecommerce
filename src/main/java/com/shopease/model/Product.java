package com.shopease.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class Product {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String categorySlug;
    private String sku;
    private String name;
    private String brand;
    private String description;
    private BigDecimal price;
    private BigDecimal mrp;
    private int stock;
    private String imageUrl;
    private BigDecimal rating = BigDecimal.ZERO;
    private boolean featured;
    private boolean active = true;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getCategorySlug() { return categorySlug; }
    public void setCategorySlug(String categorySlug) { this.categorySlug = categorySlug; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getMrp() { return mrp; }
    public void setMrp(BigDecimal mrp) { this.mrp = mrp; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isInStock() { return stock > 0; }

    public boolean isLowStock() { return stock > 0 && stock <= 5; }

    public boolean isHasImage() { return imageUrl != null && !imageUrl.isBlank(); }

    /** Percentage off MRP, e.g. 25 for 25% off. */
    public int getDiscountPercent() {
        if (mrp == null || price == null || mrp.signum() <= 0 || price.compareTo(mrp) >= 0) return 0;
        return mrp.subtract(price).multiply(BigDecimal.valueOf(100))
                .divide(mrp, 0, RoundingMode.HALF_UP).intValue();
    }
}
