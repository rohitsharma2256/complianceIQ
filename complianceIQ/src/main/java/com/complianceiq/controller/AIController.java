package com.complianceiq.controller;

import com.complianceiq.service.AIComplianceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIComplianceService aiComplianceService;

    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> askQuestion(@RequestBody Map<String, String> request) {

        String question = request.get("question");
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Question cannot be empty"));
        }

        String answer = aiComplianceService.askComplianceQuestion(question);
        return ResponseEntity.ok(Map.of("question", question, "answer", answer));
    }

    @PostMapping("/ask-rag")
    public ResponseEntity<Map<String, String>> askWithRAG(@RequestBody Map<String, String> request) {

        String question = request.get("question");
        String answer = aiComplianceService.askWithRAG(question);
        return ResponseEntity.ok(Map.of("question", question, "answer", answer));
    }

    // AGENTIC endpoint — AI khud actions lega
    @PostMapping("/agent")
    public ResponseEntity<Map<String, String>> askAgent(@RequestBody Map<String, String> request) {

        String question = request.get("question");
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Question cannot be empty"));
        }

        String answer = aiComplianceService.askAgentic(question);
        return ResponseEntity.ok(Map.of("question", question, "answer", answer));
    }

    @PostMapping("/analyze-violation")
    public ResponseEntity<Map<String, String>> analyzeViolation(@RequestBody Map<String, String> request) {

        String analysis = aiComplianceService.analyzeViolation(
                request.get("employeeName"),
                request.get("violationType"),
                request.get("description")
        );
        return ResponseEntity.ok(Map.of("analysis", analysis));
    }

    @PostMapping("/compliance-summary")
    public ResponseEntity<Map<String, String>> generateSummary(@RequestBody Map<String, Object> request) {

        String summary = aiComplianceService.generateComplianceSummary(
                (Integer) request.get("totalEmployees"),
                (String) request.get("epfTotal"),
                (String) request.get("esiTotal"),
                (String) request.get("tdsTotal"),
                (String) request.get("ptTotal"),
                (Integer) request.get("violationCount")
        );
        return ResponseEntity.ok(Map.of("summary", summary));
    }
}