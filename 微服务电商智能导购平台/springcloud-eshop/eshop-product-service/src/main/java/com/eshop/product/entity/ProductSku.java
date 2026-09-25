package com.eshop.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品 SKU（库存量单位）
 */
@TableName("product_sku")
public class ProductSku {

    private Long id;
    private Long productId;
    private String skuCode;
    private String specValues;     // JSON: {"颜色":"红色","尺寸":"M"}
    private BigDecimal price;
    private Integer stock;
    private Integer lockedStock;
    private String image;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 非数据库字段：规格中文描述，如 "红色 / M" */
    @TableField(exist = false)
    private String specDesc;

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
    public Integer getLockedStock() { return lockedStock; }
    public void setLockedStock(Integer lockedStock) { this.lockedStock = lockedStock; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public String getSpecDesc() { return specDesc; }
    public void setSpecDesc(String specDesc) { this.specDesc = specDesc; }
}
