package com.example.productcatalogservice;

import java.util.Map;

public record SearchResult(String id, String text, Map<String, Object> metadata, double score) {}
