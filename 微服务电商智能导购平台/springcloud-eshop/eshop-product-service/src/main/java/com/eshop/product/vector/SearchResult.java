package com.eshop.product.vector;

import java.util.Map;

public record SearchResult(String id, double score, Map<String, Object> payload) {
}
