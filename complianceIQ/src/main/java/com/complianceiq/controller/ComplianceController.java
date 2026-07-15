package com.complianceiq.controller;

import com.complianceiq.model.ComplianceRecord;
import com.complianceiq.model.PayrollRun;
import com.complianceiq.service.ComplianceCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/compliance")
@RequiredArgsConstructor
public class ComplianceController {

    private final ComplianceCheckService complianceCheckService;

    @PostMapping("/check/{companyId}")
    public ResponseEntity<PayrollRun> runCheck(
            @PathVariable UUID companyId,
            @RequestParam int month,
            @RequestParam int year) {

        return ResponseEntity.ok(
                complianceCheckService.runComplianceCheck(companyId, month, year));
    }

    @GetMapping("/violations/{payrollRunId}")
    public ResponseEntity<List<ComplianceRecord>> getViolations(
            @PathVariable UUID payrollRunId) {

        return ResponseEntity.ok(
                complianceCheckService.getViolations(payrollRunId));
    }

    // NEW — payroll history
    @GetMapping("/runs/{companyId}")
    public ResponseEntity<List<PayrollRun>> getRuns(@PathVariable UUID companyId) {
        return ResponseEntity.ok(
                complianceCheckService.getPayrollHistory(companyId));
    }

    // NEW — mark violation resolved
    @PutMapping("/violations/{violationId}/resolve")
    public ResponseEntity<ComplianceRecord> resolveViolation(
            @PathVariable UUID violationId) {
        return ResponseEntity.ok(
                complianceCheckService.resolveViolation(violationId));
    }
}