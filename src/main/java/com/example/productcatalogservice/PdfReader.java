package com.example.productcatalogservice;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.FileSystemResource;

/** Standalone utility for inspecting the documents produced by the PDF reader. */
public class PdfReader {

    public static void main(String[] args) throws IOException {
        Path directory = Path.of(args.length == 0 ? "src/main/rag-knowledge-base" : args[0]);
        List<Path> pdfs;
        try (var files = Files.list(directory)) {
            pdfs = files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf"))
                    .sorted(Comparator.naturalOrder())
                    .toList();
        }

        if (pdfs.isEmpty()) {
            System.out.println("No PDF files found in " + directory);
            return;
        }
        TokenTextSplitter splitter = new TokenTextSplitter(200, 30, 5, 1000, true);
        int totalChunks = 0;
        for (Path pdf : pdfs) {
            List<Document> documents = new PagePdfDocumentReader(new FileSystemResource(pdf)).get();
            List<Document> cleanedDocuments = documents.stream()
                    .map(document -> new Document(normalizeText(document.getText()), document.getMetadata()))
                    .toList();
            List<Document> chunks = splitter.apply(cleanedDocuments);
            totalChunks += chunks.size();
            System.out.printf("%n=== %s: %d document(s), %d chunk(s) ===%n",
                    pdf.getFileName(), documents.size(), chunks.size());
            for (int i = 0; i < chunks.size(); i++) {
                Document chunk = chunks.get(i);
                System.out.printf("%n--- Chunk %d ---%n", i + 1);
                System.out.println("ID: " + chunk.getId());
                System.out.println("Metadata: " + chunk.getMetadata());
                System.out.println("Text:\n" + chunk.getText());
            }
        }
        System.out.println("\nTotal PDFs: " + pdfs.size());
        System.out.println("Total chunks: " + totalChunks);
    }

    private static String normalizeText(String text) {
        return text.replaceAll("[\\t ]+", " ")
                .replaceAll("(?m)^ +| +$", "")
                .replaceAll("(?m)^DEMO DATA - RAG learning corpus\\s*\\d*\\s*$", "")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
