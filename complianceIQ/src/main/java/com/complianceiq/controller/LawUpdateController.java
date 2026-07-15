package com.complianceiq.controller;

import com.complianceiq.service.LawUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/law-updates")
@RequiredArgsConstructor
public class LawUpdateController {

    private final LawUpdateService lawUpdateService;

    // Admin adds new law/notification → auto-updates RAG
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addUpdate(
            @RequestBody Map<String, String> request) {

        Map<String, Object> result = lawUpdateService.addLawUpdate(
                request.get("title"),
                request.get("content"),
                request.get("source"),
                request.get("effectiveDate")
        );

        return ResponseEntity.ok(result);
    }
}