package com.complianceiq.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Ek employee ka ek month ka attendance summary.
 * LOP (Loss of Pay) yahin se calculate hota hai.
 */
@Entity
@Table(name = "attendance",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_attendance_emp_month",
                columnNames = {"employee_id", "month", "year"}))   // duplicate rokta hai
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(nullable = false)
    private Integer month;                 // 1-12

    @Column(nullable = false)
    private Integer year;

    /** Month ke total working days (Sunday/holiday hata ke) */
    @Column(nullable = false)
    @Builder.Default
    private Integer workingDays = 26;

    @Builder.Default
    private BigDecimal presentDays = BigDecimal.ZERO;

    /** Paid leave - salary katti nahi */
    @Builder.Default
    private BigDecimal paidLeaveDays = BigDecimal.ZERO;

    /** Unpaid absence - YAHI LOP banta hai */
    @Builder.Default
    private BigDecimal unpaidLeaveDays = BigDecimal.ZERO;

    /** Overtime hours */
    @Builder.Default
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    @Column(length = 300)
    private String remarks;

    /* ==================================================================
       CALCULATED - DB mein store nahi hote
       ================================================================== */

    /** Paid days = present + paid leave */
    @Transient
    public BigDecimal getPaidDays() {
        return nz(presentDays).add(nz(paidLeaveDays));
    }

    /** LOP days = unpaid absence */
    @Transient
    public BigDecimal getLopDays() {
        return nz(unpaidLeaveDays);
    }

    /** Attendance factor = paidDays / workingDays  (salary isse multiply hoti hai) */
    @Transient
    public BigDecimal getAttendanceFactor() {
        if (workingDays == null || workingDays == 0) return BigDecimal.ONE;
        BigDecimal factor = getPaidDays()
                .divide(new BigDecimal(workingDays), 6, RoundingMode.HALF_UP);
        // 1 se zyada nahi (extra days ka bonus nahi)
        return factor.compareTo(BigDecimal.ONE) > 0 ? BigDecimal.ONE : factor;
    }

    /** Data sahi hai ya nahi - present + leaves working days se zyada nahi hone chahiye */
    @Transient
    public boolean isValid() {
        if (workingDays == null || workingDays <= 0) return false;
        BigDecimal total = nz(presentDays).add(nz(paidLeaveDays)).add(nz(unpaidLeaveDays));
        return total.compareTo(new BigDecimal(workingDays)) <= 0;
    }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}