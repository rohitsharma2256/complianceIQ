package com.complianceiq.controller;

import com.complianceiq.service.AdditionalCalculationsService;
import com.complianceiq.service.MinimumWageService;
import com.complianceiq.service.ReconciliationService;
import com.complianceiq.service.WorkerLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/calculations")
@RequiredArgsConstructor
public class AdditionalController {

    private final AdditionalCalculationsService calculationsService;
    private final MinimumWageService minimumWageService;
    private final ReconciliationService reconciliationService;
    private final WorkerLifecycleService workerLifecycleService;

    @GetMapping("/gratuity/{employeeId}")
    public ResponseEntity<Map<String, Object>> gratuity(
            @PathVariable UUID employeeId) {
        return ResponseEntity.ok(
                calculationsService.calculateGratuity(employeeId));
    }

    @GetMapping("/bonus/{employeeId}")
    public ResponseEntity<Map<String, Object>> bonus(
            @PathVariable UUID employeeId,
            @RequestParam(defaultValue = "8.33") double percent) {
        return ResponseEntity.ok(
                calculationsService.calculateBonus(employeeId, percent));
    }

    @GetMapping("/lwf/{employeeId}")
    public ResponseEntity<Map<String, Object>> lwf(
            @PathVariable UUID employeeId) {
        return ResponseEntity.ok(
                calculationsService.calculateLWF(employeeId));
    }
    @GetMapping("/minimum-wage/{employeeId}")
    public ResponseEntity<Map<String, Object>> minimumWage(
            @PathVariable UUID employeeId,
            @RequestParam(defaultValue = "UNSKILLED") String skill) {
        return ResponseEntity.ok(
                minimumWageService.checkMinimumWage(employeeId, skill));
    }

    @GetMapping("/reconcile/{companyId}")
    public ResponseEntity<Map<String, Object>> reconcile(
            @PathVariable UUID companyId) {
        return ResponseEntity.ok(
                reconciliationService.reconcile(companyId));
    }

    @GetMapping("/contract-workers/{companyId}")
    public ResponseEntity<Map<String, Object>> contractWorkers(
            @PathVariable UUID companyId) {
        return ResponseEntity.ok(
                workerLifecycleService.checkContractWorkers(companyId));
    }

    @GetMapping("/full-final/{employeeId}")
    public ResponseEntity<Map<String, Object>> fullAndFinal(
            @PathVariable UUID employeeId,
            @RequestParam(defaultValue = "0") int pendingLeaveDays) {
        return ResponseEntity.ok(
                workerLifecycleService.calculateFullAndFinal(
                        employeeId, pendingLeaveDays));
    }

    @GetMapping("/uan-kyc/{companyId}")
    public ResponseEntity<Map<String, Object>> uanKyc(
            @PathVariable UUID companyId) {
        return ResponseEntity.ok(
                workerLifecycleService.checkUanKyc(companyId));
    }
}