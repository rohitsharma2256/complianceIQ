package com.complianceiq.dto;

import com.complianceiq.model.Employee;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class EmployeeRequest {

    @NotNull(message = "Company ID is required")
    private UUID companyId;

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String employeeCode;
    private String designation;

    @NotNull(message = "Basic salary is required")
    private BigDecimal basicSalary;

    private BigDecimal hra;
    private BigDecimal specialAllowance;

    @NotNull(message = "Total CTC is required")
    private BigDecimal totalCtc;

    private String panNumber;
    private String uanNumber;

    @NotBlank(message = "Work state is required")
    private String workState;

    private LocalDate dateOfJoining;
    private Employee.EmploymentType employmentType;
}