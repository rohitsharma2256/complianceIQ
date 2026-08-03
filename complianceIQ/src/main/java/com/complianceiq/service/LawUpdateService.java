package com.complianceiq.service;

import com.complianceiq.model.AuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LawUpdateService {

    private final VectorStore vectorStore;
    private final AuditService auditService;

    /**
     * Naya law/notification RAG mein daalta hai - turant sab firms ke
     * Ask Law answers mein reflect ho jaata hai. Redeployment nahi chahiye.
     */
    public Map<String, Object> addLawUpdate(String title,
                                            String content,
                                            String source,
                                            String effectiveDate) {
        try {
            if (title == null || title.isBlank())
                return Map.of("success", false, "error", "Title is required.");
            if (content == null || content.isBlank())
                return Map.of("success", false, "error", "Content is required.");

            String addedOn = LocalDateTime.now().toString();

            // Metadata mein null nahi ja sakta - Map.of NPE deta hai
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("title", title);
            metadata.put("source", source == null || source.isBlank() ? "Manual entry" : source);
            metadata.put("effectiveDate", effectiveDate == null ? "" : effectiveDate);
            metadata.put("addedOn", addedOn);

            Document doc = new Document(
                    "TITLE: " + title + "\n" +
                            "EFFECTIVE DATE: " + (effectiveDate == null ? "-" : effectiveDate) + "\n" +
                            "SOURCE: " + (source == null ? "-" : source) + "\n\n" +
                            content,
                    metadata);

            vectorStore.add(List.of(doc));

            // Law update saare tenants ko affect karta hai - audit zaroori
            auditService.log(AuditLog.Action.LAW_UPDATE, "LAW_UPDATE", null,
                    "Added law update: " + title
                            + (source == null || source.isBlank() ? "" : " (" + source + ")"));

            log.info("Added law update to knowledge base: {}", title);

            return Map.of(
                    "success", true,
                    "message", "Law update added. It is now live for all firms via Ask Law.",
                    "title", title,
                    "addedOn", addedOn
            );

        } catch (Exception e) {
            log.error("Error adding law update: {}", e.getMessage(), e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Daily check placeholder. Production mein yahan government RSS/API se
     * naye notifications fetch karke auto-ingest honge - abhi manual entry hai.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void dailyLawUpdateCheck() {
        log.info("Daily law update check ran at {}. Knowledge base is current "
                + "with manually added notifications.", LocalDateTime.now());
    }
}