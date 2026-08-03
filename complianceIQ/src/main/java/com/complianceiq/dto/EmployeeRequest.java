package com.complianceiq.dto;

import com.complianceiq.model.Employee.TaxRegime;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class EmployeeRequest {

    @NotNull(message = "Company is required")
    private UUID companyId;

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String employeeCode;

    @Email(message = "Invalid email")
    private String email;

    private String phone;

    /* ---------- Statutory identifiers ---------- */
    @Pattern(regexp = "^$|^[A-Z]{5}[0-9]{4}[A-Z]$", message = "Invalid PAN format")
    private String pan;

    @Pattern(regexp = "^$|^[0-9]{12}$", message = "UAN must be 12 digits")
    private String uanNumber;

    private String esicIpNumber;
    private String aadhaarNumber;

    /* ---------- Dates ---------- */
    private LocalDate dateOfBirth;
    private LocalDate dateOfJoining;

    /* ---------- Organisation ---------- */
    private String designation;
    private String department;
    private String workLocation;
    private String workState;

    /* ---------- Bank ---------- */
    private String bankName;
    private String bankAccountNumber;
    private String bankIfsc;

    /* ---------- Salary structure ---------- */
    @NotNull(message = "Basic salary is required")
    @DecimalMin(value = "0.0", message = "Basic cannot be negative")
    private BigDecimal basicSalary;

    private BigDecimal hra;
    private BigDecimal conveyanceAllowance;
    private BigDecimal specialAllowance;
    private BigDecimal medicalAllowance;
    private BigDecimal otherAllowance;

    @NotNull(message = "Total CTC is required")
    private BigDecimal totalCtc;

    /* ---------- Flags ---------- */
    private Boolean pfApplicable = true;
    private Boolean ptApplicable = true;
    private TaxRegime taxRegime = TaxRegime.NEW;
}