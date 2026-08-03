package com.complianceiq.repository;

import com.complianceiq.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    /** Ek record ka poora history - "is employee mein kya-kya badla" */
    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType, UUID entityId);

    Page<AuditLog> findByTenantIdAndActionOrderByCreatedAtDesc(
            UUID tenantId, AuditLog.Action action, Pageable pageable);

    /** Purane logs cleanup ke liye (retention policy) */
    List<AuditLog> findByCreatedAtBefore(LocalDateTime cutoff);
}