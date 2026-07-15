package com.complianceiq.dto;

import com.complianceiq.model.Employee;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EmployeeUpdateRequest {
    private String fullName;
    private String employeeCode;
    private String designation;
    private BigDecimal basicSalary;
    private BigDecimal hra;
    private BigDecimal specialAllowance;
    private BigDecimal totalCtc;
    private String panNumber;
    private String uanNumber;
    private String workState;
    private LocalDate dateOfJoining;
    private Employee.EmploymentType employmentType;
}