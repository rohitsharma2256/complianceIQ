package com.complianceiq.controller;

import com.complianceiq.dto.EmployeeRequest;
import com.complianceiq.dto.EmployeeUpdateRequest;
import com.complianceiq.model.Employee;
import com.complianceiq.service.EmployeeService;
import com.complianceiq.service.ExcelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final ExcelService excelService;

    @PostMapping
    public ResponseEntity<Employee> addEmployee(
            @Valid @RequestBody EmployeeRequest request) {

        Employee employee = employeeService.addEmployee(
                request.getCompanyId(),
                request.getFullName(),
                request.getEmployeeCode(),
                request.getDesignation(),
                request.getBasicSalary(),
                request.getHra(),
                request.getSpecialAllowance(),
                request.getTotalCtc(),
                request.getPanNumber(),
                request.getUanNumber(),
                request.getWorkState(),
                request.getDateOfJoining(),
                request.getEmploymentType()
        );
        return ResponseEntity.ok(employee);
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<Employee>> getEmployees(
            @PathVariable UUID companyId) {
        return ResponseEntity.ok(
                employeeService.getEmployeesByCompany(companyId));
    }

    @PostMapping("/upload/{companyId}")
    public ResponseEntity<Map<String, String>> uploadExcel(
            @PathVariable UUID companyId,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "File is empty"));
        }

        String result = excelService.uploadEmployees(companyId, file);
        return ResponseEntity.ok(Map.of("result", result));
    }

    // NEW — update employee
    @PutMapping("/{employeeId}")
    public ResponseEntity<Employee> updateEmployee(
            @PathVariable UUID employeeId,
            @RequestBody EmployeeUpdateRequest updated) {
        return ResponseEntity.ok(
                employeeService.updateEmployee(employeeId, updated));
    }

    // NEW — delete employee
    @DeleteMapping("/{employeeId}")
    public ResponseEntity<Map<String, String>> deleteEmployee(
            @PathVariable UUID employeeId) {
        employeeService.deleteEmployee(employeeId);
        return ResponseEntity.ok(Map.of("message", "Employee removed"));
    }
}