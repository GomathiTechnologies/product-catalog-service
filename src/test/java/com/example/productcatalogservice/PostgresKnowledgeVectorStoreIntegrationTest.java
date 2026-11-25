package com.example.productcatalogservice;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ai.embedding.EmbeddingModel;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {"spring.ai.openai.api-key=test-key", "knowledge.ingest-on-startup=false"})
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
@Transactional
class PostgresKnowledgeVectorStoreIntegrationTest {
    @Autowired KnowledgeVectorStore store;

    @Test void persistsAndSearchesVectorsAndPreservesOldDataOnInvalidReplacement() {
        float[] first = new float[1536]; first[0] = 1;
        float[] second = new float[1536]; second[1] = 1;
        store.replaceAll(List.of(new VectorDocument("first", "First", Map.of("file_name", "a.pdf", "page_number", 1), first),
                new VectorDocument("second", "Second", Map.of("file_name", "b.pdf", "page_number", 1), second)));
        assertThat(store.count()).isEqualTo(2);
        assertThat(store.getById("first")).get().extracting(VectorDocument::text).isEqualTo("First");
        assertThat(store.search(first, 2, null)).extracting(SearchResult::id).containsExactly("first", "second");
        assertThat(store.search(first, 2, 0.5)).extracting(SearchResult::id).containsExactly("first");
        var match = store.search(first, 1, null).get(0);
        assertThat(match.text()).isEqualTo("First");
        assertThat(match.metadata()).containsEntry("file_name", "a.pdf").containsEntry("page_number", 1);
        assertThat(match.score()).isCloseTo(1.0, within(0.000001));
        assertThat(store.search(first, 2, 1.0)).extracting(SearchResult::id).containsExactly("first");

        EmbeddingModel model = mock(EmbeddingModel.class);
        PdfReader reader = mock(PdfReader.class);
        when(model.embed("question")).thenReturn(first);
        var service = new EmbeddingService(model, reader, store);
        var response = service.search("question", 1, null);
        assertThat(response.totalDocuments()).isEqualTo(2);
        assertThat(response.results()).extracting(SearchResult::id).containsExactly("first");
        verify(model, only()).embed("question");
        verifyNoInteractions(reader);
        assertThatThrownBy(() -> store.replaceAll(List.of(new VectorDocument("bad", "Bad", Map.of(), new float[] {1}))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(store.count()).isEqualTo(2);
        assertThat(store.clear()).isEqualTo(2);
        assertThat(store.count()).isZero();
    }
}
