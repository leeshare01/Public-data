package com.eshop.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eshop.common.exception.BusinessException;
import com.eshop.common.result.ResultCode;
import com.eshop.product.entity.Category;
import com.eshop.product.mapper.CategoryMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分类管理
 */
@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    /**
     * 获取分类树（层级结构）
     */
    public List<Category> getTree() {
        List<Category> all = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort));
        return buildTree(all, 0L);
    }

    /**
     * 获取一级分类
     */
    public List<Category> getTopLevel() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().eq(Category::getLevel, 1).orderByAsc(Category::getSort));
    }

    /**
     * 根据父ID获取子分类
     */
    public List<Category> getByParentId(Long parentId) {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().eq(Category::getParentId, parentId).orderByAsc(Category::getSort));
    }

    /**
     * 根据ID获取分类
     */
    public Category getById(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分类不存在");
        }
        return category;
    }

    /**
     * 关键词搜索分类名（供 RAG 检索使用）
     */
    public List<Category> searchByName(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        return categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .like(Category::getName, keyword)
                        .orderByAsc(Category::getSort));
    }

    /**
     * 新增分类
     */
    public Category create(Category category) {
        // 自动计算层级
        if (category.getParentId() != null && category.getParentId() > 0) {
            Category parent = categoryMapper.selectById(category.getParentId());
            if (parent == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "父分类不存在");
            }
            category.setLevel(parent.getLevel() + 1);
        } else {
            category.setParentId(0L);
            category.setLevel(1);
        }
        if (category.getSort() == null) category.setSort(0);
        categoryMapper.insert(category);
        return category;
    }

    /**
     * 更新分类
     */
    public Category update(Long id, Category category) {
        Category existing = getById(id);
        if (category.getName() != null) existing.setName(category.getName());
        if (category.getIcon() != null) existing.setIcon(category.getIcon());
        if (category.getSort() != null) existing.setSort(category.getSort());
        categoryMapper.updateById(existing);
        return existing;
    }

    /**
     * 删除分类（同时删除子分类）
     */
    public void delete(Long id) {
        Category existing = getById(id);
        // 递归删除子分类
        deleteRecursively(id);
    }

    private void deleteRecursively(Long parentId) {
        List<Category> children = getByParentId(parentId);
        for (Category child : children) {
            deleteRecursively(child.getId());
        }
        categoryMapper.deleteById(parentId);
    }

    /**
     * 构建分类树
     */
    private List<Category> buildTree(List<Category> all, Long parentId) {
        return all.stream()
                .filter(c -> c.getParentId().equals(parentId))
                .peek(c -> c.setChildren(buildTree(all, c.getId())))
                .collect(Collectors.toList());
    }
}
