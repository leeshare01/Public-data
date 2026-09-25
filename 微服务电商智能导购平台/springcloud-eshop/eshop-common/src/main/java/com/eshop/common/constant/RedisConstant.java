package com.eshop.common.constant;

/**
 * Redis Key 常量
 */
public interface RedisConstant {

    /** 购物车前缀 */
    String CART_PREFIX = "cart:";
    /** 商品缓存前缀 */
    String PRODUCT_PREFIX = "product:";
    /** 分类缓存 */
    String CATEGORY_KEY = "categories:tree";
}
