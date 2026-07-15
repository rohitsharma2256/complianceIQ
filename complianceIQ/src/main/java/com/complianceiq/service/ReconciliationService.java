package com.complianceiq.service;

import com.complianceiq.model.Employee;
import com.complianceiq.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final EmployeeRepository employeeRepository;

    private static final BigDecimal EPF_RATE = new BigDecimal("0.12");
    private static final BigDecimal ESI_EMP_RATE = new BigDecimal("0.0075");
    private static final BigDecimal ESI_LIMIT = new BigDecimal("21000");

    // Reconcile — check if provided values match rule-based expected values
    public Map<String, Object> reconcile(UUID companyId) {

        List<Employee> employees = employeeRepository
                .findByCompanyIdAndIsActiveTrue(companyId);

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> discrepancies = new ArrayList<>();

        int totalChecked = 0;
        int issuesFound = 0;

        for (Employee emp : employees) {
            totalChecked++;

            BigDecimal basic = emp.getBasicSalary();
            BigDecimal gross = emp.getTotalCtc();

            // Expected EPF (rule-based)
            BigDecimal expectedEpf = basic.multiply(EPF_RATE)
                    .setScale(2, RoundingMode.HALF_UP);

            // Expected ESI
            BigDecimal expectedEsi = BigDecimal.ZERO;
            if (gross.compareTo(ESI_LIMIT) <= 0) {
                expectedEsi = gross.multiply(ESI_EMP_RATE)
                        .setScale(2, RoundingMode.HALF_UP);
            }

            // Check 1 — Basic salary sanity (should be > 0)
            if (basic.compareTo(BigDecimal.ZERO) <= 0) {
                discrepancies.add(makeDiscrepancy(emp.getFullName(),
                        "BASIC_SALARY",
                        "Basic salary is zero or negative",
                        "Set valid basic salary"));
                issuesFound++;
            }

            // Check 2 — Basic > Gross (impossible)
            if (basic.compareTo(gross) > 0) {
                discrepancies.add(makeDiscrepancy(emp.getFullName(),
                        "BASIC_GROSS",
                        "Basic (" + basic + ") exceeds Gross (" + gross + ")",
                        "Basic cannot be more than total CTC"));
                issuesFound++;
            }

            // Check 3 — ESI applicability flag vs actual
            boolean shouldHaveEsi = gross.compareTo(ESI_LIMIT) <= 0;
            if (shouldHaveEsi != Boolean.TRUE.equals(emp.getIsEsiApplicable())) {
                discrepancies.add(makeDiscrepancy(emp.getFullName(),
                        "ESI_FLAG",
                        "ESI applicability flag mismatch. Gross " + gross +
                                (shouldHaveEsi ? " should have ESI" :
                                        " should NOT have ESI"),
                        "Correct the ESI applicability flag"));
                issuesFound++;
            }

            // Check 4 — PAN missing (needed for TDS)
            if (emp.getPanNumber() == null || emp.getPanNumber().isBlank()) {
                discrepancies.add(makeDiscrepancy(emp.getFullName(),
                        "PAN_MISSING",
                        "PAN number missing (required for TDS)",
                        "Add PAN number"));
                issuesFound++;
            }

            // Check 5 — UAN missing (needed for EPF)
            if (Boolean.TRUE.equals(emp.getIsEpfApplicable()) &&
                    (emp.getUanNumber() == null || emp.getUanNumber().isBlank())) {
                discrepancies.add(makeDiscrepancy(emp.getFullName(),
                        "UAN_MISSING",
                        "UAN missing but EPF applicable",
                        "Add UAN number for EPF filing"));
                issuesFound++;
            }
        }

        result.put("totalEmployeesChecked", totalChecked);
        result.put("issuesFound", issuesFound);
        result.put("status", issuesFound == 0 ? "CLEAN" : "ISSUES_FOUND");
        result.put("discrepancies", discrepancies);

        if (issuesFound == 0) {
            result.put("message", "Reconciliation clean. " +
                    "All records consistent with statutory rules.");
        } else {
            result.put("message", issuesFound + " discrepancies found. " +
                    "Fix before filing deadline to avoid penalties.");
        }

        return result;
    }

    private Map<String, Object> makeDiscrepancy(String employee, String type,
                                                String issue, String fix) {
        Map<String, Object> d = new HashMap<>();
        d.put("employee", employee);
        d.put("type", type);
        d.put("issue", issue);
        d.put("recommendedFix", fix);
        return d;
    }
}