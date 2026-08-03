package com.complianceiq.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable audit trail - kisne, kya, kab kiya.
 *
 * Yeh records kabhi UPDATE ya DELETE nahi hote - append-only hai.
 * Compliance product mein inspector poochh sakta hai ki payroll records
 * mein chhed-chhaad toh nahi hui.
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_tenant_time", columnList = "tenant_id, created_at"),
        @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Kis firm ka record - tenant-scoped audit */
    @Column(name = "tenant_id")
    private UUID tenantId;

    /** Kisne kiya */
    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Action action;

    /** Kis cheez pe - EMPLOYEE, COMPANY, PAYROLL_RUN, ATTENDANCE, LAW_UPDATE */
    @Column(name = "entity_type", length = 40)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    /** Human-readable - CA ko yahi dikhta hai */
    @Column(length = 500)
    private String description;

    /** Kya badla - "basicSalary: 20000 -> 25000" */
    @Column(name = "changes", columnDefinition = "TEXT")
    private String changes;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum Action {
        CREATE, UPDATE, DELETE,
        LOGIN, LOGOUT, PASSWORD_CHANGE,
        PAYROLL_RUN, VIOLATION_RESOLVED,
        DOCUMENT_GENERATED, EXPORT,
        LAW_UPDATE, ACCOUNT_DELETION_REQUESTED, ACCOUNT_REACTIVATED
    }
}