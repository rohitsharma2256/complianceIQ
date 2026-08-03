package com.complianceiq.controller;

import com.complianceiq.model.AuditLog;
import com.complianceiq.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    /** Firm ka recent activity - tenant-scoped, dusri firm ka nahi dikhega */
    @GetMapping
    public Page<AuditLog> recent(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "30") int size) {
        return auditService.recent(page, size);
    }

    /** Ek record ka poora history - "is employee mein kya-kya badla" */
    @GetMapping("/{entityType}/{entityId}")
    public List<AuditLog> history(@PathVariable String entityType,
                                  @PathVariable UUID entityId) {
        return auditService.historyOf(entityType, entityId);
    }
}