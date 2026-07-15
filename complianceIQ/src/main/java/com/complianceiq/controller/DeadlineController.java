package com.complianceiq.controller;

import com.complianceiq.service.DeadlineService;
import com.complianceiq.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/deadlines")
@RequiredArgsConstructor
public class DeadlineController {

    private final DeadlineService deadlineService;
    private final EmailService emailService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getDeadlines() {
        List<String> deadlines = deadlineService.getUpcomingDeadlines();
        return ResponseEntity.ok(Map.of(
                "count", deadlines.size(),
                "deadlines", deadlines
        ));
    }

    @GetMapping("/penalty/{taskType}")
    public ResponseEntity<Map<String, String>> getPenalty(
            @PathVariable String taskType) {
        String info = deadlineService.getPenaltyInfo(taskType);
        return ResponseEntity.ok(Map.of(
                "taskType", taskType,
                "penaltyInfo", info
        ));
    }

    // TEST — manually email bhejo
    @PostMapping("/send-test-email")
    public ResponseEntity<Map<String, String>> sendTestEmail(
            @RequestBody Map<String, String> request) {

        String to = request.get("email");
        List<String> deadlines = deadlineService.getUpcomingDeadlines();

        emailService.sendDeadlineAlert(to, "Test CA Firm", deadlines);

        return ResponseEntity.ok(Map.of(
                "message", "Email sent to " + to
        ));
    }
}