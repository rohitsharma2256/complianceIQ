package com.complianceiq.service;

import com.complianceiq.model.Attendance;
import com.complianceiq.model.Company;
import com.complianceiq.model.Employee;
import com.complianceiq.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * EPFO ECR (Electronic Challan cum Return) file generator.
 *
 * Format: pipe-delimited text, one line per member.
 * EPFO portal galat file reject kar deta hai, isliye pehle VALIDATE karte
 * hain aur CA ko exact problems batate hain - upload ke baad reject hone
 * se accha hai pehle hi pata chal jaaye.
 */
@Service
@RequiredArgsConstructor
public class EcrService {

    private final EmployeeRepository employeeRepository;
    private final StatutoryRuleService statutoryRuleService;
    private final AttendanceService attendanceService;

    private static final BigDecimal EPS_CEILING = new BigDecimal("15000");
    private static final BigDecimal EPS_RATE    = new BigDecimal("8.33");

    /* ==================================================================
       VALIDATION - export se PEHLE
       ================================================================== */
    public EcrValidation validate(UUID companyId, int month, int year) {
        List<Employee> employees = employeeRepository.findByCompanyIdAndIsActiveTrue(companyId);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (employees.isEmpty())
            errors.add("No active employees found for this company.");

        int eligible = 0;
        for (Employee e : employees) {
            if (!Boolean.TRUE.equals(e.getPfApplicable())) continue;
            eligible++;

            String who = e.getFullName()
                    + (e.getEmployeeCode() == null ? "" : " (" + e.getEmployeeCode() + ")");

            /* --- UAN: mandatory, exactly 12 digits --- */
            String uan = e.getUanNumber();
            if (uan == null || uan.isBlank())
                errors.add(who + ": UAN is missing. EPFO will reject the file.");
            else if (!uan.trim().matches("\\d{12}"))
                errors.add(who + ": UAN '" + uan + "' must be exactly 12 digits.");

            /* --- Name: EPFO letters aur space hi accept karta hai --- */
            if (e.getFullName() == null || e.getFullName().isBlank())
                errors.add("An employee has no name recorded.");
            else if (!e.getFullName().matches("[a-zA-Z .]+"))
                warnings.add(who + ": name contains characters EPFO may reject "
                        + "(only letters, spaces and dots are accepted).");

            /* --- Wages --- */
            BigDecimal basic = nz(e.getBasicSalary());
            if (basic.compareTo(BigDecimal.ZERO) <= 0)
                errors.add(who + ": basic salary is zero or not set.");

            /* --- Statutory minimum wage sanity --- */
            if (basic.compareTo(new BigDecimal("176")) < 0
                    && basic.compareTo(BigDecimal.ZERO) > 0)
                warnings.add(who + ": basic wage looks unusually low - verify before filing.");
        }

        if (eligible == 0 && !employees.isEmpty())
            warnings.add("No employees are marked EPF-applicable for this company.");

        return new EcrValidation(errors.isEmpty(), eligible, errors, warnings);
    }

    /* ==================================================================
       ECR FILE
       ================================================================== */
    public byte[] generateEcrFile(UUID companyId, int month, int year) {

        if (YearMonth.of(year, month).isAfter(YearMonth.now()))
            throw new RuntimeException("ECR cannot be generated for a future period.");

        EcrValidation v = validate(companyId, month, year);
        if (!v.valid())
            throw new RuntimeException("ECR file has " + v.errors().size()
                    + " blocking issue(s). Fix them before exporting:\n- "
                    + String.join("\n- ", v.errors()));

        List<Employee> employees = employeeRepository.findByCompanyIdAndIsActiveTrue(companyId);
        StringBuilder sb = new StringBuilder();

        for (Employee e : employees) {
            if (!Boolean.TRUE.equals(e.getPfApplicable())) continue;

            Attendance att = attendanceService.getOrDefault(e, month, year);
            BigDecimal factor = att.getAttendanceFactor();

            // NCP = Non-Contributory Period days (LOP)
            int ncpDays = att.getLopDays().setScale(0, RoundingMode.HALF_UP).intValue();

            BigDecimal grossWages = e.getMonthlyGross()
                    .multiply(factor).setScale(0, RoundingMode.HALF_UP);

            // EPF wages: basic (earned), ceiling ke andar
            BigDecimal earnedBasic = nz(e.getBasicSalary())
                    .multiply(factor).setScale(0, RoundingMode.HALF_UP);

            var pf = statutoryRuleService.calculatePf(earnedBasic);
            BigDecimal epfWages = pf.pfWage().setScale(0, RoundingMode.HALF_UP);

            // EPS wages: ceiling se zyada nahi
            BigDecimal epsWages  = epfWages.min(EPS_CEILING);
            BigDecimal edliWages = epsWages;

            BigDecimal epfContri = pf.employee().setScale(0, RoundingMode.HALF_UP);
            BigDecimal epsContri = epsWages.multiply(EPS_RATE)
                    .divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP);
            // Employer share ka woh hissa jo EPF mein jaata hai (EPS ke baad)
            BigDecimal diff = pf.employer().setScale(0, RoundingMode.HALF_UP)
                    .subtract(epsContri).max(BigDecimal.ZERO);

            sb.append(e.getUanNumber().trim()).append("|")
                    .append(cleanName(e.getFullName())).append("|")
                    .append(grossWages).append("|")
                    .append(epfWages).append("|")
                    .append(epsWages).append("|")
                    .append(edliWages).append("|")
                    .append(epfContri).append("|")
                    .append(epsContri).append("|")
                    .append(diff).append("|")
                    .append(ncpDays).append("|")
                    .append("0")                      // refund of advances
                    .append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Filename EPFO convention: ECR_<month><year>.txt */
    public String buildFileName(int month, int year) {
        return "ECR_" + Month.of(month).name().substring(0, 3) + year + ".txt";
    }

    /* ---------------- helpers ---------------- */
    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    /** EPFO uppercase letters/space accept karta hai */
    private String cleanName(String name) {
        return name.toUpperCase().replaceAll("[^A-Z .]", "").trim();
    }

    public record EcrValidation(boolean valid, int eligibleMembers,
                                List<String> errors, List<String> warnings) {}
}