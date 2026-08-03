package com.complianceiq.mcp;

import com.complianceiq.model.ComplianceRecord;
import com.complianceiq.model.PayrollRun;
import com.complianceiq.model.StatutoryRule;
import com.complianceiq.repository.CompanyRepository;
import com.complianceiq.repository.StatutoryRuleRepository;
import com.complianceiq.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * AI tools. Sirf 4 exposed hain - 8B model bade tool surface pe confuse hota hai.
 * Deterministic actions (check, violations, report, deadlines) UI buttons se hote
 * hain - unhe LLM se karwana slow, mehnga aur bekaar tha.
 */
@Service
@RequiredArgsConstructor
public class ComplianceTools {

    private final ComplianceCheckService complianceCheckService;
    private final CompanyRepository companyRepository;
    private final DeadlineService deadlineService;
    private final StatutoryRuleRepository ruleRepository;
    private final StatutoryRuleService statutoryRuleService;
    private final AttendanceService attendanceService;

    /* ==================================================================
       TOOL 1 - PT slabs. Salary OPTIONAL.
       Yeh loop bug ka fix hai: "PT of Maharashtra?" pe model salary
       guess karke infinite loop mein fas raha tha.
       ================================================================== */
    @Tool(description = "Get Professional Tax slabs for a state. No salary needed.")
    public String getProfessionalTaxSlabs(
            @ToolParam(description = "State name") String state) {
        try {
            var slabs = ruleRepository.findByRuleTypeAndStateIgnoreCase(
                    StatutoryRule.RuleType.PT, state.trim());

            if (slabs.isEmpty())
                return "No Professional Tax rule is configured for " + state + ".";

            boolean applicable = slabs.stream().anyMatch(StatutoryRule::getApplicable);
            if (!applicable)
                return "Professional Tax is NOT levied in " + state
                        + ". The deduction is zero for employees working there.";

            StringBuilder sb = new StringBuilder("Professional Tax slabs for " + state + ":\n");
            slabs.stream().filter(StatutoryRule::getApplicable).forEach(r ->
                    sb.append("- Monthly gross ").append(r.getSlabMin())
                            .append(r.getSlabMax() == null ? " and above" : " to " + r.getSlabMax())
                            .append(": Rs ").append(r.getAmount()).append("/month\n"));
            return sb.toString();
        } catch (Exception e) {
            return "Could not fetch PT slabs: " + e.getMessage();
        }
    }

    /* ==================================================================
       TOOL 2 - Statutory explain (jab salary di gayi ho)
       ================================================================== */
    @Tool(description = "Explain PT, PF, ESI, LWF amounts for a state and salary.")
    public String explainStatutoryDeductions(
            @ToolParam(description = "State name") String state,
            @ToolParam(description = "Monthly basic salary") String basicSalary,
            @ToolParam(description = "Monthly gross salary") String grossSalary) {
        try {
            BigDecimal basic = new BigDecimal(basicSalary);
            BigDecimal gross = new BigDecimal(grossSalary);

            var pf  = statutoryRuleService.calculatePf(basic);
            var esi = statutoryRuleService.calculateEsi(gross);
            var lwf = statutoryRuleService.calculateLwf(state);
            BigDecimal pt = statutoryRuleService.calculateProfessionalTax(state, gross);

            StringBuilder sb = new StringBuilder();
            sb.append("Deductions for ").append(state)
                    .append(" (basic Rs ").append(basic)
                    .append(", gross Rs ").append(gross).append("):\n");

            sb.append("PF: employee Rs ").append(pf.employee())
                    .append(", employer Rs ").append(pf.employer());
            if (pf.cappedAtCeiling())
                sb.append(" (on the statutory ceiling of Rs ").append(pf.pfWage()).append(")");
            sb.append("\n");

            sb.append("ESI: ").append(esi.applicable()
                    ? "employee Rs " + esi.employee() + ", employer Rs " + esi.employer()
                    : "not applicable - gross exceeds the Rs 21,000 threshold").append("\n");

            sb.append("Professional Tax: Rs ").append(pt);
            if (pt.compareTo(BigDecimal.ZERO) == 0)
                sb.append(" - not levied in ").append(state).append(", or a nil slab");
            sb.append("\n");

            sb.append("LWF: ").append(lwf.applicable()
                    ? "employee Rs " + lwf.employee() + ", employer Rs " + lwf.employer()
                    : "not applicable in " + state);

            return sb.toString();
        } catch (Exception e) {
            return "Could not explain deductions: " + e.getMessage();
        }
    }

    /* ==================================================================
       TOOL 3 - Attendance summary (LOP details cap kiye - tokens bachate hain)
       ================================================================== */
    @Tool(description = "Get attendance summary: entered, missing, LOP days.")
    public String getAttendanceSummary(
            @ToolParam(description = "Company UUID") String companyId,
            @ToolParam(description = "Month 1-12") int month,
            @ToolParam(description = "Year") int year) {
        try {
            var s = attendanceService.getSummary(UUID.fromString(companyId), month, year);

            StringBuilder sb = new StringBuilder();
            sb.append("Attendance ").append(month).append("/").append(year)
                    .append(": ").append(s.recordsEntered()).append(" entered, ")
                    .append(s.recordsMissing()).append(" missing, ")
                    .append(s.employeesWithLop()).append(" with LOP (")
                    .append(s.totalLopDays()).append(" days total).");

            if (!s.lopDetails().isEmpty()) {
                sb.append("\nLOP: ");
                s.lopDetails().stream().limit(5)
                        .forEach(d -> sb.append(d).append("; "));
                if (s.lopDetails().size() > 5)
                    sb.append("and ").append(s.lopDetails().size() - 5).append(" more");
            }
            if (s.recordsMissing() > 0)
                sb.append("\nFull attendance will be assumed for the missing records.");

            return sb.toString();
        } catch (Exception e) {
            return "Could not fetch attendance summary: " + e.getMessage();
        }
    }

    /* ==================================================================
       TOOL 4 - Ready to file. Bounded multi-step: steps CODE mein fixed hain,
       model sirf decide karta hai kab chalana hai. Compliance mein sequence
       deterministic aur auditable honi chahiye.
       ================================================================== */
    @Tool(description = "Check if a company is ready to file returns for a month. "
            + "Runs attendance, compliance and blocker checks together.")
    public String readyToFile(
            @ToolParam(description = "Company UUID") String companyId,
            @ToolParam(description = "Month 1-12") int month,
            @ToolParam(description = "Year") int year) {
        try {
            UUID id = UUID.fromString(companyId);
            StringBuilder sb = new StringBuilder();

            // STEP 1 - attendance completeness
            var att = attendanceService.getSummary(id, month, year);
            sb.append("Step 1 - Attendance: ").append(att.recordsEntered())
                    .append(" entered, ").append(att.recordsMissing()).append(" missing\n");
            if (att.employeesWithLop() > 0)
                sb.append("  ").append(att.employeesWithLop())
                        .append(" employee(s) have LOP - PF and ESI reduce accordingly\n");

            // STEP 2 - compliance run (attendance ke BAAD, order matters)
            PayrollRun run = complianceCheckService.runComplianceCheck(id, month, year);
            sb.append("Step 2 - Compliance computed for ")
                    .append(run.getTotalEmployees()).append(" employees\n");

            // STEP 3 - filing blockers
            List<ComplianceRecord> v = complianceCheckService.getViolations(run.getId());
            long blockers = v.stream()
                    .filter(x -> x.getSeverity() == ComplianceRecord.Severity.HIGH)
                    .count();
            sb.append("Step 3 - Filing blockers: ").append(blockers).append("\n\n");

            v.stream()
                    .filter(x -> x.getSeverity() == ComplianceRecord.Severity.HIGH)
                    .limit(8)
                    .forEach(x -> sb.append("• ").append(x.getDescription())
                            .append("\n  Action: ").append(x.getRecommendedFix()).append("\n"));

            sb.append("\nTotals: EPF Rs ").append(run.getTotalEpfEmployee())
                    .append(" | ESI Rs ").append(run.getTotalEsiEmployee())
                    .append(" | TDS Rs ").append(run.getTotalTds())
                    .append(" | PT Rs ").append(run.getTotalProfessionalTax());

            sb.append(blockers == 0
                    ? "\n\nVERDICT: Ready to file."
                    : "\n\nVERDICT: Not ready - resolve the blockers above first.");
            return sb.toString();
        } catch (Exception e) {
            return "Could not complete the review: " + e.getMessage();
        }
    }

    /* ==================================================================
       BELOW: @Tool hataya gaya - yeh UI buttons se hote hain (0 tokens).
       Methods rakhe hain kyunki controllers/services inhe call karte hain.
       ================================================================== */

    // Button: "Check Compliance"
    public String checkPayrollCompliance(String companyId, int month, int year) {
        try {
            PayrollRun r = complianceCheckService
                    .runComplianceCheck(UUID.fromString(companyId), month, year);
            return String.format("""
                    Compliance check completed for %s:
                    Employees: %d | EPF: Rs %s | ESI: Rs %s | TDS: Rs %s | PT: Rs %s
                    Status: %s""",
                    r.getCompany().getCompanyName(), r.getTotalEmployees(),
                    r.getTotalEpfEmployee(), r.getTotalEsiEmployee(),
                    r.getTotalTds(), r.getTotalProfessionalTax(), r.getStatus());
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // Button: "Show Violations"
    public String getViolations(String payrollRunId) {
        List<ComplianceRecord> violations = complianceCheckService
                .getViolations(UUID.fromString(payrollRunId));
        if (violations.isEmpty()) return "No violations found. All compliant.";

        StringBuilder sb = new StringBuilder("Violations found:\n");
        violations.forEach(v -> sb.append(String.format("- %s: %s (Fix: %s)\n",
                v.getViolationType(), v.getDescription(), v.getRecommendedFix())));
        return sb.toString();
    }

    // Button: "Generate Report"
    public String generateComplianceReport(String companyId, int month, int year) {
        try {
            var run = complianceCheckService.runComplianceCheck(
                    UUID.fromString(companyId), month, year);
            return "Report ready. Download at /api/reports/compliance/" + run.getId();
        } catch (Exception e) {
            return "Error generating report: " + e.getMessage();
        }
    }

    // Button: "Upcoming Deadlines"
    public String getUpcomingDeadlines() {
        List<String> deadlines = deadlineService.getUpcomingDeadlines();
        return deadlines.isEmpty()
                ? "No upcoming deadlines this month."
                : "Upcoming deadlines:\n" + String.join("\n", deadlines);
    }

    // UI se hota hai - AI ko write action nahi dena
    public String markFullAttendance(String companyId, int month, int year) {
        try {
            int n = attendanceService.markFullAttendanceForCompany(
                    UUID.fromString(companyId), month, year);
            int wd = attendanceService.calculateWorkingDays(month, year);
            return "Marked full attendance (" + wd + " working days) for " + n
                    + " employees for " + month + "/" + year + ".";
        } catch (Exception e) {
            return "Could not mark attendance: " + e.getMessage();
        }
    }

    // Sidebar se company select hoti hai - AI ko tenantId pata nahi hota,
    // isliye @Tool nahi (warna model UUID guess karta tha)
    public String listCompanies(String tenantId) {
        var companies = companyRepository.findByTenantId(UUID.fromString(tenantId));
        if (companies.isEmpty()) return "No companies found.";
        StringBuilder sb = new StringBuilder("Companies:\n");
        companies.forEach(c -> sb.append(String.format("- %s (ID: %s, State: %s)\n",
                c.getCompanyName(), c.getId(), c.getState())));
        return sb.toString();
    }
}