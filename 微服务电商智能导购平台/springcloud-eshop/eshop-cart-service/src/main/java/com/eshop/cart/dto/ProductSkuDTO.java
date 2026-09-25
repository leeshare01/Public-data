package com.eshop.cart.dto;

import java.math.BigDecimal;

/**
 * SKU DTO（从 product-service Feign 获取）
 */
public class ProductSkuDTO {

    private Long id;
    private Long productId;
    private String skuCode;
    private String specValues;     // JSON: {"颜色":"红色","尺寸":"M"}
    private BigDecimal price;
    private Integer stock;
    private String image;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getSkuCode() { return skuCode; }
    public void setSkuCode(String skuCode) { this.skuCode = skuCode; }
    public String getSpecValues() { return specValues; }
    public void setSpecValues(String specValues) { this.specValues = specValues; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
}
