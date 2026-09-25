package com.eshop.cart.controller;

import com.eshop.cart.dto.CartSummaryDTO;
import com.eshop.cart.service.CartService;
import com.eshop.common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /** 获取购物车列表 */
    @GetMapping("/items")
    public Result<CartSummaryDTO> getItems(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(cartService.getItems(userId));
    }

    /** 添加商品到购物车（支持 SKU 级别） */
    @PostMapping("/items")
    public Result<Void> addItem(@RequestHeader("X-User-Id") Long userId,
                                 @RequestBody Map<String, Object> body) {
        Long skuId = Long.valueOf(body.get("skuId").toString());
        int quantity = body.containsKey("quantity") ? Integer.parseInt(body.get("quantity").toString()) : 1;
        cartService.addItem(userId, skuId, quantity);
        return Result.success("添加成功", null);
    }

    /** 修改数量 */
    @PutMapping("/items/{skuId}")
    public Result<Void> updateQuantity(@RequestHeader("X-User-Id") Long userId,
                                        @PathVariable Long skuId,
                                        @RequestBody Map<String, Object> body) {
        int quantity = Integer.parseInt(body.get("quantity").toString());
        cartService.updateQuantity(userId, skuId, quantity);
        return Result.success(null);
    }

    /** 删除购物车商品 */
    @DeleteMapping("/items/{skuId}")
    public Result<Void> removeItem(@RequestHeader("X-User-Id") Long userId,
                                    @PathVariable Long skuId) {
        cartService.removeItem(userId, skuId);
        return Result.success(null);
    }

    /** 选中/取消选中 */
    @PutMapping("/items/{skuId}/select")
    public Result<Void> selectItem(@RequestHeader("X-User-Id") Long userId,
                                    @PathVariable Long skuId,
                                    @RequestBody Map<String, Object> body) {
        boolean selected = Boolean.TRUE.equals(body.get("selected"));
        cartService.selectItem(userId, skuId, selected);
        return Result.success(null);
    }

    /** 全选/全不选 */
    @PutMapping("/select-all")
    public Result<Void> selectAll(@RequestHeader("X-User-Id") Long userId,
                                   @RequestParam boolean selected) {
        cartService.selectAll(userId, selected);
        return Result.success(null);
    }

    /** 清空购物车 */
    @DeleteMapping("/items")
    public Result<Void> clear(@RequestHeader("X-User-Id") Long userId) {
        cartService.clear(userId);
        return Result.success("已清空", null);
    }

    /** 获取选中商品汇总 */
    @GetMapping("/summary")
    public Result<CartSummaryDTO> getSummary(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(cartService.getSummary(userId));
    }
}
