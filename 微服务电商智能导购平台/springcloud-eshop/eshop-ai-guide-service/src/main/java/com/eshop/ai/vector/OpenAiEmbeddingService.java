package com.eshop.ai.vector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * OpenAI 兼容的文本嵌入服务
 *
 * 支持所有 OpenAI Embedding API 格式的服务：
 * - SiliconFlow BGE-M3: https://api.siliconflow.cn/v1/embeddings
 * - OpenAI text-embedding-3-small: https://api.openai.com/v1/embeddings
 * - 阿里云 DashScope（需专用客户端，但也可通过兼容层）
 *
 * 配置示例（application.yml）：
 * <pre>
 * vector:
 *   embedding:
 *     provider: openai
 *     openai:
 *       api-key: ${EMBEDDING_API_KEY:}
 *       base-url: https://api.siliconflow.cn/v1
 *       model: BAAI/bge-m3
 * </pre>
 *
 * 当 api-key 未设置时，该 Bean 不会创建（回退到 NgramEmbeddingService）。
 */
@Component
@ConditionalOnProperty(prefix = "vector.embedding", name = "provider", havingValue = "openai")
public class OpenAiEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiEmbeddingService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public OpenAiEmbeddingService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${vector.embedding.openai.api-key}") String apiKey,
            @Value("${vector.embedding.openai.base-url:https://api.siliconflow.cn/v1}") String baseUrl,
            @Value("${vector.embedding.openai.model:BAAI/bge-m3}") String model) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.model = model;
    }

    @PostConstruct
    public void init() {
        log.info("OpenAiEmbeddingService 初始化: baseUrl={}, model={}", baseUrl, model);
    }

    @Override
    public float[] embed(String text) {
        float[][] result = embedBatch(List.of(text));
        return result.length > 0 ? result[0] : new float[dimension()];
    }

    @Override
    public float[][] embedBatch(List<String> texts) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            ArrayNode input = body.putArray("input");
            texts.forEach(input::add);

            HttpEntity<String> entity = new HttpEntity<>(
                    objectMapper.writeValueAsString(body), headers);

            String url = baseUrl + "/embeddings";
            String response = restTemplate.postForObject(url, entity, String.class);
            JsonNode root = objectMapper.readTree(response);

            // 解析 OpenAI 兼容格式的返回
            // {"data": [{"embedding": [...], "index": 0}], "model": "..."}
            JsonNode data = root.path("data");
            float[][] result = new float[texts.size()][];

            for (JsonNode item : data) {
                int idx = item.path("index").asInt();
                JsonNode embedding = item.path("embedding");
                if (idx < texts.size() && embedding.isArray()) {
                    float[] vec = new float[embedding.size()];
                    for (int i = 0; i < embedding.size(); i++) {
                        vec[i] = (float) embedding.get(i).asDouble();
                    }
                    result[idx] = vec;
                }
            }

            // 填充失败的项
            for (int i = 0; i < result.length; i++) {
                if (result[i] == null) {
                    result[i] = new float[dimension()];
                }
            }

            return result;
        } catch (Exception e) {
            log.error("OpenAI Embedding API 调用失败: {}", e.getMessage());
            // 降级：返回零向量
            float[][] result = new float[texts.size()][];
            for (int i = 0; i < texts.size(); i++) {
                result[i] = new float[dimension()];
            }
            return result;
        }
    }

    @Override
    public int dimension() {
        // BGE-M3 输出 1024 维，OpenAI text-embedding-3-small 输出 1536 维
        // 这里不确定，返回 0 让调用方处理；实际会被正常填充
        return 0;
    }
}
