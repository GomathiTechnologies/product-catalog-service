package com.example.productcatalogservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "knowledge.ingest-on-startup", havingValue = "true", matchIfMissing = true)
public class KnowledgeStartupIngestion implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeStartupIngestion.class);
    private final KnowledgeVectorStore store;
    private final EmbeddingService embeddingService;

    public KnowledgeStartupIngestion(KnowledgeVectorStore store, EmbeddingService embeddingService) {
        this.store = store;
        this.embeddingService = embeddingService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        int existing = store.count();
        if (existing > 0) {
            log.info("Knowledge store already contains {} documents; skipping startup ingestion", existing);
            return;
        }
        log.info("Knowledge store is empty; ingesting PDF documents");
        EmbeddingService.IngestionResult result = embeddingService.ingest();
        if (result.documents() == 0) {
            log.warn("Startup ingestion produced no documents; check the PDF knowledge-base directory");
        } else {
            log.info("Startup ingestion stored {} documents", result.documents());
        }
    }
}
