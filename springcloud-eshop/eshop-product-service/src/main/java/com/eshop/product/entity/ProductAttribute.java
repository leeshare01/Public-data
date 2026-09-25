package com.eshop.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 商品参数/属性
 */
@TableName("product_attribute")
public class ProductAttribute {

    private Long id;
    private Long productId;
    private String attrName;
    private String attrValue;
    private Integer sortOrder;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getAttrName() { return attrName; }
    public void setAttrName(String attrName) { this.attrName = attrName; }
    public String getAttrValue() { return attrValue; }
    public void setAttrValue(String attrValue) { this.attrValue = attrValue; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
