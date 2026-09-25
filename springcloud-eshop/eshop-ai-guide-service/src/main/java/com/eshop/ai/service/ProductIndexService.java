package com.eshop.ai.service;

import com.eshop.ai.feign.ProductFeignClient;
import com.eshop.ai.vector.EmbeddingService;
import com.eshop.ai.vector.VectorStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 商品索引服务
 *
 * 将商品数据向量化并存入向量库，供 RAG 混合检索使用。
 * 在服务启动时自动执行全量索引。
 */
@Service
public class ProductIndexService {

    private static final Logger log = LoggerFactory.getLogger(ProductIndexService.class);

    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final ProductFeignClient productFeignClient;

    public ProductIndexService(EmbeddingService embeddingService,
                               VectorStore vectorStore,
                               ProductFeignClient productFeignClient) {
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.productFeignClient = productFeignClient;
    }

    /**
     * 启动时自动索引所有商品（异步执行，不阻塞启动）
     */
    @PostConstruct
    public void init() {
        log.info("商品向量索引服务已启动，将在后台索引所有商品...");
        CompletableFuture.runAsync(this::indexAllProducts)
                .exceptionally(e -> {
                    log.error("商品索引失败，30秒后重试: {}", e.getMessage());
                    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                    scheduler.schedule(this::indexAllProducts, 30, TimeUnit.SECONDS);
                    scheduler.shutdown();
                    return null;
                });
    }

    /**
     * 全量索引所有商品
     */
    public synchronized void indexAllProducts() {
        try {
            log.info("开始全量商品索引...");

            // 获取所有上架商品（不分页：一次查 1000 条，覆盖所有商品）
            var result = productFeignClient.searchProducts(1, 1000, null,
                    null, null, null, null, null);

            if (result == null || result.getData() == null) {
                log.warn("商品服务返回为空，跳过索引");
                return;
            }

            List<Map<String, Object>> products = result.getData().getRecords();
            if (products == null || products.isEmpty()) {
                log.warn("商品列表为空，跳过索引");
                return;
            }

            // 清空旧索引，重建
            vectorStore.clear();
            log.info("已清空旧索引，开始索引 {} 个商品...", products.size());

            int indexed = 0;
            int failed = 0;
            for (Map<String, Object> product : products) {
                try {
                    String docText = buildDocumentText(product);
                    float[] vector = embeddingService.embed(docText);
                    String id = String.valueOf(product.get("id"));
                    vectorStore.add(id, vector, product);
                    indexed++;
                } catch (Exception e) {
                    failed++;
                    if (failed <= 3) {
                        log.warn("商品索引失败: productId={}, error={}",
                                product.get("id"), e.getMessage());
                    }
                }
            }

            log.info("商品索引完成: 成功={}, 失败={}, 向量库总计={}",
                    indexed, failed, vectorStore.size());

        } catch (Exception e) {
            log.error("商品索引异常: {}", e.getMessage(), e);
            throw new RuntimeException("商品索引失败", e);
        }
    }

    /**
     * 增量索引单个商品
     */
    public void indexProduct(Map<String, Object> product) {
        try {
            String docText = buildDocumentText(product);
            float[] vector = embeddingService.embed(docText);
            String id = String.valueOf(product.get("id"));
            vectorStore.add(id, vector, product);
            log.debug("商品索引成功: id={}", id);
        } catch (Exception e) {
            log.warn("商品索引失败: productId={}, error={}", product.get("id"), e.getMessage());
        }
    }

    /**
     * 构建用于向量化的商品文档文本
     *
     * 策略：将商品的关键信息拼接为一段文本，使语义相似的商品的向量距离更近。
     * 包含：名称、副标题、关键词、品牌、分类、描述。
     * 权重越大的字段重复次数越多（隐式加权）。
     */
    private String buildDocumentText(Map<String, Object> product) {
        StringBuilder sb = new StringBuilder();

        // 商品名称（最高权重：出现 3 次）
        String name = getStr(product, "name");
        if (!name.isEmpty()) {
            sb.append(name).append(" ").append(name).append(" ").append(name).append(" ");
        }

        // 副标题
        String subtitle = getStr(product, "subtitle");
        if (!subtitle.isEmpty()) sb.append(subtitle).append(" ");

        // 关键词（如果有）
        String keywords = getStr(product, "keywords");
        if (!keywords.isEmpty()) {
            // 关键词可能是逗号分隔的多个词
            sb.append(keywords.replace(",", " ")).append(" ");
        }

        // 品牌
        String brand = getStr(product, "brand");
        if (!brand.isEmpty()) sb.append(brand).append(" ");

        // 分类名称
        String categoryName = getStr(product, "categoryName");
        if (!categoryName.isEmpty()) {
            sb.append(categoryName).append(" ").append(categoryName).append(" ");
        }

        // 描述
        String description = getStr(product, "description");
        if (!description.isEmpty()) sb.append(description);

        return sb.toString().trim();
    }

    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val == null ? "" : val.toString().trim();
    }

    /** 获取当前向量库中已索引的商品数量 */
    public int getIndexedCount() {
        return vectorStore.size();
    }
}
