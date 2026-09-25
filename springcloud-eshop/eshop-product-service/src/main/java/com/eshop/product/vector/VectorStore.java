package com.eshop.product.vector;

import java.util.List;
import java.util.Map;

public interface VectorStore {
    void add(String id, float[] vector, Map<String, Object> payload);
    List<SearchResult> search(float[] queryVector, int topK);
    void remove(String id);
    void clear();
    int size();
}
