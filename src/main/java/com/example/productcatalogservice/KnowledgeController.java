package com.example.productcatalogservice;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/ai/knowledge")
public class KnowledgeController {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeController.class);
    private final EmbeddingService service;
    private final KnowledgeVectorStore store;

    public KnowledgeController(EmbeddingService service, KnowledgeVectorStore store) {
        this.service = service;
        this.store = store;
    }

    @PostMapping("/ingest")
    public IngestResponse ingest() {
        try {
            var result = service.ingest();
            return new IngestResponse("SUCCESS", result.documents(), result.dimensions(), result.executionTimeMs());
        } catch (IOException exception) {
            log.warn("Failed to read knowledge base: {}", exception.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to read knowledge base");
        } catch (DataAccessException exception) {
            log.warn("Knowledge database unavailable: {}", exception.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Knowledge database unavailable; previous documents retained");
        } catch (RuntimeException exception) {
            log.warn("Knowledge ingestion failed: {}", exception.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to generate embeddings; previous documents retained");
        }
    }

    @GetMapping("/count")
    public Map<String, Integer> count() { return Map.of("documents", store.count()); }

    @GetMapping("/documents")
    public List<DocumentView> documents(@RequestParam(defaultValue = "false") boolean includeEmbedding) {
        return store.getAll().stream().map(doc -> view(doc, includeEmbedding)).toList();
    }

    @GetMapping("/documents/{id}")
    public DocumentView document(@PathVariable String id, @RequestParam(defaultValue = "false") boolean includeEmbedding) {
        return view(store.getById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found")), includeEmbedding);
    }

    @PostMapping("/search")
    public EmbeddingService.SearchResponse search(@RequestBody SearchRequest request) {
        if (request.query() == null || request.query().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "query is required");
        int topK = request.topK() == null ? 3 : request.topK();
        if (topK < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "topK must be at least 1");
        Double threshold = request.similarityThreshold();
        if (threshold != null && (!Double.isFinite(threshold) || threshold < -1 || threshold > 1)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "similarityThreshold must be between -1 and 1");
        }
        try {
            return service.search(request.query(), topK, threshold);
        } catch (IllegalArgumentException exception) {
            log.warn("Embedding comparison failed: {}", exception.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Embeddings cannot be compared; re-ingest with the current model");
        } catch (DataAccessException exception) {
            log.warn("Knowledge database unavailable: {}", exception.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Knowledge database unavailable");
        } catch (RuntimeException exception) {
            log.warn("Knowledge search failed: {}", exception.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to generate query embedding");
        }
    }

    @DeleteMapping("/documents")
    public Map<String, Object> clear() {
        int deleted = store.clear();
        return Map.of("status", "SUCCESS", "deletedDocuments", deleted, "remainingDocuments", 0);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleError(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of("error", exception.getReason()));
    }

    private DocumentView view(VectorDocument doc, boolean includeEmbedding) {
        float[] embedding = doc.embedding();
        return new DocumentView(doc.id(), doc.text(), doc.metadata(), embedding.length, includeEmbedding ? embedding : null);
    }

    public record SearchRequest(String query, Integer topK, Double similarityThreshold) {}
    public record IngestResponse(String status, int documents, int dimensions, long executionTimeMs) {}
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DocumentView(String id, String text, Map<String, Object> metadata, int dimensions, float[] embedding) {}
}
