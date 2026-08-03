package com.complianceiq.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Ek hi table mein saare statutory rules: PT, LWF, PF, ESI.
 * Naya state ya rate change = nayi DB row, CODE CHANGE ZERO.
 */
@Entity
@Table(name = "statutory_rules")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StatutoryRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** PT, LWF, PF, ESI */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleType ruleType;

    /** "MAHARASHTRA", "HARYANA"... ya "ALL" country-wide (PF/ESI) ke liye */
    @Column(nullable = false)
    private String state;

    /** Is state mein rule lagta hai ya nahi. Haryana PT -> false (YAHI FIX HAI) */
    @Builder.Default
    private Boolean applicable = true;

    /** Slab range - monthly gross ke hisaab se (PT ke liye) */
    private BigDecimal slabMin;
    private BigDecimal slabMax;

    /** Fixed amount (PT / LWF employee share) */
    private BigDecimal amount;

    /** Percentage rates (PF 12%, ESI 0.75%) ya LWF employer share */
    private BigDecimal employeeRate;
    private BigDecimal employerRate;

    /** PF -> 15000 ceiling | ESI -> 21000 threshold */
    private BigDecimal wageCeiling;

    /**  (rate history + audit ke liye) */
    private LocalDate effectiveFrom;

    @Column(length = 500)
    private String remarks;

    public enum RuleType { PT, LWF, PF, ESI }
}