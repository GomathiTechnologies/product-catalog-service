package com.example.productcatalogservice;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class InMemoryVectorStore {
    private volatile List<VectorDocument> documents = List.of();

    public synchronized void replaceAll(List<VectorDocument> replacements) {
        documents = List.copyOf(replacements);
    }

    public List<VectorDocument> getAll() { return documents; }
    public Optional<VectorDocument> getById(String id) {
        return documents.stream().filter(document -> document.id().equals(id)).findFirst();
    }
    public int count() { return documents.size(); }
    public synchronized int clear() {
        int count = documents.size();
        documents = List.of();
        return count;
    }

    public List<SearchResult> search(List<VectorDocument> snapshot, float[] query, int topK, Double threshold) {
        return snapshot.stream().map(document -> new SearchResult(document.id(), document.text(), document.metadata(),
                        cosine(query, document.embedding())))
                .filter(result -> threshold == null || result.score() >= threshold)
                .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
                .limit(topK).toList();
    }

    private static double cosine(float[] a, float[] b) {
        if (a.length == 0 || a.length != b.length) {
            throw new IllegalArgumentException("Embedding dimensions do not match");
        }
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            if (!Float.isFinite(a[i]) || !Float.isFinite(b[i])) throw new IllegalArgumentException("Embedding contains non-finite values");
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0 || normB == 0) throw new IllegalArgumentException("Cannot compare zero embeddings");
        return Math.max(-1, Math.min(1, dot / Math.sqrt(normA * normB)));
    }
}
