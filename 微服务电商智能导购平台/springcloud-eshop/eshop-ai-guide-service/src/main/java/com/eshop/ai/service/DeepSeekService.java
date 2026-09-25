package com.eshop.ai.service;

import com.eshop.ai.config.DeepSeekConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * DeepSeek API 调用服务
 *
 * 支持非流式（RestTemplate）和流式 SSE（Java HttpClient）两种模式。
 */
@Service
public class DeepSeekService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekService.class);

    private final DeepSeekConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public DeepSeekService(DeepSeekConfig config, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.config = config;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(15))
                .build();
    }

    /**
     * 是否已配置 API Key。
     * 未配置时应用仍可启动，但对话功能不可用，健康检测据此返回 NOT_CONFIGURED。
     */
    public boolean isConfigured() {
        return config.getApiKey() != null && !config.getApiKey().isBlank();
    }

    /**
     * 对话消息
     */
    public static class Message {
        private String role;
        private String content;

        public Message() {}

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    // ========== 非流式调用 ==========

    /**
     * 非流式调用 DeepSeek API，返回完整回复文本
     */
    public String callNonStreaming(List<Message> messages) {
        return callNonStreaming(messages, null);
    }

    /**
     * 非流式调用 DeepSeek API，带商品上下文（RAG）
     */
    public String callNonStreaming(List<Message> messages, String productContext) {
        try {
            String requestBody = buildRequestBody(messages, false, productContext);
            String url = config.getBaseUrl() + "/chat/completions";

            var entity = new org.springframework.http.HttpEntity<>(
                    requestBody,
                    buildHeaders()
            );

            var response = restTemplate.postForEntity(url, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText("");
            log.info("DeepSeek 非流式调用成功，回复长度={}", content.length());
            return content;
        } catch (Exception e) {
            log.error("DeepSeek 非流式调用失败: {}", e.getMessage());
            throw new RuntimeException("AI 服务调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 非流式调用，返回完整 JSON 响应（含 token 用量等）
     */
    public JsonNode callNonStreamingRaw(List<Message> messages) {
        return callNonStreamingRaw(messages, null);
    }

    /**
     * 非流式调用，返回完整 JSON 响应，带商品上下文（RAG）
     */
    public JsonNode callNonStreamingRaw(List<Message> messages, String productContext) {
        try {
            String requestBody = buildRequestBody(messages, false, productContext);
            String url = config.getBaseUrl() + "/chat/completions";

            var entity = new org.springframework.http.HttpEntity<>(requestBody, buildHeaders());
            var response = restTemplate.postForEntity(url, entity, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            log.error("DeepSeek 非流式调用失败: {}", e.getMessage());
            throw new RuntimeException("AI 服务调用失败: " + e.getMessage(), e);
        }
    }

    // ========== 流式 SSE 调用（转发到 SseEmitter） ==========

    /**
     * 流式调用 DeepSeek API，通过回调处理每个 SSE 事件
     */
    public void callStreaming(List<Message> messages,
                              Consumer<String> onText,
                              BiConsumer<String, String> onEvent,
                              Runnable onDone,
                              Consumer<Exception> onError) {
        callStreaming(messages, null, onText, onEvent, onDone, onError);
    }

    /**
     * 流式调用 DeepSeek API，带商品上下文（RAG）
     */
    public void callStreaming(List<Message> messages,
                              String productContext,
                              Consumer<String> onText,
                              BiConsumer<String, String> onEvent,
                              Runnable onDone,
                              Consumer<Exception> onError) {
        try {
            String requestBody = buildRequestBody(messages, true, productContext);
            String url = config.getBaseUrl() + "/chat/completions";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                    .thenAccept(response -> {
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                            String line;
                            StringBuilder fullText = new StringBuilder();

                            while ((line = reader.readLine()) != null) {
                                if (line.startsWith("data: ")) {
                                    String data = line.substring(6).trim();
                                    if ("[DONE]".equals(data)) {
                                        break;
                                    }
                                    try {
                                        JsonNode json = objectMapper.readTree(data);
                                        JsonNode choices = json.path("choices");
                                        if (choices.isArray() && choices.size() > 0) {
                                            JsonNode delta = choices.get(0).path("delta");
                                            String content = delta.path("content").asText("");
                                            if (!content.isEmpty()) {
                                                fullText.append(content);
                                                onText.accept(content);
                                            }
                                            // 如果 delta 中有 reasoning_content（DeepSeek-R1 等推理模型）
                                            String reasoning = delta.path("reasoning_content").asText("");
                                            if (!reasoning.isEmpty()) {
                                                onEvent.accept("reasoning", reasoning);
                                            }
                                        }
                                    } catch (Exception e) {
                                        log.warn("解析 DeepSeek SSE 消息失败: {}", e.getMessage());
                                    }
                                }
                            }
                            onDone.run();
                        } catch (Exception e) {
                            onError.accept(e);
                        }
                    })
                    .exceptionally(e -> {
                        onError.accept(new Exception("DeepSeek 流式调用失败: " + e.getMessage()));
                        return null;
                    });

        } catch (Exception e) {
            onError.accept(e);
        }
    }

    /**
     * 流式调用 DeepSeek API 并直接写入 SseEmitter
     */
    public void callStreamingToEmitter(List<Message> messages, SseEmitter emitter) {
        callStreaming(messages,
                text -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data("{\"type\":\"text\",\"content\":" +
                                        objectMapper.writeValueAsString(text) + "}"));
                    } catch (Exception e) {
                        // emitter 已关闭
                    }
                },
                (type, content) -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data("{\"type\":\"" + type + "\",\"content\":" +
                                        objectMapper.writeValueAsString(content) + "}"));
                    } catch (Exception e) {
                        // emitter 已关闭
                    }
                },
                () -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data("{\"type\":\"done\",\"conversationId\":0}"));
                        emitter.complete();
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                },
                error -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data("{\"type\":\"error\",\"content\":\"" +
                                        error.getMessage().replace("\"", "\\\"") + "\"}"));
                        emitter.complete();
                    } catch (Exception e) {
                        emitter.completeWithError(error);
                    }
                }
        );
    }

    // ========== 辅助方法 ==========

    private org.springframework.http.HttpHeaders buildHeaders() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "Bearer " + config.getApiKey());
        return headers;
    }

    private String buildRequestBody(List<Message> messages, boolean stream, String productContext) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", config.getModel());
            root.put("stream", stream);
            root.put("max_tokens", 2048);
            root.put("temperature", 0.7);

            // 添加系统提示词
            ArrayNode msgArray = objectMapper.createArrayNode();
            msgArray.add(objectMapper.createObjectNode()
                    .put("role", "system")
                    .put("content", buildSystemPrompt(productContext)));

            for (Message msg : messages) {
                msgArray.add(objectMapper.createObjectNode()
                        .put("role", msg.getRole())
                        .put("content", msg.getContent()));
            }
            root.set("messages", msgArray);

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("构建请求体失败", e);
        }
    }

    /**
     * 构建系统提示词（AI 导购角色设定）
     * @param productContext 商品上下文（RAG 检索结果），可为 null
     */
    private String buildSystemPrompt(String productContext) {
        String prompt = "你是「智能导购助手」，一个专业、热情、有耐心的电商导购 AI。\n\n" +
                "## 核心能力\n" +
                "1. **商品推荐**：根据用户需求推荐合适的商品，说明推荐理由\n" +
                "2. **购物建议**：提供穿搭建议、商品对比等专业导购服务\n" +
                "3. **售前咨询**：回答关于商品功能、材质、适用场景等问题\n\n" +
                "## 回答风格\n" +
                "- 语气亲切自然，使用中文交流\n" +
                "- 推荐商品时给出具体理由（材质、性价比、适用场景）\n" +
                "- 如果用户需求不明确，主动询问更多偏好\n" +
                "- 不确定的信息不要编造，坦诚告知用户\n\n" +
                "## 商品推荐格式\n" +
                "推荐商品时请用编号列表，格式如：\n" +
                "1️⃣ **商品名称** — ¥价格\n" +
                "   ✅ 推荐理由\n\n" +
                "## 重要限制\n" +
                "- 只推荐以下「当前商城可售商品」列表中的商品，绝不推荐列表之外的商品\n" +
                "- 如果列表中没有用户需要的商品，礼貌告知暂无此商品，并推荐最接近的替代品\n" +
                "- 不回答与购物无关的问题，礼貌引导回购物话题\n" +
                "- 不提供医疗、投资等专业建议";

        // RAG：如果有商品上下文，注入到系统提示词中
        if (productContext != null && !productContext.isEmpty()) {
            prompt += "\n\n## 当前商城可售商品\n" +
                    "以下是你当前可以推荐的真实商品数据。请严格基于这些商品回答用户问题，" +
                    "不要推荐列表中不存在的商品：\n\n" + productContext;
        } else {
            prompt += "\n\n## 当前商城可售商品\n" +
                    "⚠️ 用户搜索的商品没有匹配结果。请这样回应：\n" +
                    "1. 礼貌告知目前没有该品类商品\n" +
                    "2. 主动介绍商城现有的商品大类：服装（男装/女装）、电子产品（手机/耳机/电脑）、家居用品、运动户外等\n" +
                    "3. 引导用户浏览其他品类，例如「要不您看看我们最新款的 XX？」\n" +
                    "注意：不要只说「没有」就结束，要推荐替代品类！";
        }

        return prompt;
    }
}
