package com.eshop.cart.service;

import com.eshop.cart.dto.CartItemDTO;
import com.eshop.cart.dto.CartSummaryDTO;
import com.eshop.cart.dto.ProductDTO;
import com.eshop.cart.dto.ProductSkuDTO;
import com.eshop.cart.feign.ProductClient;
import com.eshop.common.constant.RedisConstant;
import com.eshop.common.exception.BusinessException;
import com.eshop.common.result.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 购物车服务 — Redis 实现
 * Redis Hash 结构：cart:{userId} → field: skuId, value: "quantity,selected"
 * 支持多规格商品（每个 SKU 独立条目）
 */
@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final StringRedisTemplate redisTemplate;
    private final ProductClient productClient;
    private final ObjectMapper objectMapper;

    public CartService(StringRedisTemplate redisTemplate, ProductClient productClient,
                       ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.productClient = productClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 获取购物车所有条目（SKU 级别）
     */
    public CartSummaryDTO getItems(Long userId) {
        String key = cartKey(userId);
        Map<Object, Object> raw = redisTemplate.opsForHash().entries(key);
        if (raw.isEmpty()) return CartSummaryDTO.of(Collections.emptyList());

        Map<String, String> entries = raw.entrySet().stream()
                .collect(Collectors.toMap(e -> (String) e.getKey(), e -> (String) e.getValue()));

        // 收集所有 skuId
        List<Long> skuIds = entries.keySet().stream()
                .map(Long::valueOf).collect(Collectors.toList());

        // 批量查询 SKU 详情
        Map<Long, ProductSkuDTO> skuMap = fetchSkuMap(skuIds);

        // 收集所有 productId → 批量查询商品名
        Set<Long> productIds = skuMap.values().stream()
                .map(ProductSkuDTO::getProductId).collect(Collectors.toSet());
        Map<Long, ProductDTO> productMap = fetchProductMap(new ArrayList<>(productIds));

        List<CartItemDTO> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            Long skuId = Long.valueOf(entry.getKey());
            String[] parts = entry.getValue().split(",");
            int quantity = Integer.parseInt(parts[0]);
            boolean selected = parts.length > 1 && "1".equals(parts[1]);

            ProductSkuDTO sku = skuMap.get(skuId);
            if (sku == null) {
                log.warn("购物车 SKU 不存在，跳过: {}", skuId);
                continue;
            }

            String productName = "";
            ProductDTO product = productMap.get(sku.getProductId());
            if (product != null) {
                productName = product.getName();
            }

            String specInfo = parseSpecValues(sku.getSpecValues());
            String image = sku.getImage() != null && !sku.getImage().isBlank()
                    ? sku.getImage() : (product != null ? product.getMainImage() : "");

            result.add(CartItemDTO.ofSku(skuId, sku.getProductId(), productName,
                    specInfo, image, sku.getPrice(), quantity, selected, sku.getStock()));
        }

        return CartSummaryDTO.of(result);
    }

    /**
     * 添加商品到购物车（支持 SKU ID 或 Product ID）
     */
    public void addItem(Long userId, Long skuId, int quantity) {
        // 先尝试作为 SKU ID 查询
        ProductSkuDTO sku = fetchSku(skuId);

        // 如果 SKU 不存在，尝试作为 Product ID 查询并取默认 SKU
        if (sku == null) {
            // 首页加购传的是 productId，直接查该商品的所有 SKU 取第一个
            List<ProductSkuDTO> skus = fetchSkusByProductId(skuId);
            if (skus != null && !skus.isEmpty()) {
                sku = skus.get(0);
            }
        }

        if (sku == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品或SKU不存在");
        }

        // 直接用 SKU 的真实 ID 存入 Redis（不用 productId）
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();
        String key = cartKey(userId);
        String field = String.valueOf(sku.getId());
        String val = ops.get(key, field);

        int newQuantity = quantity;
        boolean selected = true;
        if (val != null) {
            String[] parts = val.split(",");
            newQuantity += Integer.parseInt(parts[0]);
            selected = parts.length > 1 && "1".equals(parts[1]);
        }
        ops.put(key, field, newQuantity + "," + (selected ? "1" : "0"));

        log.debug("加购: userId={}, skuId={}, quantity={}, field={}", userId, skuId, quantity, field);
    }

    /**
     * 修改数量
     */
    public void updateQuantity(Long userId, Long skuId, int quantity) {
        if (quantity <= 0) {
            removeItem(userId, skuId);
            return;
        }
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();
        String key = cartKey(userId);
        String field = String.valueOf(skuId);
        String val = ops.get(key, field);
        if (val == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "购物车中不存在该商品");
        }
        String[] parts = val.split(",");
        boolean selected = parts.length > 1 && "1".equals(parts[1]);
        ops.put(key, field, quantity + "," + (selected ? "1" : "0"));
    }

    /**
     * 删除购物车商品
     */
    public void removeItem(Long userId, Long skuId) {
        redisTemplate.opsForHash().delete(cartKey(userId), String.valueOf(skuId));
    }

    /**
     * 选中/取消选中
     */
    public void selectItem(Long userId, Long skuId, boolean selected) {
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();
        String key = cartKey(userId);
        String field = String.valueOf(skuId);
        String val = ops.get(key, field);
        if (val == null) return;
        String[] parts = val.split(",");
        int quantity = Integer.parseInt(parts[0]);
        ops.put(key, field, quantity + "," + (selected ? "1" : "0"));
    }

    /**
     * 全选/全不选
     */
    public void selectAll(Long userId, boolean selected) {
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();
        String key = cartKey(userId);
        Map<String, String> entries = ops.entries(key);
        Map<String, String> updated = new HashMap<>();
        for (Map.Entry<String, String> e : entries.entrySet()) {
            String[] parts = e.getValue().split(",");
            updated.put(e.getKey(), parts[0] + "," + (selected ? "1" : "0"));
        }
        if (!updated.isEmpty()) {
            ops.putAll(key, updated);
        }
    }

    /**
     * 清空购物车
     */
    public void clear(Long userId) {
        redisTemplate.delete(cartKey(userId));
    }

    /**
     * 获取选中商品汇总（下单用）
     */
    public CartSummaryDTO getSummary(Long userId) {
        String key = cartKey(userId);
        Map<Object, Object> raw = redisTemplate.opsForHash().entries(key);
        if (raw.isEmpty()) return CartSummaryDTO.of(Collections.emptyList());

        Map<String, String> entries = raw.entrySet().stream()
                .collect(Collectors.toMap(e -> (String) e.getKey(), e -> (String) e.getValue()));

        // 只处理选中商品
        List<Long> selectedSkuIds = new ArrayList<>();
        for (Map.Entry<String, String> e : entries.entrySet()) {
            String[] parts = e.getValue().split(",");
            boolean selected = parts.length > 1 && "1".equals(parts[1]);
            if (selected) {
                selectedSkuIds.add(Long.valueOf(e.getKey()));
            }
        }

        if (selectedSkuIds.isEmpty()) return CartSummaryDTO.of(Collections.emptyList());

        Map<Long, ProductSkuDTO> skuMap = fetchSkuMap(selectedSkuIds);
        Set<Long> productIds = skuMap.values().stream()
                .map(ProductSkuDTO::getProductId).collect(Collectors.toSet());
        Map<Long, ProductDTO> productMap = fetchProductMap(new ArrayList<>(productIds));

        List<CartItemDTO> result = new ArrayList<>();
        for (Long skuId : selectedSkuIds) {
            String val = entries.get(String.valueOf(skuId));
            int quantity = Integer.parseInt(val.split(",")[0]);

            ProductSkuDTO sku = skuMap.get(skuId);
            if (sku == null) continue;

            String productName = "";
            ProductDTO product = productMap.get(sku.getProductId());
            if (product != null) productName = product.getName();

            String specInfo = parseSpecValues(sku.getSpecValues());
            String image = sku.getImage() != null && !sku.getImage().isBlank()
                    ? sku.getImage() : (product != null ? product.getMainImage() : "");

            result.add(CartItemDTO.ofSku(skuId, sku.getProductId(), productName,
                    specInfo, image, sku.getPrice(), quantity, true, sku.getStock()));
        }

        return CartSummaryDTO.of(result);
    }

    // ========== 内部方法 ==========

    private String cartKey(Long userId) {
        return RedisConstant.CART_PREFIX + userId;
    }

    /** 查询单个 SKU */
    private ProductSkuDTO fetchSku(Long skuId) {
        try {
            var result = productClient.getSkuById(skuId);
            if (result != null && result.getCode() == 200) {
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("Feign 查询 SKU 失败: {}", e.getMessage());
        }
        return null;
    }

    /** 查询商品下的所有 SKU */
    private List<ProductSkuDTO> fetchSkusByProductId(Long productId) {
        try {
            var result = productClient.getSkusByProductId(productId);
            if (result != null && result.getCode() == 200) {
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("Feign 查询商品SKU列表失败: {}", e.getMessage());
        }
        return null;
    }

    /** 批量查询 SKU（使用批量接口避免 N+1 问题） */
    private Map<Long, ProductSkuDTO> fetchSkuMap(List<Long> skuIds) {
        Map<Long, ProductSkuDTO> map = new HashMap<>();
        if (skuIds.isEmpty()) return map;
        try {
            var result = productClient.getSkusByIds(skuIds);
            if (result != null && result.getCode() == 200 && result.getData() != null) {
                for (ProductSkuDTO sku : result.getData()) {
                    map.put(sku.getId(), sku);
                }
            }
        } catch (Exception e) {
            log.warn("Feign 批量查询 SKU 失败: {}", e.getMessage());
        }
        return map;
    }

    /** 批量查询商品 */
    private Map<Long, ProductDTO> fetchProductMap(List<Long> productIds) {
        Map<Long, ProductDTO> map = new HashMap<>();
        if (productIds.isEmpty()) return map;
        try {
            var result = productClient.listByIds(productIds);
            if (result != null && result.getCode() == 200 && result.getData() != null) {
                for (ProductDTO p : result.getData()) {
                    map.put(p.getId(), p);
                }
            }
        } catch (Exception e) {
            log.warn("Feign 批量查询商品失败: {}", e.getMessage());
        }
        return map;
    }

    /** 将 SKU 的 JSON 规格值转为可读文本 "红色 / M" */
    private String parseSpecValues(String specValuesJson) {
        if (specValuesJson == null || specValuesJson.isBlank()) return "";
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(specValuesJson, Map.class);
            return map.values().stream()
                    .map(v -> v == null ? "" : v.toString())
                    .collect(Collectors.joining(" / "));
        } catch (Exception e) {
            return specValuesJson;
        }
    }
}
