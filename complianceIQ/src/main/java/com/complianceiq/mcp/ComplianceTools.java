package com.complianceiq.mcp;

import com.complianceiq.model.PayrollRun;
import com.complianceiq.model.ComplianceRecord;
import com.complianceiq.service.ComplianceCheckService;
import com.complianceiq.repository.CompanyRepository;
import com.complianceiq.service.DeadlineService;
import com.complianceiq.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ComplianceTools {

    private final ComplianceCheckService complianceCheckService;
    private final CompanyRepository companyRepository;
    private final DeadlineService deadlineService;
    //private final ReportService reportService;

    @Tool(description = "Run a payroll compliance check for a company. " +
            "Returns calculations AND a PayrollRun ID. " +
            "IMPORTANT: Save the PayrollRun ID from the result - " +
            "you need it to fetch violations using getViolations tool.")
    public String checkPayrollCompliance(
            @ToolParam(description = "Company UUID") String companyId,
            @ToolParam(description = "Month number 1-12") int month,
            @ToolParam(description = "Year e.g. 2026") int year) {

        try {
            PayrollRun result = complianceCheckService
                    .runComplianceCheck(UUID.fromString(companyId), month, year);

            return String.format("""
                    Compliance check completed for %s:
                    Total Employees: %d
                    Total EPF (Employee): Rs %s
                    Total ESI (Employee): Rs %s
                    Total TDS: Rs %s
                    Total Professional Tax: Rs %s
                    Status: %s
                    """,
                    result.getCompany().getCompanyName(),
                    result.getTotalEmployees(),
                    result.getTotalEpfEmployee(),
                    result.getTotalEsiEmployee(),
                    result.getTotalTds(),
                    result.getTotalProfessionalTax(),
                    result.getStatus());
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get all compliance violations for a payroll run. " +
            "Returns list of employees violating rules with fixes.")
    public String getViolations(
            @ToolParam(description = "Payroll run UUID") String payrollRunId) {

        List<ComplianceRecord> violations = complianceCheckService
                .getViolations(UUID.fromString(payrollRunId));

        if (violations.isEmpty()) {
            return "No violations found. All compliant.";
        }

        StringBuilder sb = new StringBuilder("Violations found:\n");
        for (ComplianceRecord v : violations) {
            sb.append(String.format("- %s: %s (Fix: %s)\n",
                    v.getViolationType(),
                    v.getDescription(),
                    v.getRecommendedFix()));
        }
        return sb.toString();
    }

    @Tool(description = "List all client companies for a tenant (CA firm).")
    public String listCompanies(
            @ToolParam(description = "Tenant UUID") String tenantId) {

        var companies = companyRepository.findByTenantId(UUID.fromString(tenantId));
        if (companies.isEmpty()) return "No companies found.";

        StringBuilder sb = new StringBuilder("Companies:\n");
        companies.forEach(c -> sb.append(String.format(
                "- %s (ID: %s, State: %s)\n",
                c.getCompanyName(), c.getId(), c.getState())));
        return sb.toString();
    }

    @Tool(description = "Get upcoming compliance deadlines for TDS, EPF and ESI " +
            "for the current month. Returns each deadline with the due date " +
            "and number of days remaining. Present these deadlines clearly to the user.")
    public String getUpcomingDeadlines() {
        List<String> deadlines = deadlineService.getUpcomingDeadlines();
        if (deadlines.isEmpty()) {
            return "No upcoming deadlines this month.";
        }
        return "Upcoming deadlines:\n" + String.join("\n", deadlines);
    }


    @Tool(description = "Run compliance check AND get violations in one step. " +
            "Use this when user wants both the check results and violations. " +
            "Returns complete compliance report with all violations.")
    public String checkComplianceWithViolations(
            @ToolParam(description = "The company UUID") String companyId,
            @ToolParam(description = "Month number 1-12") int month,
            @ToolParam(description = "Year e.g. 2026") int year) {

        try {
            // Step 1: Compliance check
            PayrollRun result = complianceCheckService.runComplianceCheck(
                    UUID.fromString(companyId), month, year);

            // Step 2: Violations same method mein fetch karo
            List<ComplianceRecord> violations = complianceCheckService
                    .getViolations(result.getId());

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("""
                Compliance Report for %s (Month %d/%d):
                - Total Employees: %d
                - Total EPF Employee: Rs %s
                - Total ESI Employee: Rs %s
                - Total TDS: Rs %s
                - Total Professional Tax: Rs %s
                - Status: %s

                """,
                    result.getCompany().getCompanyName(),
                    month, year,
                    result.getTotalEmployees(),
                    result.getTotalEpfEmployee(),
                    result.getTotalEsiEmployee(),
                    result.getTotalTds(),
                    result.getTotalProfessionalTax(),
                    result.getStatus()));

            if (violations.isEmpty()) {
                sb.append("No violations found. All employees compliant.");
            } else {
                sb.append(String.format("VIOLATIONS FOUND (%d):\n", violations.size()));
                for (ComplianceRecord v : violations) {
                    sb.append(String.format("- %s: %s (Fix: %s)\n",
                            v.getViolationType(),
                            v.getDescription(),
                            v.getRecommendedFix()));
                }
            }

            return sb.toString();

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Generate a compliance audit report for a company " +
            "for a specific month and year. Runs the compliance check and " +
            "returns a download link. IMPORTANT: Always include the full " +
            "download path (/api/reports/compliance/...) in your answer " +
            "so the user can download the PDF.")
    public String generateComplianceReport(
            @ToolParam(description = "The company UUID") String companyId,
            @ToolParam(description = "Month number 1-12") int month,
            @ToolParam(description = "Year e.g. 2026") int year) {

        try {
            var run = complianceCheckService.runComplianceCheck(
                    java.util.UUID.fromString(companyId), month, year);

            return String.format("""
                Compliance report is ready for %s (Month %d/%d).

                Summary:
                - Total Employees: %d
                - Total EPF: Rs %s
                - Total ESI: Rs %s
                - Total TDS: Rs %s
                - Total Professional Tax: Rs %s
                - Status: %s

                Download the full PDF report at:
                /api/reports/compliance/%s
                """,
                    run.getCompany().getCompanyName(),
                    month, year,
                    run.getTotalEmployees(),
                    run.getTotalEpfEmployee(),
                    run.getTotalEsiEmployee(),
                    run.getTotalTds(),
                    run.getTotalProfessionalTax(),
                    run.getStatus(),
                    run.getId());

        } catch (Exception e) {
            return "Error generating report: " + e.getMessage();
        }
    }
}