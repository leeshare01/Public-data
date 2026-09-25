package com.eshop.product.vector;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InMemoryVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryVectorStore.class);

    private final java.util.Map<String, VectorEntry> store = new java.util.concurrent.ConcurrentHashMap<>();

    private record VectorEntry(float[] vector, java.util.Map<String, Object> payload) {}

    @PostConstruct
    public void init() {
        log.info("InMemoryVectorStore 初始化");
    }

    @Override
    public void add(String id, float[] vector, java.util.Map<String, Object> payload) {
        store.put(id, new VectorEntry(vector, payload));
    }

    @Override
    public java.util.List<SearchResult> search(float[] queryVector, int topK) {
        if (store.isEmpty()) return java.util.Collections.emptyList();

        return store.entrySet().parallelStream()
                .map(e -> {
                    double score = cosineSimilarity(queryVector, e.getValue().vector());
                    return new SearchResult(e.getKey(), score, e.getValue().payload());
                })
                .filter(r -> r.score() > 0.01)
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(Math.max(1, topK))
                .toList();
    }

    @Override public void remove(String id) { store.remove(id); }
    @Override public void clear() { store.clear(); }
    @Override public int size() { return store.size(); }

    public static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) return 0.0;
        double dot = 0, nA = 0, nB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            nA += (double) a[i] * a[i];
            nB += (double) b[i] * b[i];
        }
        return Math.sqrt(nA) * Math.sqrt(nB) < 1e-10 ? 0.0 : dot / (Math.sqrt(nA) * Math.sqrt(nB));
    }
}
