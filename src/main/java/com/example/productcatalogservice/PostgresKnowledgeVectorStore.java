package com.example.productcatalogservice;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PostgresKnowledgeVectorStore implements KnowledgeVectorStore {
    private static final int DIMENSIONS = 1536;
    private static final TypeReference<Map<String, Object>> METADATA_TYPE = new TypeReference<>() {};
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public PostgresKnowledgeVectorStore(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void replaceAll(List<VectorDocument> documents) {
        // Validate and serialize before deleting; transaction rollback also protects the old collection.
        var rows = documents.stream().map(doc -> new Row(doc.id(), doc.text(), json(doc.metadata()), vector(doc.embedding()))).toList();
        jdbc.update("DELETE FROM public.knowledge_vectors");
        for (Row row : rows) {
            jdbc.update("INSERT INTO public.knowledge_vectors (id, content, metadata, embedding) VALUES (?, ?, CAST(? AS jsonb), CAST(? AS vector))",
                    row.id(), row.text(), row.metadata(), row.embedding());
        }
    }

    @Override
    public List<VectorDocument> getAll() {
        return jdbc.query("SELECT id, content, metadata::text, embedding::text FROM public.knowledge_vectors ORDER BY id", (rs, row) ->
                new VectorDocument(rs.getString(1), rs.getString(2), metadata(rs.getString(3)), parseVector(rs.getString(4))));
    }

    @Override
    public Optional<VectorDocument> getById(String id) {
        return jdbc.query("SELECT id, content, metadata::text, embedding::text FROM public.knowledge_vectors WHERE id = ?", (rs, row) ->
                new VectorDocument(rs.getString(1), rs.getString(2), metadata(rs.getString(3)), parseVector(rs.getString(4))), id).stream().findFirst();
    }

    @Override
    public int count() {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM public.knowledge_vectors", Integer.class);
        return count == null ? 0 : count;
    }

    @Override
    @Transactional
    public int clear() { return jdbc.update("DELETE FROM public.knowledge_vectors"); }

    @Override
    public List<SearchResult> search(float[] query, int topK, Double threshold) {
        String sql = "SELECT id, content, metadata::text, 1 - (embedding <=> CAST(? AS vector)) AS score "
                + "FROM public.knowledge_vectors WHERE (CAST(? AS double precision) IS NULL OR 1 - (embedding <=> CAST(? AS vector)) >= ?) "
                + "ORDER BY embedding <=> CAST(? AS vector) LIMIT ?";
        String value = vector(query);
        return jdbc.query(sql, (rs, row) -> new SearchResult(rs.getString(1), rs.getString(2), metadata(rs.getString(3)), rs.getDouble(4)),
                value, threshold, value, threshold, value, topK);
    }

    private String vector(float[] values) {
        if (values == null || values.length != DIMENSIONS) throw new IllegalArgumentException("Embedding must have " + DIMENSIONS + " dimensions");
        boolean nonzero = false;
        for (float value : values) {
            if (!Float.isFinite(value)) throw new IllegalArgumentException("Embedding contains non-finite values");
            nonzero |= value != 0;
        }
        if (!nonzero) throw new IllegalArgumentException("Embedding must not be zero");
        return "[" + java.util.stream.IntStream.range(0, values.length).mapToObj(i -> Float.toString(values[i])).collect(Collectors.joining(",")) + "]";
    }

    private float[] parseVector(String value) {
        String[] items = value.substring(1, value.length() - 1).split(",");
        float[] vector = new float[items.length];
        for (int i = 0; i < items.length; i++) vector[i] = Float.parseFloat(items[i]);
        return vector;
    }

    private String json(Map<String, Object> metadata) {
        try { return mapper.writeValueAsString(metadata); }
        catch (JsonProcessingException exception) { throw new IllegalArgumentException("Could not serialize metadata", exception); }
    }

    private Map<String, Object> metadata(String value) {
        try { return mapper.readValue(value, METADATA_TYPE); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Could not read metadata", exception); }
    }

    private record Row(String id, String text, String metadata, String embedding) {}
}
