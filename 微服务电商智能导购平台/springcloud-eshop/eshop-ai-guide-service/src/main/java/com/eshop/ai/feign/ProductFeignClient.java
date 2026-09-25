package com.eshop.ai.feign;

import com.eshop.common.dto.PageResult;
import com.eshop.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 商品服务 Feign 客户端
 */
@FeignClient(name = "eshop-product-service", path = "/api/product",
        url = "${product.service.url:}")
public interface ProductFeignClient {

    /** 分页搜索商品 */
    @GetMapping("/page")
    Result<PageResult<Map<String, Object>>> searchProducts(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "asc", required = false) Boolean asc);

    /** 批量查询商品 */
    @PostMapping("/batch")
    Result<List<Map<String, Object>>> getProductsByIds(@RequestBody List<Long> ids);
}
