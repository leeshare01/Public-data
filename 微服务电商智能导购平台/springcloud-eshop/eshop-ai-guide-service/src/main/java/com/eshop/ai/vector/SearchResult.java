package com.eshop.ai.vector;

import java.util.Map;

/**
 * 向量搜索结果
 *
 * @param id     唯一标识
 * @param score  相似度得分（0~1，越大越相似）
 * @param payload 附加数据
 */
public record SearchResult(String id, double score, Map<String, Object> payload) {
}
