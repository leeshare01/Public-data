package com.eshop.order.feign;

import com.eshop.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "eshop-cart-service", path = "/api/cart")
public interface CartClient {

    /** 获取选中商品汇总 */
    @GetMapping("/summary")
    Result<Map<String, Object>> getSummary(@RequestHeader("X-User-Id") Long userId);

    /** 清空购物车中已下单的商品 */
    @DeleteMapping("/items")
    Result<Void> clear(@RequestHeader("X-User-Id") Long userId);
}
