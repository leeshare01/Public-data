package com.eshop.product.controller;

import com.eshop.common.result.Result;
import com.eshop.product.entity.Category;
import com.eshop.product.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product/category")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /** 获取分类树 */
    @GetMapping("/tree")
    public Result<List<Category>> getTree() {
        return Result.success(categoryService.getTree());
    }

    /** 获取一级分类 */
    @GetMapping("/top")
    public Result<List<Category>> getTopLevel() {
        return Result.success(categoryService.getTopLevel());
    }

    /** 根据父ID获取子分类 */
    @GetMapping("/{parentId}/children")
    public Result<List<Category>> getChildren(@PathVariable Long parentId) {
        return Result.success(categoryService.getByParentId(parentId));
    }

    /** 获取分类详情 */
    @GetMapping("/{id}")
    public Result<Category> getById(@PathVariable Long id) {
        return Result.success(categoryService.getById(id));
    }

    /** 新增分类 */
    @PostMapping
    public Result<Category> create(@RequestBody Category category) {
        return Result.success("新增成功", categoryService.create(category));
    }

    /** 更新分类 */
    @PutMapping("/{id}")
    public Result<Category> update(@PathVariable Long id, @RequestBody Category category) {
        return Result.success("更新成功", categoryService.update(id, category));
    }

    /** 删除分类 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success("删除成功", null);
    }
}
