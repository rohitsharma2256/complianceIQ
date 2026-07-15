package com.complianceiq.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "compliance_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_run_id", nullable = false)
    private PayrollRun payrollRun;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "violation_type")
    private ViolationType violationType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "recommended_fix", columnDefinition = "TEXT")
    private String recommendedFix;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    @Builder.Default
    private Severity severity = Severity.MEDIUM;

    @Column(name = "is_resolved")
    @Builder.Default
    private Boolean isResolved = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum ViolationType {
        BASIC_SALARY_RULE,      // 50% basic rule violation
        EPF_CALCULATION,        // EPF galat calculate hua
        ESI_CALCULATION,        // ESI galat calculate hua
        TDS_CALCULATION,        // TDS galat calculate hua
        PROFESSIONAL_TAX,       // PT missing
        MINIMUM_WAGE,           // Minimum wage violation
        LABOUR_WELFARE_FUND     // LWF missing
    }

    public enum Severity {
        LOW,
        MEDIUM,
        HIGH
    }
}