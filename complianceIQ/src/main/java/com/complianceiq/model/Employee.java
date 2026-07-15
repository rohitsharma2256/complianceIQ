package com.complianceiq.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "employees")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "employee_code")
    private String employeeCode;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email")
    private String email;

    @Column(name = "designation")
    private String designation;

    // Salary Components
    @Column(name = "basic_salary", precision = 10, scale = 2)
    private BigDecimal basicSalary;

    @Column(name = "hra", precision = 10, scale = 2)
    private BigDecimal hra;

    @Column(name = "special_allowance", precision = 10, scale = 2)
    private BigDecimal specialAllowance;

    @Column(name = "total_ctc", precision = 10, scale = 2)
    private BigDecimal totalCtc;

    // Statutory Info
    @Column(name = "pan_number")
    private String panNumber;

    @Column(name = "uan_number")
    private String uanNumber;

    @Column(name = "esic_ip_number")
    private String esicIpNumber;

    @Column(name = "work_state")
    private String workState;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type")
    @Builder.Default
    private EmploymentType employmentType = EmploymentType.PERMANENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_regime")
    @Builder.Default
    private TaxRegime taxRegime = TaxRegime.NEW;

    @Column(name = "date_of_joining")
    private LocalDate dateOfJoining;

    @Column(name = "is_epf_applicable")
    @Builder.Default
    private Boolean isEpfApplicable = true;

    @Column(name = "is_esi_applicable")
    @Builder.Default
    private Boolean isEsiApplicable = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum EmploymentType {
        PERMANENT,
        CONTRACT,
        INTERN
    }

    public enum TaxRegime {
        OLD,
        NEW
    }
}