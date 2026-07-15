package com.complianceiq.service;

import com.complianceiq.model.Employee;
import com.complianceiq.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MinimumWageService {

    private final EmployeeRepository employeeRepository;

    // State-wise UNSKILLED minimum wage (monthly) - approximate 2026
    // NOTE: Rates change every 6 months (VDA). Verify with state notification.
    private static final Map<String, BigDecimal> UNSKILLED_WAGES = Map.ofEntries(
            Map.entry("Delhi", new BigDecimal("18066")),
            Map.entry("Maharashtra", new BigDecimal("14500")),
            Map.entry("Karnataka", new BigDecimal("15423")),
            Map.entry("Telangana", new BigDecimal("13000")),
            Map.entry("Tamil Nadu", new BigDecimal("13000")),
            Map.entry("Gujarat", new BigDecimal("12500")),
            Map.entry("Haryana", new BigDecimal("15220")),
            Map.entry("Uttar Pradesh", new BigDecimal("13690")),
            Map.entry("West Bengal", new BigDecimal("12000")),
            Map.entry("Kerala", new BigDecimal("14000")),
            Map.entry("Madhya Pradesh", new BigDecimal("12150")),
            Map.entry("Rajasthan", new BigDecimal("9334")),
            Map.entry("Punjab", new BigDecimal("11389")),
            Map.entry("Bihar", new BigDecimal("10800"))
    );

    // Skill multipliers (approximate)
    private static final BigDecimal SEMI_SKILLED_MULT = new BigDecimal("1.10");
    private static final BigDecimal SKILLED_MULT = new BigDecimal("1.20");
    private static final BigDecimal HIGHLY_SKILLED_MULT = new BigDecimal("1.30");

    public Map<String, Object> checkMinimumWage(UUID employeeId,
                                                String skillLevel) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Map<String, Object> result = new HashMap<>();
        result.put("employeeName", emp.getFullName());
        result.put("state", emp.getWorkState());
        result.put("currentGross", emp.getTotalCtc());

        String state = emp.getWorkState() != null ?
                emp.getWorkState().trim() : "";

        BigDecimal baseWage = UNSKILLED_WAGES.get(state);

        if (baseWage == null) {
            result.put("checked", false);
            result.put("reason", "Minimum wage data not available for " +
                    state + ". Please verify with state Labour Department.");
            return result;
        }

        // Apply skill multiplier
        BigDecimal minWage = baseWage;
        String skill = skillLevel != null ? skillLevel.toUpperCase() : "UNSKILLED";

        switch (skill) {
            case "SEMI_SKILLED" -> minWage = baseWage.multiply(SEMI_SKILLED_MULT);
            case "SKILLED" -> minWage = baseWage.multiply(SKILLED_MULT);
            case "HIGHLY_SKILLED" -> minWage = baseWage.multiply(HIGHLY_SKILLED_MULT);
            default -> skill = "UNSKILLED";
        }

        minWage = minWage.setScale(0, java.math.RoundingMode.HALF_UP);

        result.put("skillLevel", skill);
        result.put("minimumWageRequired", minWage);

        // Compliance check
        if (emp.getTotalCtc().compareTo(minWage) >= 0) {
            result.put("compliant", true);
            result.put("message", "Salary meets minimum wage requirement.");
        } else {
            BigDecimal shortfall = minWage.subtract(emp.getTotalCtc());
            result.put("compliant", false);
            result.put("shortfall", shortfall);
            result.put("message", "VIOLATION: Salary Rs " + emp.getTotalCtc() +
                    " is below minimum wage Rs " + minWage +
                    ". Shortfall: Rs " + shortfall);
            result.put("penalty", "Up to 10x compensation under " +
                    "Minimum Wages Act");
        }

        result.put("note", "Rates are approximate and revised twice yearly " +
                "via VDA. Verify with latest state notification before filing.");

        return result;
    }
}