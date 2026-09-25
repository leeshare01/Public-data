package com.eshop.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品分类
 */
public class Category {

    private Long id;
    private String name;
    private Long parentId;
    private Integer level;
    private String icon;
    private Integer sort;
    private LocalDateTime createTime;

    /** 非数据库字段：子分类列表 */
    @TableField(exist = false)
    private List<Category> children;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public List<Category> getChildren() { return children; }
    public void setChildren(List<Category> children) { this.children = children; }
}
