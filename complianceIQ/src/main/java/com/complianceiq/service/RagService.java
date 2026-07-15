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

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final VectorStore vectorStore;

    @PostConstruct
    public void loadLawDocuments() {
        try {
            // Check — documents already loaded?
            List<Document> existing = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query("EPF contribution")
                            .topK(1)
                            .build());

            if (existing != null && !existing.isEmpty()) {
                log.info("Law documents already loaded. Skipping reload.");
                return;
            }

            // Load all .txt files
            PathMatchingResourcePatternResolver resolver =
                    new PathMatchingResourcePatternResolver();

            Resource[] resources = resolver.getResources(
                    "classpath:law-documents/*.txt");

            List<Document> documents = new ArrayList<>();

            for (Resource resource : resources) {
                String content = new String(
                        resource.getInputStream().readAllBytes());

                Document doc = new Document(content,
                        java.util.Map.of("source", resource.getFilename()));
                documents.add(doc);
            }

            if (!documents.isEmpty()) {
                vectorStore.add(documents);
                log.info("Loaded {} law documents into RAG", documents.size());
            }

        } catch (Exception e) {
            log.error("Error loading law documents: {}", e.getMessage());
        }
    }

    public String searchRelevantLaw(String question) {
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(3)
                        .build());

        if (results.isEmpty()) {
            return "No relevant law found.";
        }

        StringBuilder context = new StringBuilder();
        for (Document doc : results) {
            String source = doc.getMetadata()
                    .getOrDefault("source", "unknown").toString();
            context.append("[Source: ").append(source).append("]\n");
            context.append(doc.getText()).append("\n\n");
        }

        return context.toString();
    }
}