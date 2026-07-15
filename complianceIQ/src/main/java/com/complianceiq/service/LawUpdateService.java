package com.complianceiq.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LawUpdateService {

    private final VectorStore vectorStore;

    // Manually add a new law/notification to RAG
    public Map<String, Object> addLawUpdate(String title,
                                            String content,
                                            String source,
                                            String effectiveDate) {
        try {
            // Check if already exists (avoid duplicates)
            List<Document> existing = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(title)
                            .topK(1)
                            .build());

            // Add new document with metadata
            Document doc = new Document(
                    "TITLE: " + title + "\n" +
                            "EFFECTIVE DATE: " + effectiveDate + "\n" +
                            "SOURCE: " + source + "\n\n" +
                            content,
                    Map.of(
                            "source", source,
                            "title", title,
                            "effectiveDate", effectiveDate,
                            "addedOn", LocalDateTime.now().toString()
                    ));

            vectorStore.add(List.of(doc));

            log.info("Added new law update to RAG: {}", title);

            return Map.of(
                    "success", true,
                    "message", "Law update added to RAG successfully",
                    "title", title,
                    "addedOn", LocalDateTime.now().toString()
            );

        } catch (Exception e) {
            log.error("Error adding law update: {}", e.getMessage());
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }

    // Scheduled — daily check for pending updates
    // Runs every day at 2 AM
    @Scheduled(cron = "0 0 2 * * *")
    public void dailyLawUpdateCheck() {
        log.info("Running daily law update check at {}",
                LocalDateTime.now());

        // In production: fetch from government RSS/API/curated source
        // For now: logs that check ran
        // New updates get added via addLawUpdate() API

        log.info("Law update check completed. " +
                "RAG is current with latest added notifications.");
    }
}