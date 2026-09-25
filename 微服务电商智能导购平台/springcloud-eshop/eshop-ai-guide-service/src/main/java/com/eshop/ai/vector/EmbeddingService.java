package com.eshop.ai.vector;

import java.util.List;

/**
 * 文本嵌入服务接口
 *
 * 支持多种嵌入实现：
 * - NgramEmbeddingService：基于中文 n-gram 哈希，无需外部 API，开箱即用
 * - OpenAiEmbeddingService：兼容 OpenAI Embedding API 格式（也支持 SiliconFlow BGE 等）
 */
public interface EmbeddingService {

    /**
     * 将文本转为向量
     * @param text 输入文本
     * @return 向量（float 数组）
     */
    float[] embed(String text);

    /**
     * 批量嵌入
     * @param texts 文本列表
     * @return 向量数组
     */
    float[][] embedBatch(List<String> texts);

    /**
     * 获取向量维度
     */
    int dimension();
}
