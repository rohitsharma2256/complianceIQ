package com.complianceiq.service;

import com.complianceiq.model.AuditLog;
import com.complianceiq.model.AuditLog.Action;
import com.complianceiq.repository.AuditLogRepository;
import com.complianceiq.security.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.UUID;

/**
 * Audit trail writer.
 *
 * Logging ASYNC hai - audit likhna business operation ko slow ya fail
 * nahi karna chahiye. Agar audit write fail ho jaaye toh sirf log,
 * exception aage nahi jaata.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AdminService adminService;

    /* ==================================================================
       WRITE
       ================================================================== */

    @Async
    public void log(Action action, String entityType, UUID entityId, String description) {
        write(action, entityType, entityId, description, null);
    }

    @Async
    public void logChange(Action action, String entityType, UUID entityId,
                          String description, String changes) {
        write(action, entityType, entityId, description, changes);
    }

    private void write(Action action, String entityType, UUID entityId,
                       String description, String changes) {
        try {
            String email = adminService.currentEmail();
            if (email == null) email = "system";

            UUID tenantId = null;
            try {
                tenantId = adminService.currentTenantId();
            } catch (Exception ignored) {
                // Login/register ke waqt tenant context nahi hota
            }

            auditLogRepository.save(AuditLog.builder()
                    .tenantId(tenantId)
                    .userEmail(email)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .description(truncate(description, 500))
                    .changes(changes)
                    .ipAddress(clientIp())
                    .build());

        } catch (Exception e) {
            // Audit failure business operation ko nahi rokna chahiye
            log.warn("Could not write audit log: {}", e.getMessage());
        }
    }

    /* ==================================================================
       READ
       ================================================================== */

    public Page<AuditLog> recent(int page, int size) {
        return auditLogRepository.findByTenantIdOrderByCreatedAtDesc(
                adminService.currentTenantId(), PageRequest.of(page, size));
    }

    /** Ek record ka poora history */
    public List<AuditLog> historyOf(String entityType, UUID entityId) {
        UUID tenantId = adminService.currentTenantId();
        return auditLogRepository
                .findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                .stream()
                .filter(a -> a.getTenantId() == null || a.getTenantId().equals(tenantId))
                .toList();
    }

    /* ==================================================================
       HELPERS
       ================================================================== */

    /** "field: purana -> naya" format - CA ko yeh padhne mein aasan lagta hai */
    public String diff(String field, Object oldValue, Object newValue) {
        if (java.util.Objects.equals(String.valueOf(oldValue), String.valueOf(newValue)))
            return null;
        return field + ": " + oldValue + " -> " + newValue;
    }

    public String joinDiffs(String... diffs) {
        StringBuilder sb = new StringBuilder();
        for (String d : diffs) {
            if (d == null || d.isBlank()) continue;
            if (sb.length() > 0) sb.append("; ");
            sb.append(d);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private String clientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest req = attrs.getRequest();

            // Proxy/load balancer ke peeche asli IP yahan hota hai
            String forwarded = req.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank())
                return forwarded.split(",")[0].trim();
            return req.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}