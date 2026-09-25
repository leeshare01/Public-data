package com.eshop.cart.feign;

import com.eshop.common.result.Result;
import com.eshop.cart.dto.ProductDTO;
import com.eshop.cart.dto.ProductSkuDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "eshop-product-service", path = "/api/product")
public interface ProductClient {

    @GetMapping("/{id}")
    Result<ProductDTO> getById(@PathVariable("id") Long id);

    @GetMapping("/sku/{id}")
    Result<ProductSkuDTO> getSkuById(@PathVariable("id") Long id);

    @GetMapping("/{productId}/skus")
    Result<List<ProductSkuDTO>> getSkusByProductId(@PathVariable("productId") Long productId);

    @PostMapping("/skus/batch")
    Result<List<ProductSkuDTO>> getSkusByIds(@RequestBody List<Long> ids);

    @PostMapping("/batch")
    Result<List<ProductDTO>> listByIds(@RequestBody List<Long> ids);
}
