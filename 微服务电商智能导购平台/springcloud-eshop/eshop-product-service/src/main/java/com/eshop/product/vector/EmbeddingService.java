package com.eshop.product.vector;

import java.util.List;

public interface EmbeddingService {
    float[] embed(String text);
    float[][] embedBatch(List<String> texts);
    int dimension();
}
