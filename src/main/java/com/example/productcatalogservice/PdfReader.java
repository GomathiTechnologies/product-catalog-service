package com.example.productcatalogservice;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

@Component
public class PdfReader {
    private static final Logger log = LoggerFactory.getLogger(PdfReader.class);
    private static final Path DIRECTORY = Path.of("src/main/rag-knowledge-base");

    public List<Document> readChunks() throws IOException {
        return readChunks(DIRECTORY);
    }

    private List<Document> readChunks(Path directory) throws IOException {
        List<Path> pdfs;
        try (var files = Files.list(directory)) {
            pdfs = files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf"))
                    .sorted(Comparator.naturalOrder()).toList();
        }
        TokenTextSplitter splitter = new TokenTextSplitter(200, 30, 5, 1000, true);
        List<Document> chunks = new ArrayList<>();
        for (Path pdf : pdfs) {
            List<Document> documents = new PagePdfDocumentReader(new FileSystemResource(pdf)).get();
            List<Document> cleaned = documents.stream()
                    .map(document -> new Document(normalizeText(document.getText()), document.getMetadata())).toList();
            chunks.addAll(splitter.apply(cleaned));
        }
        log.info("Processed {} PDF files and generated {} chunks", pdfs.size(), chunks.size());
        return chunks;
    }

    public static void main(String[] args) throws IOException {
        List<Document> chunks = new PdfReader().readChunks(args.length == 0 ? DIRECTORY : Path.of(args[0]));
        for (Document chunk : chunks) {
            log.info("Chunk {} metadata={} text={}", chunk.getId(), chunk.getMetadata(), chunk.getText());
        }
    }

    private static String normalizeText(String text) {
        return text.replaceAll("[\\t ]+", " ")
                .replaceAll("(?m)^ +| +$", "")
                .replaceAll("(?m)^DEMO DATA - RAG learning corpus\\s*\\d*\\s*$", "")
                .replaceAll("\\n{3,}", "\n\n").trim();
    }
}
