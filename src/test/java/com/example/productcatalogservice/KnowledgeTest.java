package com.example.productcatalogservice;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class KnowledgeTest {
    private final InMemoryVectorStore store = new InMemoryVectorStore();
    private final EmbeddingModel model = mock(EmbeddingModel.class);
    private final PdfReader reader = mock(PdfReader.class);
    private final EmbeddingService service = new EmbeddingService(model, reader, store);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new KnowledgeController(service, store)).build();
    private VectorDocument vector(String id, float... values) {
        return new VectorDocument(id, id, Map.of("file_name", id + ".pdf", "page_number", 1), values);
    }

    @Test void ranksAndFiltersAndValidatesDimensions() {
        store.replaceAll(List.of(vector("opposite", -1, 0), vector("same", 1, 0), vector("orthogonal", 0, 1)));
        var results = store.search(store.getAll(), new float[] {1, 0}, 2, 0.1);
        assertThat(results).extracting(SearchResult::id).containsExactly("same");
        assertThat(results.get(0).score()).isEqualTo(1.0);
        assertThat(store.search(store.getAll(), new float[] {1, 0}, 2, null))
                .extracting(SearchResult::id).containsExactly("same", "orthogonal");
        assertThatThrownBy(() -> store.search(store.getAll(), new float[] {1}, 3, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.search(store.getAll(), new float[] {0, 0}, 3, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(store.getById("same")).isPresent();
        float[] leaked = store.getById("same").orElseThrow().embedding();
        leaked[0] = 0;
        assertThat(store.getById("same").orElseThrow().embedding()[0]).isEqualTo(1);
        assertThat(store.clear()).isEqualTo(3);
        assertThat(store.count()).isZero();
    }

    @Test void repeatedIngestionReplacesAndFailurePreservesPreviousDocuments() throws IOException {
        when(reader.readChunks()).thenReturn(List.of(new Document("first"), new Document("second")));
        when(model.embed("first")).thenReturn(new float[] {1, 0});
        when(model.embed("second")).thenReturn(new float[] {0, 1});
        assertThat(service.ingest().documents()).isEqualTo(2);
        assertThat(service.ingest().documents()).isEqualTo(2);
        var previous = store.getAll();
        when(model.embed("second")).thenThrow(new IllegalStateException("failure"));
        assertThatThrownBy(service::ingest).isInstanceOf(IllegalStateException.class);
        assertThat(store.getAll()).isEqualTo(previous);
    }

    @Test void emptyStoreSearchAvoidsEmbeddingAndValidatesRequests() throws Exception {
        mvc.perform(post("/ai/knowledge/search").contentType(MediaType.APPLICATION_JSON).content("{\"query\":\"hello\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalDocuments").value(0))
                .andExpect(jsonPath("$.matchedDocuments").value(0));
        verifyNoInteractions(model);
        for (String body : List.of("{}", "{\"query\":\"  \"}", "{\"query\":\"hi\",\"topK\":0}", "{\"query\":\"hi\",\"similarityThreshold\":1.1}")) {
            mvc.perform(post("/ai/knowledge/search").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
        }
    }

    @Test void endpointsReturnDocumentsAndSearchWithoutVectorsByDefault() throws Exception {
        store.replaceAll(List.of(vector("one", 1, 0), vector("two", 0, 1)));
        when(model.embed("query")).thenReturn(new float[] {1, 0});
        mvc.perform(post("/ai/knowledge/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"query\",\"topK\":1,\"similarityThreshold\":0.3}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.results[0].id").value("one"))
                .andExpect(jsonPath("$.results[0].score").value(1.0)).andExpect(jsonPath("$.results[0].embedding").doesNotExist());
        mvc.perform(get("/ai/knowledge/documents/one")).andExpect(status().isOk())
                .andExpect(jsonPath("$.dimensions").value(2)).andExpect(jsonPath("$.embedding").doesNotExist());
        mvc.perform(get("/ai/knowledge/documents").param("includeEmbedding", "true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].embedding[0]").value(1.0));
        mvc.perform(get("/ai/knowledge/documents/missing")).andExpect(status().isNotFound());
        mvc.perform(delete("/ai/knowledge/documents")).andExpect(status().isOk()).andExpect(jsonPath("$.deletedDocuments").value(2));
        mvc.perform(get("/ai/knowledge/count")).andExpect(jsonPath("$.documents").value(0));
    }
}
