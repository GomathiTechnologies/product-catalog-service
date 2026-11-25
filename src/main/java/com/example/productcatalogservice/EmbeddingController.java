package com.example.productcatalogservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/ai")
public class EmbeddingController {
    private static final Logger log = LoggerFactory.getLogger(EmbeddingController.class);
    private final EmbeddingService embeddingService;

    public EmbeddingController(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @PostMapping("/embedding")
    public EmbeddingResponse embed(@RequestBody EmbeddingRequest request) {
        if (request.text() == null || request.text().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "text is required");
        }
        try {
            float[] vector = embeddingService.generateEmbedding(request.text());
            return new EmbeddingResponse(vector.length, vector);
        } catch (RuntimeException exception) {
            log.warn("Embedding request failed ({}).", exception.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Embedding is temporarily unavailable");
        }
    }

    public record EmbeddingRequest(String text) {}
    public record EmbeddingResponse(int dimensions, float[] vector) {}
}
