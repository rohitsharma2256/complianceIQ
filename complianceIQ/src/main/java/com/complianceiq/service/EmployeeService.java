package com.complianceiq.service;

import com.complianceiq.dto.EmployeeUpdateRequest;
import com.complianceiq.model.Company;
import com.complianceiq.model.Employee;
import com.complianceiq.repository.CompanyRepository;
import com.complianceiq.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;

    // ESI limit — salary ≤ 21000 toh applicable
    private static final BigDecimal ESI_WAGE_LIMIT = new BigDecimal("21000");

    public Employee addEmployee(UUID companyId,
                                String fullName,
                                String employeeCode,
                                String designation,
                                BigDecimal basicSalary,
                                BigDecimal hra,
                                BigDecimal specialAllowance,
                                BigDecimal totalCtc,
                                String panNumber,
                                String uanNumber,
                                String workState,
                                LocalDate dateOfJoining,
                                Employee.EmploymentType employmentType) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        // Auto check — ESI applicable hai ya nahi
        boolean esiApplicable = totalCtc.compareTo(ESI_WAGE_LIMIT) <= 0;

        Employee employee = Employee.builder()
                .company(company)
                .fullName(fullName)
                .employeeCode(employeeCode)
                .designation(designation)
                .basicSalary(basicSalary)
                .hra(hra)
                .specialAllowance(specialAllowance)
                .totalCtc(totalCtc)
                .panNumber(panNumber)
                .uanNumber(uanNumber)
                .workState(workState)
                .dateOfJoining(dateOfJoining)
                .employmentType(employmentType)
                .isEsiApplicable(esiApplicable)
                .build();

        return employeeRepository.save(employee);
    }

    public List<Employee> getEmployeesByCompany(UUID companyId) {
        return employeeRepository.findByCompanyIdAndIsActiveTrue(companyId);
    }
    public Employee updateEmployee(UUID employeeId, EmployeeUpdateRequest req) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        emp.setFullName(req.getFullName());
        emp.setEmployeeCode(req.getEmployeeCode());
        emp.setDesignation(req.getDesignation());
        emp.setBasicSalary(req.getBasicSalary());
        emp.setHra(req.getHra());
        emp.setSpecialAllowance(req.getSpecialAllowance());
        emp.setTotalCtc(req.getTotalCtc());
        emp.setPanNumber(req.getPanNumber());
        emp.setUanNumber(req.getUanNumber());
        emp.setWorkState(req.getWorkState());
        emp.setDateOfJoining(req.getDateOfJoining());
        emp.setEmploymentType(req.getEmploymentType());

        emp.setIsEsiApplicable(
                req.getTotalCtc().compareTo(ESI_WAGE_LIMIT) <= 0);

        return employeeRepository.save(emp);
    }
    public void deleteEmployee(UUID employeeId) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        emp.setIsActive(false);
        employeeRepository.save(emp);
    }
}