package com.eshop.ai.vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存向量存储 — 基于 ConcurrentHashMap + 暴力余弦相似度搜索
 *
 * 适用于商品数量 < 10 万的中小规模场景。
 * 线程安全，支持并发读写。
 * 如需更大规模或持久化，可替换为 Redis Stack / Milvus 实现。
 */
@Component
public class InMemoryVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryVectorStore.class);

    private final Map<String, VectorEntry> store = new ConcurrentHashMap<>();

    private record VectorEntry(float[] vector, Map<String, Object> payload) {}

    @Override
    public void add(String id, float[] vector, Map<String, Object> payload) {
        store.put(id, new VectorEntry(vector, payload));
    }

    @Override
    public List<SearchResult> search(float[] queryVector, int topK) {
        if (store.isEmpty()) {
            return Collections.emptyList();
        }

        // 并行计算所有向量的余弦相似度
        return store.entrySet().parallelStream()
                .map(e -> {
                    double score = cosineSimilarity(queryVector, e.getValue().vector());
                    return new SearchResult(e.getKey(), score, e.getValue().payload());
                })
                .filter(r -> r.score() > 0.01) // 过滤掉完全不相关的
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(Math.max(1, topK))
                .toList();
    }

    @Override
    public void remove(String id) {
        store.remove(id);
    }

    @Override
    public void clear() {
        store.clear();
    }

    @Override
    public int size() {
        return store.size();
    }

    /**
     * 计算两个向量的余弦相似度
     */
    public static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) {
            return 0.0;
        }
        double dotProduct = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        return denominator < 1e-10 ? 0.0 : dotProduct / denominator;
    }
}
