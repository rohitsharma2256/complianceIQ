package com.complianceiq.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    /* ================= IDENTITY ================= */
    @Column(name = "employee_code")
    private String employeeCode;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;                          // NAYA - service isko maangta tha

    /* ================= STATUTORY IDs ================= */
    @Column(name = "pan_number", length = 10)
    private String pan;                            // Form 16 / TDS ke liye mandatory
    // (column purana hi rakha - data safe)

    @Column(name = "uan_number", length = 12)
    private String uanNumber;                      // ECR export ke liye

    @Column(name = "esic_ip_number", length = 17)
    private String esicIpNumber;                   // ESI return ke liye

    @Column(name = "aadhaar_number", length = 12)
    private String aadhaarNumber;                  // optional

    /* ================= DATES ================= */
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;                 // gratuity, retirement age

    @Column(name = "date_of_joining")
    private LocalDate dateOfJoining;

    @Column(name = "date_of_leaving")
    private LocalDate dateOfLeaving;               // F&F settlement

    /* ================= ORGANISATION ================= */
    @Column(name = "designation")
    private String designation;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "work_location", length = 100)
    private String workLocation;                   // branch/site

    @Column(name = "work_state")
    private String workState;                      // PT/LWF isi pe depend karta hai

    /* ================= BANK ================= */
    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_account_number", length = 30)
    private String bankAccountNumber;

    @Column(name = "bank_ifsc", length = 15)
    private String bankIfsc;

    /* ================= SALARY STRUCTURE ================= */
    @Column(name = "basic_salary", precision = 10, scale = 2)
    private BigDecimal basicSalary;

    @Column(name = "hra", precision = 10, scale = 2)
    private BigDecimal hra;

    @Column(name = "conveyance_allowance", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal conveyanceAllowance = BigDecimal.ZERO;

    @Column(name = "special_allowance", precision = 10, scale = 2)
    private BigDecimal specialAllowance;

    @Column(name = "medical_allowance", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal medicalAllowance = BigDecimal.ZERO;

    @Column(name = "other_allowance", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal otherAllowance = BigDecimal.ZERO;

    @Column(name = "total_ctc", precision = 10, scale = 2)
    private BigDecimal totalCtc;

    /* ================= TYPE / REGIME ================= */
    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type")
    @Builder.Default
    private EmploymentType employmentType = EmploymentType.PERMANENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_regime")
    @Builder.Default
    private TaxRegime taxRegime = TaxRegime.NEW;

    /* ================= APPLICABILITY FLAGS ================= */
    @Column(name = "is_epf_applicable")
    @Builder.Default
    private Boolean pfApplicable = true;           // column purana, naam naya

    @Column(name = "is_esi_applicable")
    @Builder.Default
    private Boolean isEsiApplicable = false;

    @Column(name = "pt_applicable")
    @Builder.Default
    private Boolean ptApplicable = true;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /* ==================================================================
       ENUMS
       ================================================================== */
    public enum EmploymentType { PERMANENT, CONTRACT, INTERN }

    public enum TaxRegime { OLD, NEW }

    /* ==================================================================
       CALCULATED HELPERS - DB column nahi bante
       ================================================================== */

    /** GROSS = monthly earnings. ESI aur PT dono ISI pe lagte hain, CTC pe NAHI */
    @Transient
    @JsonIgnore
    public BigDecimal getMonthlyGross() {
        return nz(basicSalary)
                .add(nz(hra))
                .add(nz(conveyanceAllowance))
                .add(nz(specialAllowance))
                .add(nz(medicalAllowance))
                .add(nz(otherAllowance));
    }

    /** PF Basic+DA pe lagta hai (DA field nahi hai toh basic hi) */
    @Transient
    @JsonIgnore
    public BigDecimal getPfWageBase() {
        return nz(basicSalary);
    }

    /** PT/LWF ke liye state - employee ka work state, warna company ka */
    @Transient
    @JsonIgnore
    public String getApplicableState() {
        if (workState != null && !workState.isBlank()) return workState;
        return company != null ? company.getState() : null;
    }

    /* ==================================================================
       BRIDGE METHODS - purana code bina change ke chalta rahe
       ================================================================== */

    /** @deprecated use getPan() */
    @Transient
    @JsonIgnore
    public String getPanNumber() { return pan; }

    /** @deprecated use setPan() */
    public void setPanNumber(String panNumber) { this.pan = panNumber; }

    /** @deprecated use getPfApplicable() */
    @Transient
    @JsonIgnore
    public Boolean getIsEpfApplicable() { return pfApplicable; }

    /** @deprecated use setPfApplicable() */
    public void setIsEpfApplicable(Boolean v) { this.pfApplicable = v; }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}