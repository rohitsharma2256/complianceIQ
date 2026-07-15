package com.complianceiq.service;

import com.complianceiq.model.Employee;
import com.complianceiq.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdditionalCalculationsService {

    private final EmployeeRepository employeeRepository;

    // ─── GRATUITY ───
    // Formula: (Last Basic + DA) x 15 x years / 26
    // Eligible after 5 years continuous service
    public Map<String, Object> calculateGratuity(UUID employeeId) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Map<String, Object> result = new HashMap<>();

        if (emp.getDateOfJoining() == null) {
            result.put("eligible", false);
            result.put("reason", "Date of joining not available");
            return result;
        }

        long years = ChronoUnit.YEARS.between(
                emp.getDateOfJoining(), LocalDate.now());

        result.put("yearsOfService", years);
        result.put("employeeName", emp.getFullName());

        if (years < 5) {
            result.put("eligible", false);
            result.put("reason", "Minimum 5 years service required. " +
                    "Current: " + years + " years");
            result.put("gratuityAmount", BigDecimal.ZERO);
        } else {
            // Gratuity = (Basic x 15 x years) / 26
            BigDecimal gratuity = emp.getBasicSalary()
                    .multiply(new BigDecimal("15"))
                    .multiply(new BigDecimal(years))
                    .divide(new BigDecimal("26"), 2, RoundingMode.HALF_UP);

            // Max cap: 20 lakh
            BigDecimal maxGratuity = new BigDecimal("2000000");
            if (gratuity.compareTo(maxGratuity) > 0) {
                gratuity = maxGratuity;
            }

            result.put("eligible", true);
            result.put("gratuityAmount", gratuity);
            result.put("formula", "(Basic " + emp.getBasicSalary() +
                    " x 15 x " + years + " years) / 26");
        }

        return result;
    }

    // ─── BONUS ───
    // Payment of Bonus Act: 8.33% min, 20% max
    // Applicable if salary <= 21000
    public Map<String, Object> calculateBonus(UUID employeeId,
                                              double bonusPercent) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Map<String, Object> result = new HashMap<>();
        result.put("employeeName", emp.getFullName());

        // Bonus applicable only if salary <= 21000
        BigDecimal bonusLimit = new BigDecimal("21000");
        if (emp.getTotalCtc().compareTo(bonusLimit) > 0) {
            result.put("eligible", false);
            result.put("reason", "Salary exceeds Rs 21,000 bonus limit");
            result.put("bonusAmount", BigDecimal.ZERO);
            return result;
        }

        // Validate bonus percent (8.33 to 20)
        if (bonusPercent < 8.33) bonusPercent = 8.33;
        if (bonusPercent > 20) bonusPercent = 20;

        // Bonus calculated on basic (or 7000, whichever less for calculation ceiling)
        BigDecimal calcBase = emp.getBasicSalary();
        BigDecimal ceiling = new BigDecimal("7000");
        if (calcBase.compareTo(ceiling) > 0) {
            calcBase = ceiling;  // Bonus ceiling
        }

        // Annual bonus = monthly basic x 12 x percent
        BigDecimal annualBonus = calcBase
                .multiply(new BigDecimal("12"))
                .multiply(BigDecimal.valueOf(bonusPercent / 100))
                .setScale(2, RoundingMode.HALF_UP);

        result.put("eligible", true);
        result.put("bonusPercent", bonusPercent);
        result.put("annualBonus", annualBonus);
        result.put("note", "Bonus calculated on ceiling of Rs 7,000 " +
                "as per Payment of Bonus Act");

        return result;
    }

    // ─── LWF (Labour Welfare Fund) ───
// Only 16 states have LWF. Fixed rupee amounts, not percentage.
    public Map<String, Object> calculateLWF(UUID employeeId) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Map<String, Object> result = new HashMap<>();
        result.put("employeeName", emp.getFullName());
        result.put("state", emp.getWorkState());

        String state = emp.getWorkState() != null ?
                emp.getWorkState().trim() : "";

        BigDecimal empShare;
        BigDecimal erShare;
        String frequency;

        switch (state) {
            case "Maharashtra" -> {
                empShare = new BigDecimal("25");
                erShare = new BigDecimal("75");
                frequency = "Half-yearly (June & December)";
            }
            case "Karnataka" -> {
                empShare = new BigDecimal("50");
                erShare = new BigDecimal("100");
                frequency = "Annual (by January)";
            }
            case "Tamil Nadu" -> {
                empShare = new BigDecimal("20");
                erShare = new BigDecimal("40");
                frequency = "Annual (by January)";
            }
            case "Andhra Pradesh" -> {
                empShare = new BigDecimal("30");
                erShare = new BigDecimal("70");
                frequency = "Annual (by January)";
            }
            case "Telangana" -> {
                empShare = new BigDecimal("2");
                erShare = new BigDecimal("5");
                frequency = "Annual (by January)";
            }
            case "Gujarat" -> {
                empShare = new BigDecimal("6");
                erShare = new BigDecimal("12");
                frequency = "Half-yearly (June & December)";
            }
            case "West Bengal" -> {
                empShare = new BigDecimal("3");
                erShare = new BigDecimal("30");
                frequency = "Half-yearly (June & December)";
            }
            case "Kerala" -> {
                empShare = new BigDecimal("20");
                erShare = new BigDecimal("20");
                frequency = "Monthly";
            }
            case "Madhya Pradesh" -> {
                empShare = new BigDecimal("10");
                erShare = new BigDecimal("30");
                frequency = "Half-yearly (June & December)";
            }
            case "Chhattisgarh" -> {
                empShare = new BigDecimal("15");
                erShare = new BigDecimal("45");
                frequency = "Half-yearly (June & December)";
            }
            case "Odisha" -> {
                empShare = new BigDecimal("10");
                erShare = new BigDecimal("20");
                frequency = "Half-yearly (June & December)";
            }
            case "Punjab" -> {
                empShare = new BigDecimal("5");
                erShare = new BigDecimal("20");
                frequency = "Monthly";
            }
            case "Haryana" -> {
                empShare = new BigDecimal("31");
                erShare = new BigDecimal("62");
                frequency = "Monthly";
            }
            case "Goa" -> {
                empShare = new BigDecimal("60");
                erShare = new BigDecimal("180");
                frequency = "Half-yearly (June & December)";
            }
            case "Chandigarh" -> {
                empShare = new BigDecimal("5");
                erShare = new BigDecimal("20");
                frequency = "Monthly";
            }
            case "Delhi" -> {
                empShare = new BigDecimal("0.75");
                erShare = new BigDecimal("2.25");
                frequency = "Half-yearly (June & December)";
            }
            default -> {
                result.put("applicable", false);
                result.put("reason", "LWF is not applicable in " + state +
                        ". Only 16 states have LWF Acts.");
                result.put("employeeContribution", BigDecimal.ZERO);
                result.put("employerContribution", BigDecimal.ZERO);
                return result;
            }
        }

        result.put("applicable", true);
        result.put("employeeContribution", empShare);
        result.put("employerContribution", erShare);
        result.put("totalContribution", empShare.add(erShare));
        result.put("frequency", frequency);
        result.put("note", "LWF is a fixed amount per employee, not " +
                "percentage-based. Rates as per latest state notifications.");

        return result;
    }
}