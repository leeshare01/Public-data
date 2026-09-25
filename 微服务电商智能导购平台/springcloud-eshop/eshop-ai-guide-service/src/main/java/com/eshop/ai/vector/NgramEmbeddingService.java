package com.eshop.ai.vector;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于中文 n-gram 哈希的文本嵌入服务
 *
 * 将文本转为字符级别的 bigram（2-gram）和 trigram（3-gram），
 * 通过哈希技巧映射到固定维度向量。
 *
 * 为什么对中文有效？
 * - 中文每个字都有语义含义
 * - Bigram 能捕捉常见词汇边界（如"手机"、"电脑"、"T恤"）
 * - Trigram 能捕捉多字短语（如"蓝牙耳机"、"连衣裙"）
 *
 * 优点：无需外部 API，无需下载模型，完全本地运行，速度极快。
 * 缺点：效果不如 BERT/Transformer 类模型，但已显著优于纯关键词匹配。
 *
 * 向量维度 768，兼容大多数向量数据库的常用维度。
 */
@Component
public class NgramEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(NgramEmbeddingService.class);

    /** 向量维度（默认 768，与 BGE-small 一致） */
    private final int dimension;

    /** 是否启用 2-gram（双字词） */
    private final boolean useBigram;

    /** 是否启用 3-gram（三字词） */
    private final boolean useTrigram;

    public NgramEmbeddingService(
            @Value("${vector.embedding.dim:768}") int dimension,
            @Value("${vector.embedding.ngram.bigram:true}") boolean useBigram,
            @Value("${vector.embedding.ngram.trigram:true}") boolean useTrigram) {
        this.dimension = dimension > 0 ? dimension : 768;
        this.useBigram = useBigram;
        this.useTrigram = useTrigram;
    }

    @PostConstruct
    public void init() {
        log.info("NgramEmbeddingService 初始化: dim={}, bigram={}, trigram={}",
                dimension, useBigram, useTrigram);
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return new float[dimension];
        }

        // 预处理：统一小写、去除多余空白
        String normalized = text.toLowerCase().trim();
        float[] vector = new float[dimension];

        // 提取字符 n-gram 并哈希到向量空间
        if (useBigram && normalized.length() >= 2) {
            for (int i = 0; i < normalized.length() - 1; i++) {
                addNgram(vector, normalized.substring(i, i + 2));
            }
        }

        if (useTrigram && normalized.length() >= 3) {
            for (int i = 0; i < normalized.length() - 2; i++) {
                addNgram(vector, normalized.substring(i, i + 3));
            }
        }

        // L2 归一化
        l2Normalize(vector);

        return vector;
    }

    @Override
    public float[][] embedBatch(List<String> texts) {
        float[][] result = new float[texts.size()][];
        for (int i = 0; i < texts.size(); i++) {
            result[i] = embed(texts.get(i));
        }
        return result;
    }

    @Override
    public int dimension() {
        return dimension;
    }

    /**
     * 将单个 n-gram 通过哈希技巧加到向量中
     * 使用 hashCode 同时决定位置和方向，减少哈希碰撞影响
     */
    private void addNgram(float[] vector, String ngram) {
        int hash = ngram.hashCode();
        // 使用 Math.floorMod 确保非负索引
        int idx = Math.floorMod(hash, dimension);
        // 符号哈希：用 hash 的某位决定 +1 还是 -1，使哈希值更均匀分布
        vector[idx] += (hash & 1) == 0 ? 1.0f : -1.0f;
    }

    /** L2 归一化 */
    private void l2Normalize(float[] vector) {
        double norm = 0.0;
        for (float v : vector) {
            norm += (double) v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 1e-10) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) (vector[i] / norm);
            }
        }
    }
}
