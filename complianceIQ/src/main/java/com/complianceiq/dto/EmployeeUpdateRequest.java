package com.complianceiq.dto;

import com.complianceiq.model.Employee.TaxRegime;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EmployeeUpdateRequest {
    private String fullName;
    private String employeeCode;
    private String email;
    private String phone;
    private String pan;
    private String uanNumber;
    private String esicIpNumber;
    private String aadhaarNumber;
    private LocalDate dateOfBirth;
    private LocalDate dateOfJoining;
    private String designation;
    private String department;
    private String workLocation;
    private String workState;
    private String bankName;
    private String bankAccountNumber;
    private String bankIfsc;
    private BigDecimal basicSalary;
    private BigDecimal hra;
    private BigDecimal conveyanceAllowance;
    private BigDecimal specialAllowance;
    private BigDecimal medicalAllowance;
    private BigDecimal otherAllowance;
    private BigDecimal totalCtc;
    private Boolean pfApplicable;
    private Boolean ptApplicable;
    private TaxRegime taxRegime;
}