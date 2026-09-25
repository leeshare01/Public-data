package com.eshop.ai.service;

import com.eshop.ai.entity.Conversation;
import com.eshop.ai.entity.ConversationMessage;
import com.eshop.ai.feign.ProductFeignClient;
import com.eshop.ai.vector.EmbeddingService;
import com.eshop.ai.vector.SearchResult;
import com.eshop.ai.vector.VectorStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * AI 导购核心业务服务
 *
 * 完整的 RAG 流程：
 *   用户提问 → 查询理解 → 双路混合检索（向量语义 + 关键词精确）
 *   → 融合重排 → 上下文注入 → LLM 生成 → 流式/非流式返回
 */
@Service
public class AiGuideService {

    private static final Logger log = LoggerFactory.getLogger(AiGuideService.class);

    // 混合检索权重
    private static final double VECTOR_WEIGHT = 0.6;
    private static final double KEYWORD_WEIGHT = 0.4;

    private final ConversationService conversationService;
    private final DeepSeekService deepSeekService;
    private final ProductFeignClient productFeignClient;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final ProductIndexService productIndexService;
    private final ObjectMapper objectMapper;

    public AiGuideService(ConversationService conversationService,
                          DeepSeekService deepSeekService,
                          ProductFeignClient productFeignClient,
                          EmbeddingService embeddingService,
                          VectorStore vectorStore,
                          ProductIndexService productIndexService,
                          ObjectMapper objectMapper) {
        this.conversationService = conversationService;
        this.deepSeekService = deepSeekService;
        this.productFeignClient = productFeignClient;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.productIndexService = productIndexService;
        this.objectMapper = objectMapper;
    }

    // ==================== 非流式聊天 ====================

    /**
     * 非流式 AI 导购（完整 RAG 流程：混合检索 + LLM 生成）
     */
    public Map<String, Object> chat(Long userId, Long conversationId, String message) {
        Conversation conv = getOrCreateConversation(userId, conversationId);

        // 保存用户消息
        ConversationMessage userMsg = new ConversationMessage();
        userMsg.setConversationId(conv.getId());
        userMsg.setRole("user");
        userMsg.setContent(message);
        userMsg.setContentType("text");
        conversationService.saveMessage(userMsg);

        // ===== RAG 检索阶段 =====
        // 双路混合检索：向量语义检索 + 关键词精确检索
        List<Map<String, Object>> recommendations = hybridSearch(message, 10);
        String productContext = formatProductContext(recommendations);
        List<Long> productIds = extractProductIds(recommendations);

        // ===== LLM 生成阶段 =====
        List<DeepSeekService.Message> messages = buildMessageHistory(conv.getId());
        String reply = deepSeekService.callNonStreaming(messages, productContext);

        // 保存助手回复
        saveAssistantMessage(conv.getId(), reply, productIds);

        // 自动生成标题
        if ("新对话".equals(conv.getTitle())) {
            generateTitle(conv.getId(), message);
        }

        // 组装返回
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conversationId", conv.getId());
        result.put("reply", reply);
        result.put("productIds", productIds);
        result.put("recommendations", recommendations.size() > 5 ? recommendations.subList(0, 5) : recommendations);
        result.put("knowledgeRefs", Collections.emptyList());
        return result;
    }

    // ==================== 流式 SSE 聊天 ====================

    /**
     * 流式 AI 导购（SSE，完整 RAG 流程）
     */
    public SseEmitter chatStream(Long userId, Long conversationId, String message) {
        SseEmitter emitter = new SseEmitter(300_000L);

        Conversation conv = getOrCreateConversation(userId, conversationId);
        Long convId = conv.getId();

        // 保存用户消息
        ConversationMessage userMsg = new ConversationMessage();
        userMsg.setConversationId(convId);
        userMsg.setRole("user");
        userMsg.setContent(message);
        userMsg.setContentType("text");
        conversationService.saveMessage(userMsg);

        // 构建消息历史
        List<DeepSeekService.Message> messages = buildMessageHistory(convId);

        // ===== RAG 检索阶段（在 LLM 流式调用之前完成）=====
        List<Map<String, Object>> recommendations = hybridSearch(message, 10);
        String productContext = formatProductContext(recommendations);
        List<Long> productIds = extractProductIds(recommendations);

        // ===== 异步：流式调用 LLM + SSE 推送 =====
        StringBuilder fullReply = new StringBuilder();

        CompletableFuture.runAsync(() -> {
            deepSeekService.callStreaming(messages, productContext,
                    // onText
                    text -> {
                        fullReply.append(text);
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data("{\"type\":\"text\",\"content\":" +
                                            objectMapper.writeValueAsString(text) + "}"));
                        } catch (IOException e) {
                            // emitter 已关闭
                        }
                    },
                    // onEvent
                    (type, content) -> {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data("{\"type\":\"" + type + "\",\"content\":" +
                                            objectMapper.writeValueAsString(content) + "}"));
                        } catch (IOException e) {
                            // 忽略
                        }
                    },
                    // onDone
                    () -> {
                        try {
                            saveAssistantMessage(convId, fullReply.toString(), productIds);

                            if ("新对话".equals(conv.getTitle())) {
                                generateTitle(convId, message);
                            }

                            Map<String, Object> doneData = new LinkedHashMap<>();
                            doneData.put("type", "done");
                            doneData.put("conversationId", convId);
                            doneData.put("recommendations",
                                    recommendations.size() > 5
                                            ? recommendations.subList(0, 5)
                                            : recommendations);
                            doneData.put("productIds", productIds);

                            emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data(objectMapper.writeValueAsString(doneData)));
                            emitter.complete();
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    },
                    // onError
                    error -> {
                        log.error("DeepSeek 流式调用异常: {}", error.getMessage());
                        try {
                            Map<String, Object> errData = new LinkedHashMap<>();
                            errData.put("type", "error");
                            errData.put("content", "AI 服务暂时不可用，请稍后再试");
                            emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data(objectMapper.writeValueAsString(errData)));
                            emitter.complete();
                        } catch (IOException e) {
                            emitter.completeWithError(error);
                        }
                    }
            );
        });

        return emitter;
    }

    // ==================== 健康检测 ====================

    public Map<String, Object> healthCheck() {
        boolean deepseekConfigured = deepSeekService.isConfigured();

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "UP");
        // 未配置 DEEPSEEK_API_KEY 时如实返回，避免误导为「AI 服务正常」
        status.put("deepseekStatus", deepseekConfigured ? "UP" : "NOT_CONFIGURED");
        status.put("deepseekConfigured", deepseekConfigured);
        if (!deepseekConfigured) {
            status.put("deepseekHint", "未配置 DEEPSEEK_API_KEY，AI 导购对话不可用；请设置该环境变量后重启服务");
        }
        status.put("vectorStore", vectorStore.getClass().getSimpleName());
        status.put("vectorStoreSize", vectorStore.size());
        status.put("embeddingProvider", embeddingService.getClass().getSimpleName());
        status.put("embeddingDim", embeddingService.dimension());
        status.put("lastExceptionTime", null);
        return status;
    }

    // ==================== 重新索引 ====================

    /** 手动触发全量商品重新索引 */
    public Map<String, Object> reindex() {
        long start = System.currentTimeMillis();
        productIndexService.indexAllProducts();
        long elapsed = System.currentTimeMillis() - start;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "SUCCESS");
        result.put("vectorCount", vectorStore.size());
        result.put("elapsedMs", elapsed);
        return result;
    }

    // ==================== RAG 混合检索 ====================

    /**
     * 双路混合检索：向量语义检索 + 关键词精确检索 → 融合重排
     *
     * 为什么双路？
     * - 向量检索：召回语义相似的（"健身T恤" → 搜出"运动速干衣"）
     * - 关键词检索：精确命中品牌/型号（"iPhone 15" → 精确匹配）
     * - 融合：兼顾 recall（向量）和 precision（关键词）
     */
    private List<Map<String, Object>> hybridSearch(String userMessage, int limit) {
        try {
            String keyword = extractKeywords(userMessage);

            // 1️⃣ 向量语义检索（召回 TOP-30，给重排留出空间）
            List<SearchResult> vecResults;
            if (vectorStore.size() > 0) {
                float[] queryVec = embeddingService.embed(userMessage);
                vecResults = vectorStore.search(queryVec, 30);
            } else {
                vecResults = Collections.emptyList();
            }

            // 2️⃣ 关键词精确检索（带三级兜底）
            List<Map<String, Object>> kwResults = keywordSearch(keyword, 20);

            // 3️⃣ Hybrid 融合重排
            return hybridRerank(vecResults, kwResults, limit);

        } catch (Exception e) {
            log.error("混合检索异常，降级为纯关键词搜索: {}", e.getMessage());
            return fallbackSearch(extractKeywords(userMessage), limit);
        }
    }

    /**
     * Hybrid 融合重排
     *
     * 得分公式：
     *   hybridScore = vectorScore × VECTOR_WEIGHT + keywordBoost × KEYWORD_WEIGHT
     *
     * 双命中（向量+关键词同时命中）的商品优先排在前面，
     * 只有向量命中的次之，只有关键词命中的排在最后。
     */
    private List<Map<String, Object>> hybridRerank(
            List<SearchResult> vecResults,
            List<Map<String, Object>> kwResults,
            int limit) {

        // 构建关键词命中集合
        Set<Long> kwIds = kwResults.stream()
                .map(p -> ((Number) p.get("id")).longValue())
                .collect(Collectors.toSet());

        // 构建关键词结果索引（id → product）
        Map<Long, Map<String, Object>> kwMap = kwResults.stream()
                .collect(Collectors.toMap(
                        p -> ((Number) p.get("id")).longValue(),
                        p -> p,
                        (a, b) -> a));

        // 用 LinkedHashMap 保持插入顺序，后续再排序
        Map<Long, ScoredProduct> merged = new LinkedHashMap<>();

        // 添加向量结果
        for (SearchResult sr : vecResults) {
            Long id = Long.parseLong(sr.id());
            double score = sr.score() * VECTOR_WEIGHT;
            merged.put(id, new ScoredProduct(id, score, sr.payload()));
        }

        // 添加关键词结果并计算 boost
        for (Long id : kwIds) {
            if (merged.containsKey(id)) {
                // 双命中 → 加 keyword boost
                ScoredProduct sp = merged.get(id);
                sp.score += KEYWORD_WEIGHT;
            } else {
                // 仅关键词命中 → 用 keyword weight 作为基础分
                Map<String, Object> product = kwMap.get(id);
                if (product != null) {
                    merged.put(id, new ScoredProduct(id, KEYWORD_WEIGHT, product));
                }
            }
        }

        // 按得分降序排列，取 TOP-N
        return merged.values().stream()
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(limit)
                .map(sp -> {
                    // 为排序分添加 debug 信息（可选，不污染业务数据）
                    sp.product.put("_hybridScore", sp.score);
                    return sp.product;
                })
                .collect(Collectors.toList());
    }

    /**
     * 关键词检索（带三级兜底策略）
     *
     * 第一轮：完整关键词搜索
     * 第二轮：按空格拆分后逐词搜索
     * 第三轮：2字子串拆分搜索
     */
    private List<Map<String, Object>> keywordSearch(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) return Collections.emptyList();

        // 第一轮：完整关键词
        var result = doProductSearch(keyword, limit);
        if (!result.isEmpty()) return result;

        // 第二轮：逐词搜索（含单字词，如"喝"、"茶"等可匹配商品关键词字段）
        if (keyword.contains(" ")) {
            for (String word : keyword.split("\\s+")) {
                if (word.isBlank()) continue;
                var fb = doProductSearch(word, limit);
                if (!fb.isEmpty()) return fb;
            }
        }

        // 第三轮：2字子串
        if (keyword.length() >= 3) {
            for (int i = 0; i <= keyword.length() - 2; i++) {
                String sub = keyword.substring(i, i + 2);
                if (sub.isBlank()) continue;
                var fb = doProductSearch(sub, limit);
                if (!fb.isEmpty()) return fb;
            }
        }

        // 第四轮：原始消息降级（去掉已处理的助词后直接搜索）
        if (keyword.length() >= 2) {
            var fb = doProductSearch(keyword.replaceAll("\\s+", ""), limit);
            if (!fb.isEmpty()) return fb;
        }

        return Collections.emptyList();
    }

    /**
     * 降级方案：纯关键词搜索（向量库为空时使用）
     */
    private List<Map<String, Object>> fallbackSearch(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) return Collections.emptyList();
        return doProductSearch(keyword, limit);
    }

    /** 执行一次商品搜索 */
    private List<Map<String, Object>> doProductSearch(String keyword, int limit) {
        try {
            var result = productFeignClient.searchProducts(1, limit, null,
                    keyword, null, null, "sales", false);
            if (result != null && result.getData() != null
                    && result.getData().getRecords() != null
                    && !result.getData().getRecords().isEmpty()) {
                return result.getData().getRecords();
            }
        } catch (Exception e) {
            log.warn("商品搜索失败: keyword={} error={}", keyword, e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * 意图→搜索关键词映射表
     * 当用户使用口语化表达（如"吃的"）时，扩展出可匹配商品名的关键词
     */
    private static final Map<String, String> INTENT_KEYWORDS = new LinkedHashMap<>();
    static {
        INTENT_KEYWORDS.put("吃", "零食 食品 美味 坚果 干果 糕点点心 休闲食品 礼盒");
        INTENT_KEYWORDS.put("喝", "饮料 茶 咖啡 水 冲饮 饮品 果汁 牛奶 泡");
        INTENT_KEYWORDS.put("穿", "衣服 服饰 穿搭 外套 裤子 上衣 裙子 衬衫");
        INTENT_KEYWORDS.put("戴", "手表 手环 首饰 穿戴 饰品 配饰");
        INTENT_KEYWORDS.put("用", "家居 日用品 用品 收纳 厨房 卫浴 生活");
        INTENT_KEYWORDS.put("玩", "游戏 电子 娱乐 休闲 数码");
        INTENT_KEYWORDS.put("手机", "手机 华为 小米 iPhone 苹果 三星 荣耀");
        INTENT_KEYWORDS.put("电脑", "电脑 笔记本 键盘 鼠标 显示器 耳机");
        INTENT_KEYWORDS.put("礼物", "礼盒 礼物 礼品 送礼 送人 伴手礼 礼盒装");
        INTENT_KEYWORDS.put("送人", "礼盒 礼物 礼品 送礼 送人 伴手礼 礼盒装");
    }

    /**
     * 简单关键词提取
     * 去掉动宾结构、助词、语气词，提取商品核心关键词
     */
    private String extractKeywords(String message) {
        String cleaned = message
                .replaceAll("[？?！!，,。.、：:；;…·（）()【】\\[\\]{}「」『』\"\"''~～]", " ")
                .replaceAll("[的了吗呢吧么啊呀哦嗯哟]", " ");

        String[] stopWords = {"推荐", "介绍", "看看", "找一下", "搜一下", "查一下",
                "搜索", "查找", "查询", "有没有", "给我", "我要", "我想",
                "需要", "想要", "想看", "想买", "想找", "买一个", "买个",
                "能", "可以", "帮", "来一个", "下单", "购买"};
        for (String w : stopWords) {
            cleaned = cleaned.replace(w, " ");
        }

        cleaned = cleaned.replaceAll("[便宜实惠高端性价比超值好优质新款热销爆款]", " ");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();

        // 关键词扩展：检测口语意图词，追加可搜索的商品关键词
        String expanded = expandIntentKeywords(cleaned, message);

        if (expanded.isEmpty() || expanded.length() <= 1) {
            // 单字符关键词（如"喝"）可能直接命中商品关键词字段
            if (expanded.length() == 1 && !expanded.matches("[的是了和与及]")) {
                return expanded;
            }
            // 兜底：去掉语气助词后尝试匹配分类名或商品关键词
            String fallback = message.replaceAll("[的了吗呢吧么啊]", "").trim();
            return fallback.isEmpty() ? message : fallback;
        }
        return expanded;
    }

    /**
     * 意图关键词扩展
     * 将"吃/喝/穿"等口语意图词扩展为可匹配商品名的关键词
     */
    private String expandIntentKeywords(String cleaned, String original) {
        // 先尝试精确匹配 cleaned 中的词
        for (Map.Entry<String, String> entry : INTENT_KEYWORDS.entrySet()) {
            if (cleaned.contains(entry.getKey())) {
                return entry.getValue();
            }
            // 也检查原始消息中是否包含意图词（避免停用词过滤后丢失意图）
            if (original.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return cleaned;
    }

    // ==================== 上下文构建 ====================

    /**
     * 格式化商品列表为 LLM 可读的上下文文本
     *
     * 包含商品的关键信息：名称、价格、描述、品牌、销量、分类。
     * LLM 基于这些文本信息做出推荐决策。
     */
    private String formatProductContext(List<Map<String, Object>> products) {
        if (products == null || products.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < products.size(); i++) {
            Map<String, Object> p = products.get(i);
            String name = getStr(p, "name");
            Object price = p.get("price");
            String subtitle = getStr(p, "subtitle");
            String desc = getStr(p, "description");
            String brand = getStr(p, "brand");
            Object sales = p.get("sales");
            String categoryName = getStr(p, "categoryName");
            Object originalPrice = p.get("originalPrice");
            // 评分（如果有）
            Object rating = p.get("rating");
            // 销量
            Object salesCount = p.get("sales");

            sb.append(i + 1).append(". 【").append(name).append("】");
            if (price != null) {
                sb.append(" — ¥").append(price);
                if (originalPrice != null && ((Number) originalPrice).doubleValue() > ((Number) price).doubleValue()) {
                    sb.append(" (原价¥").append(originalPrice).append(")");
                }
            }
            sb.append("\n");

            if (!brand.isEmpty()) {
                sb.append("   品牌：").append(brand).append("\n");
            }
            if (!categoryName.isEmpty()) {
                sb.append("   类目：").append(categoryName).append("\n");
            }
            if (!subtitle.isEmpty()) {
                sb.append("   简介：").append(subtitle).append("\n");
            }
            if (!desc.isEmpty()) {
                String shortDesc = desc.length() > 100 ? desc.substring(0, 100) + "..." : desc;
                sb.append("   描述：").append(shortDesc).append("\n");
            }
            if (salesCount != null && ((Number) salesCount).intValue() > 0) {
                sb.append("   销量：已售 ").append(salesCount).append(" 件\n");
            }
            if (rating != null) {
                sb.append("   评分：").append(rating).append("\n");
            }

            sb.append("\n");
        }
        return sb.toString();
    }

    // ==================== 辅助方法 ====================

    private static class ScoredProduct {
        Long id;
        double score;
        Map<String, Object> product;

        ScoredProduct(Long id, double score, Map<String, Object> product) {
            this.id = id;
            this.score = score;
            this.product = product;
        }
    }

    private Conversation getOrCreateConversation(Long userId, Long conversationId) {
        if (conversationId != null && conversationId > 0) {
            Conversation conv = conversationService.getById(conversationId);
            if (conv != null && conv.getUserId().equals(userId)) {
                return conv;
            }
        }
        return conversationService.createConversation(userId);
    }

    private List<DeepSeekService.Message> buildMessageHistory(Long conversationId) {
        List<ConversationMessage> history = conversationService.getMessageHistory(conversationId);
        return history.stream()
                .map(msg -> new DeepSeekService.Message(msg.getRole(), msg.getContent()))
                .collect(Collectors.toList());
    }

    private void saveAssistantMessage(Long conversationId, String content, List<Long> productIds) {
        ConversationMessage msg = new ConversationMessage();
        msg.setConversationId(conversationId);
        msg.setRole("assistant");
        msg.setContent(content);
        msg.setContentType("text");
        msg.setRelatedProductIds(productIds.isEmpty() ? null :
                productIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        conversationService.saveMessage(msg);
    }

    /** 提取商品 ID 列表 */
    private List<Long> extractProductIds(List<Map<String, Object>> products) {
        return products.stream()
                .map(p -> ((Number) p.get("id")).longValue())
                .collect(Collectors.toList());
    }

    private void generateTitle(Long conversationId, String firstMessage) {
        try {
            String title = firstMessage.length() > 30
                    ? firstMessage.substring(0, 30) + "..."
                    : firstMessage;
            conversationService.updateTitle(conversationId, title);
        } catch (Exception e) {
            log.warn("生成标题失败: {}", e.getMessage());
        }
    }

    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val == null ? "" : val.toString().trim();
    }
}
