package com.eshop.cart.dto;

import java.math.BigDecimal;

/**
 * 商品 DTO（从 product-service Feign 获取）
 */
public class ProductDTO {

    private Long id;
    private String name;
    private String subtitle;
    private String brand;
    private String mainImage;
    private Long categoryId;
    private BigDecimal price;
    private Integer sales;
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getMainImage() { return mainImage; }
    public void setMainImage(String mainImage) { this.mainImage = mainImage; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getSales() { return sales; }
    public void setSales(Integer sales) { this.sales = sales; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
