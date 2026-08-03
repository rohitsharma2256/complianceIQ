package com.complianceiq.service;

import com.complianceiq.model.*;
import com.complianceiq.repository.*;
import com.complianceiq.security.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ComplianceCheckService {

    private final EmployeeRepository employeeRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final ComplianceRecordRepository complianceRecordRepository;
    private final CompanyRepository companyRepository;
    private final StatutoryRuleService statutoryRuleService;
    private final AttendanceService attendanceService;
    private final AuditService auditService;
    private final AdminService adminService;

    // NOTE: EPF/ESI/PT rates ab StatutoryRuleService (DB) se aate hain - yahan hardcode nahi
    private static final BigDecimal BASIC_SALARY_MIN_PERCENT = new BigDecimal("0.50");

    public PayrollRun runComplianceCheck(UUID companyId, int month, int year) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        // Tenant isolation - dusri firm ka payroll na chala sake
        adminService.requireOwnCompany(company);

        List<Employee> employees = employeeRepository
                .findByCompanyIdAndIsActiveTrue(companyId);

        if (employees.isEmpty()) {
            throw new RuntimeException("No active employees found");
        }

        BigDecimal totalBasic       = BigDecimal.ZERO;
        BigDecimal totalEpfEmployee = BigDecimal.ZERO;
        BigDecimal totalEpfEmployer = BigDecimal.ZERO;
        BigDecimal totalEsiEmployee = BigDecimal.ZERO;
        BigDecimal totalEsiEmployer = BigDecimal.ZERO;
        BigDecimal totalTds         = BigDecimal.ZERO;
        BigDecimal totalPt          = BigDecimal.ZERO;
        BigDecimal totalLwf         = BigDecimal.ZERO;

        // Existing run update karo, ya naya banao (no duplicates)
        PayrollRun payrollRun = payrollRunRepository
                .findByCompanyIdAndMonthAndYear(companyId, month, year)
                .orElse(PayrollRun.builder()
                        .company(company)
                        .month(month)
                        .year(year)
                        .build());

        payrollRun.setTotalEmployees(employees.size());
        payrollRun.setStatus(PayrollRun.Status.PENDING);
        payrollRun = payrollRunRepository.save(payrollRun);

        // Purani violations hatao (fresh check)
        List<ComplianceRecord> old =
                complianceRecordRepository.findByPayrollRunId(payrollRun.getId());
        if (!old.isEmpty()) {
            complianceRecordRepository.deleteAll(old);
        }

        List<ComplianceRecord> violations = new ArrayList<>();

        for (Employee emp : employees) {

            /* ================= ATTENDANCE + LOP ================= */
            Attendance att     = attendanceService.getOrDefault(emp, month, year);
            BigDecimal factor  = att.getAttendanceFactor();      // 24/26 = 0.923
            BigDecimal lopDays = att.getLopDays();

            /* ================= CONTRACTED (full) amounts ================= */
            BigDecimal fullBasic = emp.getPfWageBase();
            BigDecimal fullGross = emp.getMonthlyGross();
            BigDecimal ctc       = nz(emp.getTotalCtc());
            String     state     = emp.getApplicableState();     // work state priority

            /* ================= EARNED amounts (LOP ke baad) ================= */
            // Yeh asli paid amount hai - PF/ESI contribution isi pe lagta hai
            BigDecimal earnedBasic = fullBasic.multiply(factor).setScale(2, RoundingMode.HALF_UP);
            BigDecimal earnedGross = fullGross.multiply(factor).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lopAmount   = fullGross.subtract(earnedGross);

            /* ================= PF (earned basic pe, Rs 15,000 ceiling) ================= */
            if (Boolean.TRUE.equals(emp.getPfApplicable())) {
                var pf = statutoryRuleService.calculatePf(earnedBasic);
                totalEpfEmployee = totalEpfEmployee.add(pf.employee());
                totalEpfEmployer = totalEpfEmployer.add(pf.employer());

                // Ceiling laga toh CA ko batao (warna woh sochega calculation galat hai)
                if (pf.cappedAtCeiling()) {
                    violations.add(ComplianceRecord.builder()
                            .payrollRun(payrollRun)
                            .employee(emp)
                            .violationType(ComplianceRecord.ViolationType.INFO)
                            .severity(ComplianceRecord.Severity.LOW)
                            .description(emp.getFullName() + "'s basic is Rs " + earnedBasic
                                    + ", but EPF is computed on the statutory wage ceiling of Rs "
                                    + pf.pfWage() + " (employee share Rs " + pf.employee() + ")")
                            .recommendedFix("No action needed - this is the statutory ceiling under the EPF Act.")
                            .build());
                }
            }

            /* ================= ESI =================
               Eligibility CONTRACTED gross pe (statutory rule),
               contribution EARNED gross pe (jo actually paid hua) */
            var esiCheck = statutoryRuleService.calculateEsi(fullGross);
            if (esiCheck.applicable()) {
                var esiActual = statutoryRuleService.calculateEsi(earnedGross);
                totalEsiEmployee = totalEsiEmployee.add(esiActual.employee());
                totalEsiEmployer = totalEsiEmployer.add(esiActual.employer());

                // ESI eligible hai par ESIC IP missing -> return file nahi ho sakta
                if (emp.getEsicIpNumber() == null || emp.getEsicIpNumber().isBlank()) {
                    violations.add(ComplianceRecord.builder()
                            .payrollRun(payrollRun)
                            .employee(emp)
                            .violationType(ComplianceRecord.ViolationType.MISSING_DATA)
                            .severity(ComplianceRecord.Severity.HIGH)
                            .description(emp.getFullName() + " is ESI-eligible (gross Rs "
                                    + fullGross + ") but the ESIC IP number is missing")
                            .recommendedFix("Add the ESIC IP number - the ESI return cannot be filed without it")
                            .build());
                }
            }

            /* ================= Professional Tax =================
               FULL gross pe - LOP se PT slab nahi badalta */
            if (Boolean.TRUE.equals(emp.getPtApplicable())) {
                totalPt = totalPt.add(
                        statutoryRuleService.calculateProfessionalTax(state, fullGross));
            }

            /* ================= LWF (sirf applicable states) ================= */
            var lwf = statutoryRuleService.calculateLwf(state);
            totalLwf = totalLwf.add(lwf.employee());

            /* ================= TDS ================= */
            BigDecimal annualIncome = ctc.multiply(new BigDecimal("12"));
            totalTds = totalTds.add(calculateTds(annualIncome));

            /* ================= LOP violation (CA ko dikhe) ================= */
            if (lopDays.compareTo(BigDecimal.ZERO) > 0) {
                violations.add(ComplianceRecord.builder()
                        .payrollRun(payrollRun)
                        .employee(emp)
                        .violationType(ComplianceRecord.ViolationType.INFO)
                        .severity(ComplianceRecord.Severity.LOW)
                        .description(emp.getFullName() + " has " + lopDays + " LOP day(s). "
                                + "Gross reduced from Rs " + fullGross + " to Rs " + earnedGross
                                + " (deduction Rs " + lopAmount + ")")
                        .recommendedFix("Verify the unpaid absence with the attendance register")
                        .build());
            }

            /* ================= Attendance record missing ================= */
            if (att.getId() == null) {
                violations.add(ComplianceRecord.builder()
                        .payrollRun(payrollRun)
                        .employee(emp)
                        .violationType(ComplianceRecord.ViolationType.MISSING_DATA)
                        .severity(ComplianceRecord.Severity.MEDIUM)
                        .description("No attendance record for " + emp.getFullName()
                                + " for " + month + "/" + year + " - full attendance assumed")
                        .recommendedFix("Enter attendance before finalising payroll")
                        .build());
            }

            /* ================= UAN missing (ECR export) ================= */
            if (Boolean.TRUE.equals(emp.getPfApplicable())
                    && (emp.getUanNumber() == null || emp.getUanNumber().isBlank())) {
                violations.add(ComplianceRecord.builder()
                        .payrollRun(payrollRun)
                        .employee(emp)
                        .violationType(ComplianceRecord.ViolationType.MISSING_DATA)
                        .severity(ComplianceRecord.Severity.HIGH)
                        .description(emp.getFullName() + " has no UAN - ECR export will fail")
                        .recommendedFix("Add the UAN from the EPFO portal before filing ECR")
                        .build());
            }

            /* ================= PAN missing (TDS / Form 16) ================= */
            if (emp.getPan() == null || emp.getPan().isBlank()) {
                violations.add(ComplianceRecord.builder()
                        .payrollRun(payrollRun)
                        .employee(emp)
                        .violationType(ComplianceRecord.ViolationType.MISSING_DATA)
                        .severity(ComplianceRecord.Severity.MEDIUM)
                        .description(emp.getFullName() + " has no PAN - Form 16 cannot be issued "
                                + "and TDS attracts the higher 20% rate")
                        .recommendedFix("Collect the PAN from the employee")
                        .build());
            }

            /* ================= Basic >= 50% CTC (Labour Code 2025) ================= */
            BigDecimal minBasicRequired = ctc.multiply(BASIC_SALARY_MIN_PERCENT);
            if (fullBasic.compareTo(minBasicRequired) < 0) {
                violations.add(ComplianceRecord.builder()
                        .payrollRun(payrollRun)
                        .employee(emp)
                        .violationType(ComplianceRecord.ViolationType.BASIC_SALARY_RULE)
                        .severity(ComplianceRecord.Severity.HIGH)
                        .description(emp.getFullName() + "'s basic salary is Rs " + fullBasic
                                + ". Minimum required is Rs " + minBasicRequired
                                + " (50% of CTC Rs " + ctc + " as per Labour Code 2025)")
                        .recommendedFix("Increase basic salary to Rs " + minBasicRequired
                                + ". Adjust allowances accordingly to maintain the same CTC.")
                        .build());
            }

            totalBasic = totalBasic.add(earnedBasic);   // earned basic - LOP ke baad
        }

        if (!violations.isEmpty()) {
            complianceRecordRepository.saveAll(violations);
        }

        payrollRun.setTotalBasicSalary(totalBasic);
        payrollRun.setTotalEpfEmployee(totalEpfEmployee);
        payrollRun.setTotalEpfEmployer(totalEpfEmployer);
        payrollRun.setTotalEsiEmployee(totalEsiEmployee);
        payrollRun.setTotalEsiEmployer(totalEsiEmployer);
        payrollRun.setTotalTds(totalTds);
        payrollRun.setTotalProfessionalTax(totalPt);
        // payrollRun.setTotalLwf(totalLwf);   // <-- PayrollRun mein LWF field add karne ke baad uncomment
        payrollRun.setStatus(PayrollRun.Status.PROCESSED);

        PayrollRun saved = payrollRunRepository.save(payrollRun);

        auditService.log(AuditLog.Action.PAYROLL_RUN, "PAYROLL_RUN", saved.getId(),
                "Compliance check run for " + company.getCompanyName()
                        + " (" + month + "/" + year + ") - " + employees.size()
                        + " employees, " + violations.size() + " finding(s)");

        return saved;
    }

    private BigDecimal calculateTds(BigDecimal annualIncome) {
        BigDecimal standardDeduction = new BigDecimal("75000");
        BigDecimal taxableIncome = annualIncome.subtract(standardDeduction);

        if (taxableIncome.compareTo(new BigDecimal("300000")) <= 0) {
            return BigDecimal.ZERO;
        } else if (taxableIncome.compareTo(new BigDecimal("700000")) <= 0) {
            BigDecimal tax = taxableIncome
                    .subtract(new BigDecimal("300000"))
                    .multiply(new BigDecimal("0.05"));
            return tax.divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
        } else {
            BigDecimal tax = new BigDecimal("20000")
                    .add(taxableIncome
                            .subtract(new BigDecimal("700000"))
                            .multiply(new BigDecimal("0.10")));
            return tax.divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
        }
    }

    /* ==================================================================
       READ / RESOLVE  -  sab pe tenant check
       ================================================================== */

    public List<ComplianceRecord> getViolations(UUID payrollRunId) {
        // getPayrollRun khud tenant verify karta hai
        getPayrollRun(payrollRunId);
        return complianceRecordRepository
                .findByPayrollRunIdAndIsResolvedFalse(payrollRunId);
    }

    public PayrollRun getPayrollRun(UUID payrollRunId) {
        PayrollRun run = payrollRunRepository.findById(payrollRunId)
                .orElseThrow(() -> new RuntimeException("Payroll run not found"));
        adminService.requireOwnCompany(run.getCompany());
        return run;
    }

    public List<PayrollRun> getPayrollHistory(UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));
        adminService.requireOwnCompany(company);
        return payrollRunRepository.findByCompanyId(companyId);
    }

    public ComplianceRecord resolveViolation(UUID violationId) {
        ComplianceRecord v = complianceRecordRepository.findById(violationId)
                .orElseThrow(() -> new RuntimeException("Violation not found"));

        // Violation apni firm ka hai ya nahi
        adminService.requireOwnCompany(v.getPayrollRun().getCompany());

        v.setIsResolved(true);
        ComplianceRecord saved = complianceRecordRepository.save(v);

        auditService.log(AuditLog.Action.VIOLATION_RESOLVED, "VIOLATION", saved.getId(),
                "Marked resolved: " + truncateDesc(saved.getDescription()));

        return saved;
    }

    /* ---------- helpers ---------- */
    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** Audit description bahut lamba na ho */
    private String truncateDesc(String s) {
        if (s == null) return "";
        return s.length() > 150 ? s.substring(0, 150) + "..." : s;
    }
}