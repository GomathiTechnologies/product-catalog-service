package com.example.productcatalogservice;

import java.util.List;
import java.util.Optional;

public interface KnowledgeVectorStore {
    void replaceAll(List<VectorDocument> documents);
    List<VectorDocument> getAll();
    Optional<VectorDocument> getById(String id);
    int count();
    int clear();
    List<SearchResult> search(float[] query, int topK, Double threshold);
}
