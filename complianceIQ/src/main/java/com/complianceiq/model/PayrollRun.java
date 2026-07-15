package com.complianceiq.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payroll_runs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "total_employees")
    private Integer totalEmployees;

    @Column(name = "total_basic_salary", precision = 12, scale = 2)
    private BigDecimal totalBasicSalary;

    @Column(name = "total_epf_employee", precision = 12, scale = 2)
    private BigDecimal totalEpfEmployee;

    @Column(name = "total_epf_employer", precision = 12, scale = 2)
    private BigDecimal totalEpfEmployer;

    @Column(name = "total_esi_employee", precision = 12, scale = 2)
    private BigDecimal totalEsiEmployee;

    @Column(name = "total_esi_employer", precision = 12, scale = 2)
    private BigDecimal totalEsiEmployer;

    @Column(name = "total_tds", precision = 12, scale = 2)
    private BigDecimal totalTds;

    @Column(name = "total_professional_tax", precision = 12, scale = 2)
    private BigDecimal totalProfessionalTax;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private Status status = Status.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum Status {
        PENDING,
        PROCESSED,
        FILED
    }
}