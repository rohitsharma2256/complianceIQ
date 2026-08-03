package com.complianceiq.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class AttendanceRequest {

    @NotNull(message = "Employee is required")
    private UUID employeeId;

    @NotNull @Min(1) @Max(12)
    private Integer month;

    @NotNull @Min(2020)
    private Integer year;

    /** Null -> auto calculate (Sundays excluded) */
    private Integer workingDays;

    @DecimalMin(value = "0.0")
    private BigDecimal presentDays;

    @DecimalMin(value = "0.0")
    private BigDecimal paidLeaveDays;

    @DecimalMin(value = "0.0")
    private BigDecimal unpaidLeaveDays;

    @DecimalMin(value = "0.0")
    private BigDecimal overtimeHours;

    private String remarks;
}