package com.eshop.product.controller;

import com.eshop.common.dto.PageParam;
import com.eshop.common.dto.PageResult;
import com.eshop.common.result.Result;
import com.eshop.common.result.ResultCode;
import com.eshop.product.entity.ProductSku;
import com.eshop.product.entity.Product;
import com.eshop.product.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /** 分页查询商品（支持分类筛选、搜索、价格区间、排序） */
    @GetMapping("/page")
    public Result<PageResult<Product>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) Boolean asc) {
        PageParam pageParam = new PageParam();
        pageParam.setPage(page);
        pageParam.setSize(size);
        return Result.success(productService.page(pageParam, categoryId, keyword, minPrice, maxPrice, sortBy, asc));
    }

    /** 管理员分页查询（不限制上下架状态） */
    @GetMapping("/admin/page")
    public Result<PageResult<Product>> adminPage(
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        if (!"ADMIN".equals(role)) {
            return Result.error(ResultCode.FORBIDDEN, "无管理员权限");
        }
        PageParam pageParam = new PageParam();
        pageParam.setPage(page);
        pageParam.setSize(size);
        return Result.success(productService.adminPage(pageParam, categoryId, keyword, status));
    }

    /** 获取商品详情 */
    @GetMapping("/{id}")
    public Result<Product> getById(@PathVariable Long id) {
        return Result.success(productService.getById(id));
    }

    /** 管理端获取商品详情（不限上下架状态） */
    @GetMapping("/admin/{id}")
    public Result<Product> adminGetById(
            @RequestHeader(value = "X-Role", required = false) String role,
            @PathVariable Long id) {
        if (!"ADMIN".equals(role)) {
            return Result.error(ResultCode.FORBIDDEN, "无管理员权限");
        }
        return Result.success(productService.adminGetById(id));
    }

    /** 新增商品 */
    @PostMapping
    public Result<Product> create(
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestBody Product product) {
        if (!"ADMIN".equals(role)) {
            return Result.error(ResultCode.FORBIDDEN, "无管理员权限");
        }
        return Result.success("新增成功", productService.create(product));
    }

    /** 更新商品 */
    @PutMapping("/{id}")
    public Result<Product> update(
            @RequestHeader(value = "X-Role", required = false) String role,
            @PathVariable Long id,
            @RequestBody Product product) {
        if (!"ADMIN".equals(role)) {
            return Result.error(ResultCode.FORBIDDEN, "无管理员权限");
        }
        return Result.success("更新成功", productService.update(id, product));
    }

    /** 删除商品（下架） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @RequestHeader(value = "X-Role", required = false) String role,
            @PathVariable Long id) {
        if (!"ADMIN".equals(role)) {
            return Result.error(ResultCode.FORBIDDEN, "无管理员权限");
        }
        productService.delete(id);
        return Result.success("删除成功", null);
    }

    /** 批量查询（内部 Feign 调用） */
    @PostMapping("/batch")
    public Result<List<Product>> listByIds(@RequestBody List<Long> ids) {
        return Result.success(productService.listByIds(ids));
    }

    /** 查询 SKU 详情（购物车 Feign 调用） */
    @GetMapping("/sku/{id}")
    public Result<ProductSku> getSkuById(@PathVariable Long id) {
        return Result.success(productService.getSkuById(id));
    }

    /** 查询商品下的所有 SKU（购物车 Feign 调用） */
    @GetMapping("/{productId}/skus")
    public Result<List<ProductSku>> getSkusByProductId(@PathVariable Long productId) {
        return Result.success(productService.getSkusByProductId(productId));
    }

    /** 批量查询 SKU（购物车 Feign 批量调用，解决 N+1 问题） */
    @PostMapping("/skus/batch")
    public Result<List<ProductSku>> getSkusByIds(@RequestBody List<Long> ids) {
        return Result.success(productService.getSkusByIds(ids));
    }
}
