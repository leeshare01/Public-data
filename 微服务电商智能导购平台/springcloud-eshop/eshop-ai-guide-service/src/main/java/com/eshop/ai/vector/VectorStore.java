package com.eshop.ai.vector;

import java.util.List;
import java.util.Map;

/**
 * 向量存储接口
 *
 * 支持多种后端实现：InMemory、Redis Stack、Milvus 等。
 * 当前默认使用 InMemoryVectorStore。
 */
public interface VectorStore {

    /**
     * 添加向量到存储
     * @param id 唯一标识（如商品ID）
     * @param vector 向量
     * @param payload 附加数据（如商品信息）
     */
    void add(String id, float[] vector, Map<String, Object> payload);

    /**
     * 搜索最相似的 TOP-K 向量
     * @param queryVector 查询向量
     * @param topK 返回数量
     * @return 按相似度降序排列的结果
     */
    List<SearchResult> search(float[] queryVector, int topK);

    /**
     * 删除向量
     * @param id 唯一标识
     */
    void remove(String id);

    /** 清空所有向量 */
    void clear();

    /** 当前存储的向量数量 */
    int size();
}
