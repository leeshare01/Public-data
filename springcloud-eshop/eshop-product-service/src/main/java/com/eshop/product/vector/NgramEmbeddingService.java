package com.eshop.product.vector;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NgramEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(NgramEmbeddingService.class);

    private final int dimension;

    public NgramEmbeddingService(@Value("${product.vector.dim:768}") int dimension) {
        this.dimension = dimension > 0 ? dimension : 768;
    }

    @PostConstruct
    public void init() {
        log.info("Product NgramEmbeddingService: dim={}", dimension);
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) return new float[dimension];
        String normalized = text.toLowerCase().trim();
        float[] vector = new float[dimension];

        for (int i = 0; i < normalized.length() - 1; i++)
            addNgram(vector, normalized.substring(i, i + 2));
        for (int i = 0; i < normalized.length() - 2; i++)
            addNgram(vector, normalized.substring(i, i + 3));

        double norm = 0;
        for (float v : vector) norm += (double) v * v;
        norm = Math.sqrt(norm);
        if (norm > 1e-10)
            for (int i = 0; i < vector.length; i++) vector[i] /= norm;
        return vector;
    }

    @Override
    public float[][] embedBatch(List<String> texts) {
        float[][] result = new float[texts.size()][];
        for (int i = 0; i < texts.size(); i++) result[i] = embed(texts.get(i));
        return result;
    }

    @Override
    public int dimension() { return dimension; }

    private void addNgram(float[] vector, String ngram) {
        int hash = ngram.hashCode();
        int idx = Math.floorMod(hash, dimension);
        vector[idx] += (hash & 1) == 0 ? 1.0f : -1.0f;
    }
}
