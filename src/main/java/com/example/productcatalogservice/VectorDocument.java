package com.example.productcatalogservice;

import java.util.Map;

public record VectorDocument(String id, String text, Map<String, Object> metadata, float[] embedding) {
    public VectorDocument {
        metadata = Map.copyOf(metadata);
        embedding = embedding.clone();
    }

    @Override
    public float[] embedding() { return embedding.clone(); }
}
