package com.example.productcatalogservice;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class KnowledgeStartupIngestionTest {
    private final KnowledgeVectorStore store = mock(KnowledgeVectorStore.class);
    private final EmbeddingService service = mock(EmbeddingService.class);
    private final KnowledgeStartupIngestion startup = new KnowledgeStartupIngestion(store, service);

    @Test void ingestsWhenStoreIsEmpty() throws Exception {
        when(store.count()).thenReturn(0);
        when(service.ingest()).thenReturn(new EmbeddingService.IngestionResult(10, 1536, 100));
        startup.run(null);
        verify(service).ingest();
    }

    @Test void keepsExistingVectors() throws Exception {
        when(store.count()).thenReturn(10);
        startup.run(null);
        verifyNoInteractions(service);
    }

    @Test void reportsIngestionFailureAtStartup() throws Exception {
        when(store.count()).thenReturn(0);
        when(service.ingest()).thenThrow(new IOException("PDF directory unavailable"));
        assertThatThrownBy(() -> startup.run(null)).isInstanceOf(IOException.class);
    }
}
