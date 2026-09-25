package com.eshop.cart.dto;

import java.math.BigDecimal;

/**
 * 购物车条目 DTO（返回给前端）
 */
public class CartItemDTO {

    private Long skuId;          // 兼容前端：传 productId
    private Long productId;
    private String productName;
    private String specInfo;
    private String image;
    private BigDecimal price;
    private Integer quantity;
    private Boolean selected;
    private Integer stock;

    public static CartItemDTO of(Long productId, String productName, String image,
                                  BigDecimal price, Integer quantity, Boolean selected) {
        CartItemDTO dto = new CartItemDTO();
        dto.setSkuId(productId);
        dto.setProductId(productId);
        dto.setProductName(productName);
        dto.setSpecInfo("");
        dto.setImage(image);
        dto.setPrice(price);
        dto.setQuantity(quantity);
        dto.setSelected(selected);
        dto.setStock(999);
        return dto;
    }

    /** 带 SKU 信息创建一个条目 */
    public static CartItemDTO ofSku(Long skuId, Long productId, String productName,
                                     String specInfo, String image, BigDecimal price,
                                     Integer quantity, Boolean selected, Integer stock) {
        CartItemDTO dto = new CartItemDTO();
        dto.setSkuId(skuId);
        dto.setProductId(productId);
        dto.setProductName(productName);
        dto.setSpecInfo(specInfo != null ? specInfo : "");
        dto.setImage(image != null ? image : "");
        dto.setPrice(price);
        dto.setQuantity(quantity);
        dto.setSelected(selected);
        dto.setStock(stock != null ? stock : 999);
        return dto;
    }

    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getSpecInfo() { return specInfo; }
    public void setSpecInfo(String specInfo) { this.specInfo = specInfo; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Boolean getSelected() { return selected; }
    public void setSelected(Boolean selected) { this.selected = selected; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
}
