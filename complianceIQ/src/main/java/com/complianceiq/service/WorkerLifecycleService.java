package com.complianceiq.service;

import com.complianceiq.model.Employee;
import com.complianceiq.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkerLifecycleService {

    private final EmployeeRepository employeeRepository;

    // ─── CONTRACT WORKERS ───
    // Principal employer liability check
    public Map<String, Object> checkContractWorkers(UUID companyId) {
        List<Employee> employees = employeeRepository
                .findByCompanyIdAndIsActiveTrue(companyId);

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> contractWorkers = new ArrayList<>();

        for (Employee emp : employees) {
            if (emp.getEmploymentType() ==
                    Employee.EmploymentType.CONTRACT) {

                Map<String, Object> worker = new HashMap<>();
                worker.put("name", emp.getFullName());
                worker.put("epfApplicable", emp.getIsEpfApplicable());
                worker.put("esiApplicable", emp.getIsEsiApplicable());

                // Compliance flags
                List<String> alerts = new ArrayList<>();
                if (emp.getUanNumber() == null || emp.getUanNumber().isBlank()) {
                    alerts.add("UAN missing - principal employer liable");
                }
                if (!Boolean.TRUE.equals(emp.getIsEpfApplicable())) {
                    alerts.add("EPF not marked - verify contractor compliance");
                }
                worker.put("alerts", alerts);
                contractWorkers.add(worker);
            }
        }

        result.put("totalContractWorkers", contractWorkers.size());
        result.put("contractWorkers", contractWorkers);
        result.put("note", "Principal employer is liable for PF/ESI " +
                "compliance of contract workers if contractor defaults.");

        return result;
    }

    // ─── FULL & FINAL SETTLEMENT ───
    // On exit: gratuity + leave encashment + pending dues
    public Map<String, Object> calculateFullAndFinal(UUID employeeId,
                                                     int pendingLeaveDays) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Map<String, Object> result = new HashMap<>();
        result.put("employeeName", emp.getFullName());

        BigDecimal basic = emp.getBasicSalary();

        // 1. Gratuity (if 5+ years)
        BigDecimal gratuity = BigDecimal.ZERO;
        if (emp.getDateOfJoining() != null) {
            long years = ChronoUnit.YEARS.between(
                    emp.getDateOfJoining(), LocalDate.now());
            if (years >= 5) {
                gratuity = basic.multiply(new BigDecimal("15"))
                        .multiply(new BigDecimal(years))
                        .divide(new BigDecimal("26"), 2, RoundingMode.HALF_UP);
            }
            result.put("yearsOfService", years);
        }

        // 2. Leave encashment (per day = basic / 30)
        BigDecimal perDaySalary = basic.divide(
                new BigDecimal("30"), 2, RoundingMode.HALF_UP);
        BigDecimal leaveEncashment = perDaySalary
                .multiply(new BigDecimal(pendingLeaveDays));

        // 3. Final month salary (assume full)
        BigDecimal finalSalary = emp.getTotalCtc();

        BigDecimal totalSettlement = gratuity
                .add(leaveEncashment)
                .add(finalSalary);

        result.put("gratuity", gratuity);
        result.put("pendingLeaveDays", pendingLeaveDays);
        result.put("leaveEncashment", leaveEncashment);
        result.put("finalMonthSalary", finalSalary);
        result.put("totalSettlement", totalSettlement);
        result.put("settlementDeadline", "Within 2 working days of exit " +
                "(as per Labour Code 2025)");

        return result;
    }

    // ─── UAN / KYC TRACKING ───
    public Map<String, Object> checkUanKyc(UUID companyId) {
        List<Employee> employees = employeeRepository
                .findByCompanyIdAndIsActiveTrue(companyId);

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> pending = new ArrayList<>();

        int totalEpfEmployees = 0;
        int uanMissing = 0;
        int panMissing = 0;

        for (Employee emp : employees) {
            if (Boolean.TRUE.equals(emp.getIsEpfApplicable())) {
                totalEpfEmployees++;

                boolean hasIssue = false;
                Map<String, Object> item = new HashMap<>();
                item.put("name", emp.getFullName());
                List<String> issues = new ArrayList<>();

                if (emp.getUanNumber() == null || emp.getUanNumber().isBlank()) {
                    issues.add("UAN not created");
                    uanMissing++;
                    hasIssue = true;
                }
                if (emp.getPanNumber() == null || emp.getPanNumber().isBlank()) {
                    issues.add("PAN/KYC pending");
                    panMissing++;
                    hasIssue = true;
                }

                if (hasIssue) {
                    item.put("issues", issues);
                    pending.add(item);
                }
            }
        }

        result.put("totalEpfEmployees", totalEpfEmployees);
        result.put("uanMissing", uanMissing);
        result.put("panMissing", panMissing);
        result.put("pendingKyc", pending);
        result.put("status", pending.isEmpty() ? "ALL_COMPLETE" : "ACTION_NEEDED");

        return result;
    }
}