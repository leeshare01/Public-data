package com.eshop.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品实体
 */
public class Product {

    private Long id;
    private String name;
    private String subtitle;
    private String brand;
    private String description;
    private String mainImage;
    private String subImages;      // JSON: 轮播图URL数组
    private Long categoryId;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer sales;
    private Integer status;        // 0-下架 1-上架
    private String keywords;       // 搜索关键词，逗号分隔
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 非数据库字段：分类名称 */
    @TableField(exist = false)
    private String categoryName;

    /** 非数据库字段：SKU列表 */
    @TableField(exist = false)
    private List<ProductSku> skuList;

    /** 非数据库字段：参数/属性列表 */
    @TableField(exist = false)
    private List<ProductAttribute> attributes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getMainImage() { return mainImage; }
    public void setMainImage(String mainImage) { this.mainImage = mainImage; }
    public String getSubImages() { return subImages; }
    public void setSubImages(String subImages) { this.subImages = subImages; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public Integer getSales() { return sales; }
    public void setSales(Integer sales) { this.sales = sales; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public List<ProductSku> getSkuList() { return skuList; }
    public void setSkuList(List<ProductSku> skuList) { this.skuList = skuList; }
    public List<ProductAttribute> getAttributes() { return attributes; }
    public void setAttributes(List<ProductAttribute> attributes) { this.attributes = attributes; }
}
