package com.eshop.order.feign;

import com.eshop.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "eshop-product-service", path = "/api/product")
public interface ProductClient {

    @GetMapping("/{id}")
    Result<Map<String, Object>> getById(@PathVariable("id") Long id);

    @PostMapping("/batch")
    Result<List<Map<String, Object>>> listByIds(@RequestBody List<Long> ids);
}
