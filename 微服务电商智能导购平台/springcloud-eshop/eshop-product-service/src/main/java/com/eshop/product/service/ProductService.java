package com.eshop.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eshop.common.dto.PageParam;
import com.eshop.common.dto.PageResult;
import com.eshop.common.exception.BusinessException;
import com.eshop.common.result.ResultCode;
import com.eshop.product.entity.Category;
import com.eshop.product.entity.Product;
import com.eshop.product.entity.ProductAttribute;
import com.eshop.product.entity.ProductSku;
import com.eshop.product.mapper.ProductAttributeMapper;
import com.eshop.product.mapper.ProductMapper;
import com.eshop.product.mapper.ProductSkuMapper;
import com.eshop.product.vector.EmbeddingService;
import com.eshop.product.vector.VectorStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 商品管理
 */
@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductMapper productMapper;
    private final CategoryService categoryService;
    private final ProductSkuMapper skuMapper;
    private final ProductAttributeMapper attributeMapper;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;

    public ProductService(ProductMapper productMapper, CategoryService categoryService,
                          ProductSkuMapper skuMapper, ProductAttributeMapper attributeMapper,
                          EmbeddingService embeddingService, VectorStore vectorStore) {
        this.productMapper = productMapper;
        this.categoryService = categoryService;
        this.skuMapper = skuMapper;
        this.attributeMapper = attributeMapper;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
    }

    /**
     * 启动时自动索引所有商品到向量库（语义搜索兜底用）
     */
    @PostConstruct
    public void initVectorIndex() {
        CompletableFuture.runAsync(() -> {
            try {
                List<Product> products = productMapper.selectList(
                        new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1));
                vectorStore.clear();
                for (Product p : products) {
                    String doc = buildDocumentText(p);
                    float[] vec = embeddingService.embed(doc);
                    vectorStore.add(String.valueOf(p.getId()), vec, null);
                }
                log.info("商品向量索引完成: {} 个商品", products.size());
            } catch (Exception e) {
                log.warn("商品向量索引失败: {}", e.getMessage());
            }
        });
    }

    /**
     * 构建用于向量化的商品文档文本
     */
    private String buildDocumentText(Product p) {
        StringBuilder sb = new StringBuilder();
        String name = p.getName();
        if (name != null) sb.append(name).append(" ").append(name).append(" ");
        if (p.getSubtitle() != null) sb.append(p.getSubtitle()).append(" ");
        if (p.getKeywords() != null) sb.append(p.getKeywords().replace(",", " ")).append(" ");
        if (p.getBrand() != null) sb.append(p.getBrand()).append(" ");
        // 分类名
        try {
            Category cat = categoryService.getById(p.getCategoryId());
            if (cat != null) sb.append(cat.getName()).append(" ");
        } catch (Exception ignored) {}
        if (p.getDescription() != null) sb.append(p.getDescription());
        return sb.toString().trim();
    }

    /**
     * 分页查询商品（支持分类筛选、关键词搜索、价格区间、排序 + 向量语义兜底）
     *
     * 当 SQL 关键词搜索无结果时，自动触发向量语义搜索作为兜底。
     * 这意味着搜"家用品"也能找到"家居用品"分类下的商品。
     */
    public PageResult<Product> page(PageParam pageParam, Long categoryId, String keyword,
                                    BigDecimal minPrice, BigDecimal maxPrice, String sortBy, Boolean asc) {
        // 是否需要向量兜底（关键词搜索返回空时触发）
        boolean needsVectorFallback = StringUtils.hasText(keyword);

        // 构建 SQL 搜索条件
        LambdaQueryWrapper<Product> wrapper = buildSearchWrapper(categoryId, keyword, minPrice, maxPrice, sortBy, asc);

        IPage<Product> page = productMapper.selectPage(
                new Page<>(pageParam.getPage(), pageParam.getSize()), wrapper);

        List<Product> records = page.getRecords();

        // 向量语义兜底：SQL 搜索无结果时，用 embedding 语义召回
        if (needsVectorFallback && (records == null || records.isEmpty()) && vectorStore.size() > 0) {
            try {
                float[] queryVec = embeddingService.embed(keyword);
                var vectorResults = vectorStore.search(queryVec, pageParam.getSize() * 2);

                if (!vectorResults.isEmpty()) {
                    List<Long> vectorIds = vectorResults.stream()
                            .map(r -> Long.parseLong(r.id()))
                            .collect(Collectors.toList());

                    // 按向量得分排序重建查询（保持语义排序）
                    wrapper = new LambdaQueryWrapper<Product>()
                            .eq(Product::getStatus, 1)
                            .in(Product::getId, vectorIds);
                    if (categoryId != null) {
                        wrapper.in(Product::getCategoryId, getCategoryAndChildrenIds(categoryId));
                    }
                    // 价格区间
                    if (minPrice != null) wrapper.ge(Product::getPrice, minPrice);
                    if (maxPrice != null) wrapper.le(Product::getPrice, maxPrice);

                    page = productMapper.selectPage(
                            new Page<>(1, pageParam.getSize()), wrapper);
                    records = page.getRecords();

                    // 按向量相似度得分重新排序
                    Map<Long, Double> scoreMap = vectorResults.stream()
                            .collect(Collectors.toMap(
                                    r -> Long.parseLong(r.id()),
                                    com.eshop.product.vector.SearchResult::score,
                                    (a, b) -> a));
                    records.sort((a, b) -> Double.compare(
                            scoreMap.getOrDefault(b.getId(), 0.0),
                            scoreMap.getOrDefault(a.getId(), 0.0)));

                    log.info("向量语义兜底: keyword={}, 命中 {} 个商品", keyword, records.size());
                }
            } catch (Exception e) {
                log.warn("向量搜索兜底失败: {}", e.getMessage());
            }
        }

        // 填充分类名称
        List<Product> finalRecords = records.stream().peek(p -> {
            try {
                Category category = categoryService.getById(p.getCategoryId());
                p.setCategoryName(category.getName());
            } catch (Exception ignored) {}
        }).collect(Collectors.toList());

        return PageResult.of(finalRecords, page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    /**
     * 构建商品搜索查询条件（抽取为独立方法，支持子串兜底复用）
     */
    private LambdaQueryWrapper<Product> buildSearchWrapper(Long categoryId, String keyword,
                                                           BigDecimal minPrice, BigDecimal maxPrice,
                                                           String sortBy, Boolean asc) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1);

        // 分类筛选（含子分类）
        if (categoryId != null) {
            List<Long> categoryIds = getCategoryAndChildrenIds(categoryId);
            wrapper.in(Product::getCategoryId, categoryIds);
        }

        // 关键词搜索（名称、副标题、品牌、关键词、描述、分类名称）
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> {
                w.like(Product::getName, keyword)
                        .or().like(Product::getSubtitle, keyword)
                        .or().like(Product::getBrand, keyword)
                        .or().like(Product::getKeywords, keyword)
                        .or().like(Product::getDescription, keyword);
                // 分类名称搜索：先完整词匹配，若未命中且关键词较长则尝试 2 字子串
                List<Long> matchedCategoryIds = categoryService.searchByName(keyword).stream()
                        .map(Category::getId).collect(Collectors.toList());
                if (matchedCategoryIds.isEmpty() && keyword.length() >= 3) {
                    for (int i = 0; i <= keyword.length() - 2 && matchedCategoryIds.isEmpty(); i++) {
                        matchedCategoryIds = categoryService.searchByName(keyword.substring(i, i + 2)).stream()
                                .map(Category::getId).collect(Collectors.toList());
                    }
                }
                if (!matchedCategoryIds.isEmpty()) {
                    w.or().in(Product::getCategoryId, matchedCategoryIds);
                }
            });
        }

        // 价格区间
        if (minPrice != null) wrapper.ge(Product::getPrice, minPrice);
        if (maxPrice != null) wrapper.le(Product::getPrice, maxPrice);

        // 排序（兼容前端格式：price-asc / price-desc / newest）
        if (StringUtils.hasText(sortBy)) {
            String effectiveSortBy = sortBy;
            boolean isAsc = asc != null && asc;
            switch (sortBy) {
                case "price-asc" -> { effectiveSortBy = "price"; isAsc = true; }
                case "price-desc" -> { effectiveSortBy = "price"; isAsc = false; }
                case "newest" -> { effectiveSortBy = "createTime"; isAsc = false; }
            }
            switch (effectiveSortBy) {
                case "price" -> wrapper.orderBy(true, isAsc, Product::getPrice);
                case "sales" -> wrapper.orderBy(true, isAsc, Product::getSales);
                case "createTime" -> wrapper.orderBy(true, isAsc, Product::getCreateTime);
                default -> wrapper.orderByDesc(Product::getSales);
            }
        } else {
            wrapper.orderByDesc(Product::getSales);
        }

        return wrapper;
    }

    /**
     * 获取商品详情（含 SKU 列表和参数）
     */
    public Product getById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        if (product.getStatus() != 1) {
            throw new BusinessException(ResultCode.PRODUCT_SOLD_OUT);
        }
        enrichProduct(product);
        return product;
    }

    /**
     * 填充商品附加信息（分类名、SKU列表、参数）
     */
    private void enrichProduct(Product product) {
        // 填充分类名称
        try {
            Category category = categoryService.getById(product.getCategoryId());
            product.setCategoryName(category.getName());
        } catch (Exception ignored) {}

        // 填充 SKU 列表
        List<ProductSku> skuList = skuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>()
                        .eq(ProductSku::getProductId, product.getId())
                        .orderByAsc(ProductSku::getSortOrder));
        for (ProductSku sku : skuList) {
            sku.setSpecDesc(buildSpecDesc(sku.getSpecValues()));
        }
        product.setSkuList(skuList);

        // 填充参数列表
        List<ProductAttribute> attrs = attributeMapper.selectList(
                new LambdaQueryWrapper<ProductAttribute>()
                        .eq(ProductAttribute::getProductId, product.getId())
                        .orderByAsc(ProductAttribute::getSortOrder));
        product.setAttributes(attrs);
    }

    /**
     * 将 SKU 的 JSON 规格值转为可读文本 "红色 / M"
     */
    private String buildSpecDesc(String specValuesJson) {
        if (specValuesJson == null || specValuesJson.isBlank()) return "";
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = mapper.readValue(specValuesJson, java.util.Map.class);
            StringBuilder sb = new StringBuilder();
            for (Object v : map.values()) {
                if (sb.length() > 0) sb.append(" / ");
                sb.append(v == null ? "" : v.toString());
            }
            return sb.toString();
        } catch (Exception e) {
            return specValuesJson;
        }
    }

    /**
     * 管理端获取商品详情（不限上下架状态）
     */
    public Product adminGetById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        enrichProduct(product);
        return product;
    }

    /**
     * 新增商品（含 SKU 和参数）
     */
    public Product create(Product product) {
        categoryService.getById(product.getCategoryId());
        if (product.getStatus() == null) product.setStatus(1);
        if (product.getSales() == null) product.setSales(0);
        productMapper.insert(product);

        // 处理 SKU 列表
        if (product.getSkuList() != null && !product.getSkuList().isEmpty()) {
            for (ProductSku sku : product.getSkuList()) {
                sku.setId(null);
                sku.setProductId(product.getId());
                skuMapper.insert(sku);
            }
        }

        // 处理参数列表
        if (product.getAttributes() != null && !product.getAttributes().isEmpty()) {
            for (ProductAttribute attr : product.getAttributes()) {
                attr.setId(null);
                attr.setProductId(product.getId());
                attributeMapper.insert(attr);
            }
        }

        return product;
    }

    /**
     * 更新商品（含 SKU 和参数替换）
     */
    public Product update(Long id, Product product) {
        Product existing = productMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        if (product.getName() != null) existing.setName(product.getName());
        if (product.getSubtitle() != null) existing.setSubtitle(product.getSubtitle());
        if (product.getBrand() != null) existing.setBrand(product.getBrand());
        if (product.getDescription() != null) existing.setDescription(product.getDescription());
        if (product.getMainImage() != null) existing.setMainImage(product.getMainImage());
        if (product.getCategoryId() != null) existing.setCategoryId(product.getCategoryId());
        if (product.getPrice() != null) existing.setPrice(product.getPrice());
        if (product.getOriginalPrice() != null) existing.setOriginalPrice(product.getOriginalPrice());
        if (product.getStatus() != null) existing.setStatus(product.getStatus());
        if (product.getKeywords() != null) existing.setKeywords(product.getKeywords());
        if (product.getSubImages() != null) existing.setSubImages(product.getSubImages());
        productMapper.updateById(existing);

        // 处理 SKU 列表：全量替换
        if (product.getSkuList() != null) {
            skuMapper.delete(new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, id));
            for (ProductSku sku : product.getSkuList()) {
                sku.setId(null);
                sku.setProductId(id);
                skuMapper.insert(sku);
            }
        }

        // 处理参数列表：全量替换
        if (product.getAttributes() != null) {
            attributeMapper.delete(new LambdaQueryWrapper<ProductAttribute>().eq(ProductAttribute::getProductId, id));
            for (ProductAttribute attr : product.getAttributes()) {
                attr.setId(null);
                attr.setProductId(id);
                attributeMapper.insert(attr);
            }
        }

        return existing;
    }

    /**
     * 删除商品（物理删除，含 SKU 和参数）
     */
    public void delete(Long id) {
        Product existing = productMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        skuMapper.delete(new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, id));
        attributeMapper.delete(new LambdaQueryWrapper<ProductAttribute>().eq(ProductAttribute::getProductId, id));
        productMapper.deleteById(id);
    }

    /**
     * 查询 SKU 详情（供其他服务 Feign 调用）
     */
    public ProductSku getSkuById(Long id) {
        ProductSku sku = skuMapper.selectById(id);
        if (sku == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "SKU不存在");
        }
        return sku;
    }

    /**
     * 查询商品下的所有 SKU（按排序字段升序）
     */
    public List<ProductSku> getSkusByProductId(Long productId) {
        return skuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>()
                        .eq(ProductSku::getProductId, productId)
                        .orderByAsc(ProductSku::getSortOrder));
    }

    /**
     * 批量查询 SKU（供购物车 Feign 批量调用，解决 N+1 问题）
     */
    public List<ProductSku> getSkusByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        return skuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>()
                        .in(ProductSku::getId, ids)
                        .orderByAsc(ProductSku::getSortOrder));
    }

    /**
     * 批量查询商品（供其他服务 Feign 调用）
     */
    public List<Product> listByIds(List<Long> ids) {
        return productMapper.selectBatchIds(ids);
    }

    /**
     * 管理员分页查询商品（不限制上下架状态）
     */
    public PageResult<Product> adminPage(PageParam pageParam, Long categoryId, String keyword, Integer status) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.in(Product::getCategoryId, getCategoryAndChildrenIds(categoryId));
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> {
                w.like(Product::getName, keyword)
                        .or().like(Product::getSubtitle, keyword)
                        .or().like(Product::getBrand, keyword);
            });
        }
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        wrapper.orderByDesc(Product::getCreateTime);

        IPage<Product> page = productMapper.selectPage(
                new Page<>(pageParam.getPage(), pageParam.getSize()), wrapper);

        List<Product> records = page.getRecords().stream().peek(p -> {
            try {
                Category cat = categoryService.getById(p.getCategoryId());
                p.setCategoryName(cat.getName());
            } catch (Exception ignored) {}
        }).collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    /**
     * 获取分类及其所有子分类ID
     */
    private List<Long> getCategoryAndChildrenIds(Long categoryId) {
        List<Long> ids = new java.util.ArrayList<>();
        ids.add(categoryId);
        collectChildIds(categoryId, ids);
        return ids;
    }

    private void collectChildIds(Long parentId, List<Long> ids) {
        List<Category> children = categoryService.getByParentId(parentId);
        for (Category child : children) {
            ids.add(child.getId());
            collectChildIds(child.getId(), ids);
        }
    }
}
