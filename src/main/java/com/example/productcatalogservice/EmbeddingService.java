package com.example.productcatalogservice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {
    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private final EmbeddingModel embeddingModel;
    private final PdfReader pdfReader;
    private final KnowledgeVectorStore store;

    public EmbeddingService(EmbeddingModel embeddingModel, PdfReader pdfReader, KnowledgeVectorStore store) {
        this.embeddingModel = embeddingModel;
        this.pdfReader = pdfReader;
        this.store = store;
    }

    public float[] generateEmbedding(String text) {
        float[] vector = embeddingModel.embed(text);
        log.info("Generated embedding with {} dimensions", vector.length);
        return vector;
    }

    public IngestionResult ingest() throws IOException {
        long start = System.nanoTime();
        List<Document> chunks = pdfReader.readChunks();
        List<VectorDocument> vectors = new ArrayList<>();
        int dimensions = 0;
        for (Document chunk : chunks) {
            float[] vector = generateEmbedding(chunk.getText());
            if (vector.length == 0 || (dimensions != 0 && dimensions != vector.length)) {
                throw new IllegalStateException("Inconsistent embedding dimensions");
            }
            dimensions = vector.length;
            vectors.add(new VectorDocument(chunk.getId(), chunk.getText(), chunk.getMetadata(), vector));
            log.info("Embedded document {} from {} with {} dimensions", chunk.getId(), chunk.getMetadata().get("file_name"), dimensions);
        }
        store.replaceAll(vectors);
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        log.info("Ingested {} chunks in {} ms", vectors.size(), elapsed);
        return new IngestionResult(vectors.size(), dimensions, elapsed);
    }

    public SearchResponse search(String query, int topK, Double threshold) {
        long start = System.nanoTime();
        int count = store.count();
        List<SearchResult> results = count == 0 ? List.of() : store.search(generateEmbedding(query), topK, threshold);
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        log.info("Search checked {} stored vectors and returned {} matches in {} ms", count, results.size(), elapsed);
        return new SearchResponse(query, count, results.size(), elapsed, results);
    }

    public record IngestionResult(int documents, int dimensions, long executionTimeMs) {}
    public record SearchResponse(String query, int totalDocuments, int matchedDocuments, long executionTimeMs, List<SearchResult> results) {}
}
