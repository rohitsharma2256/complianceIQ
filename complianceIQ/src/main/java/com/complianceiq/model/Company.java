package com.complianceiq.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "companies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "state", nullable = false)
    private String state;

    @Column(name = "city")
    private String city;

    @Column(name = "industry_type")
    private String industryType;

    @Builder.Default
    @Column(name = "employee_count")
    private Integer employeeCount = 0;

    @Column(name = "epf_registration_number")
    private String epfRegistrationNumber;

    @Column(name = "esic_registration_number")
    private String esicRegistrationNumber;

    @Column(name = "tan_number")
    private String tanNumber;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /* ---------- Statutory registrations */
    @Column(length = 21)
    private String cin;                    // Corporate Identity Number

    @Column(length = 15)
    private String gstin;                  // GST number

    @Column(length = 10)
    private String pan;                    // Company PAN



    /* ---------- Registered address (Form 16 / reports ke liye) ---------- */
    @Column(length = 300)
    private String addressLine1;

    @Column(length = 300)
    private String addressLine2;

    @Column(length = 10)
    private String pincode;

    /* ---------- Company bank (salary transfer / challan) ---------- */
    @Column(length = 100)
    private String bankName;

    @Column(length = 30)
    private String bankAccountNumber;

    @Column(length = 15)
    private String bankIfsc;

    /* ---------- Employer signatory (Form 16 Part B ke liye mandatory) ---------- */
    @Column(length = 150)
    private String signatoryName;

    @Column(length = 150)
    private String signatoryDesignation;

    @Column(length = 500)
    private String logoUrl;                // Payslip pe logo (Checklist D2)
}