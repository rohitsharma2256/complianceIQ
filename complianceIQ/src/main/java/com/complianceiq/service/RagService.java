package com.complianceiq.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final VectorStore vectorStore;

    /** Har chunk ka target size (characters). ~250 tokens */
    private static final int CHUNK_SIZE = 1000;

    /** Chunks ke beech overlap - taaki context beech mein na toote */
    private static final int CHUNK_OVERLAP = 150;

    /** Prompt mein bheja jaane wala max context - rate limit se bachne ke liye */
    private static final int MAX_CONTEXT_CHARS = 4000;

    @PostConstruct
    public void loadLawDocuments() {
        try {
            // Documents already loaded? -> skip
            List<Document> existing = vectorStore.similaritySearch(
                    SearchRequest.builder().query("EPF contribution").topK(1).build());

            if (existing != null && !existing.isEmpty()) {
                log.info("Law documents already loaded. Skipping reload.");
                return;
            }

            PathMatchingResourcePatternResolver resolver =
                    new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:law-documents/*.txt");

            List<Document> documents = new ArrayList<>();

            for (Resource resource : resources) {
                String content = new String(
                        resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                String filename = resource.getFilename();

                // POORI FILE nahi - chunks banao (yahi RAG ka asli kaam hai)
                List<String> chunks = splitIntoChunks(content);

                for (int i = 0; i < chunks.size(); i++) {
                    documents.add(new Document(chunks.get(i), Map.of(
                            "source", filename == null ? "unknown" : filename,
                            "chunk", i + 1,
                            "totalChunks", chunks.size()
                    )));
                }
                log.info("{} -> {} chunks", filename, chunks.size());
            }

            if (!documents.isEmpty()) {
                vectorStore.add(documents);
                log.info("Loaded {} chunks from {} law documents into RAG",
                        documents.size(), resources.length);
            } else {
                log.warn("No law documents found at classpath:law-documents/*.txt");
            }

        } catch (Exception e) {
            log.error("Error loading law documents: {}", e.getMessage(), e);
        }
    }

    /**
     * Document ko chhote chunks mein todo.
     * Paragraph boundary pe todne ki koshish karta hai taaki meaning na toote.
     */
    private List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;

        text = text.trim();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());

            // Beech mein sentence na kate - paragraph/line break dhundo
            if (end < text.length()) {
                int para = text.lastIndexOf("\n\n", end);
                int line = text.lastIndexOf("\n", end);
                int dot  = text.lastIndexOf(". ", end);

                int breakAt = Math.max(para, Math.max(line, dot));
                // Break point chunk ke aadhe se aage ho tabhi use karo
                if (breakAt > start + CHUNK_SIZE / 2) {
                    end = breakAt + 1;
                }
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) chunks.add(chunk);

            // Overlap - agla chunk thoda peeche se shuru (context continuity)
            start = end - CHUNK_OVERLAP;
            if (start < 0 || start >= text.length()) break;
            if (end >= text.length()) break;
        }
        return chunks;
    }

    /**
     * Question se relevant law chunks nikaalo.
     * topK 2 rakha hai - rate limit se bachne ke liye.
     */
    public String searchRelevantLaw(String question) {
        try {
            List<Document> results = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(question)
                            .topK(2)                    // 3 se 2 - tokens bachate hain
                            .build());

            if (results == null || results.isEmpty()) {
                return "No relevant law found in the knowledge base.";
            }

            StringBuilder context = new StringBuilder();
            for (Document doc : results) {
                String source = doc.getMetadata()
                        .getOrDefault("source", "unknown").toString();

                String piece = "[Source: " + source + "]\n" + doc.getText() + "\n\n";

                // Hard cap - context bada hua toh 429 aayega
                if (context.length() + piece.length() > MAX_CONTEXT_CHARS) break;
                context.append(piece);
            }

            return context.length() == 0
                    ? "No relevant law found in the knowledge base."
                    : context.toString();

        } catch (Exception e) {
            log.error("RAG search failed: {}", e.getMessage(), e);
            return "Could not search the law knowledge base.";
        }
    }
}