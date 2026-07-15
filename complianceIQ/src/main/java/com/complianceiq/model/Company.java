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
}