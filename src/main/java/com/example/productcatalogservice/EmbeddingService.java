package com.example.productcatalogservice;

import java.util.Arrays;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {
    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public float[] generateEmbedding(String text) {
        float[] vector = embeddingModel.embed(text);
        System.out.println("Text: " + text);
        System.out.println("Dimensions: " + vector.length);
        System.out.println("Vector: " + Arrays.toString(vector));
        return vector;
    }
}
