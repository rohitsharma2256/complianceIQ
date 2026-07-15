package com.complianceiq.service;

import com.complianceiq.model.*;
import com.complianceiq.repository.*;
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

    private static final BigDecimal EPF_RATE = new BigDecimal("0.12");
    private static final BigDecimal ESI_EMPLOYEE_RATE = new BigDecimal("0.0075");
    private static final BigDecimal ESI_EMPLOYER_RATE = new BigDecimal("0.0325");
    private static final BigDecimal ESI_WAGE_LIMIT = new BigDecimal("21000");
    private static final BigDecimal BASIC_SALARY_MIN_PERCENT = new BigDecimal("0.50");

    public PayrollRun runComplianceCheck(UUID companyId, int month, int year) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        List<Employee> employees = employeeRepository
                .findByCompanyIdAndIsActiveTrue(companyId);

        if (employees.isEmpty()) {
            throw new RuntimeException("No active employees found");
        }

        BigDecimal totalBasic = BigDecimal.ZERO;
        BigDecimal totalEpfEmployee = BigDecimal.ZERO;
        BigDecimal totalEpfEmployer = BigDecimal.ZERO;
        BigDecimal totalEsiEmployee = BigDecimal.ZERO;
        BigDecimal totalEsiEmployer = BigDecimal.ZERO;
        BigDecimal totalTds = BigDecimal.ZERO;
        BigDecimal totalPt = BigDecimal.ZERO;

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

            BigDecimal basic = emp.getBasicSalary();
            BigDecimal ctc = emp.getTotalCtc();

            // 50% Basic Salary Rule
            BigDecimal minBasicRequired = ctc.multiply(BASIC_SALARY_MIN_PERCENT);
            if (basic.compareTo(minBasicRequired) < 0) {
                violations.add(ComplianceRecord.builder()
                        .payrollRun(payrollRun)
                        .employee(emp)
                        .violationType(ComplianceRecord.ViolationType.BASIC_SALARY_RULE)
                        .severity(ComplianceRecord.Severity.HIGH)
                        .description(emp.getFullName() + "'s basic salary is Rs " + basic +
                                ". Minimum required is Rs " + minBasicRequired +
                                " (50% of CTC Rs " + ctc + " as per Labour Code 2025)")
                        .recommendedFix("Increase basic salary to Rs " + minBasicRequired +
                                ". Adjust allowances accordingly to maintain same CTC.")
                        .build());
            }

            // EPF
            if (Boolean.TRUE.equals(emp.getIsEpfApplicable())) {
                BigDecimal empEpf = basic.multiply(EPF_RATE)
                        .setScale(2, RoundingMode.HALF_UP);
                totalEpfEmployee = totalEpfEmployee.add(empEpf);
                totalEpfEmployer = totalEpfEmployer.add(empEpf);
            }

            // ESI
            if (ctc.compareTo(ESI_WAGE_LIMIT) <= 0) {
                BigDecimal empEsi = ctc.multiply(ESI_EMPLOYEE_RATE)
                        .setScale(2, RoundingMode.HALF_UP);
                BigDecimal erEsi = ctc.multiply(ESI_EMPLOYER_RATE)
                        .setScale(2, RoundingMode.HALF_UP);
                totalEsiEmployee = totalEsiEmployee.add(empEsi);
                totalEsiEmployer = totalEsiEmployer.add(erEsi);
            }

            // Professional Tax
            totalPt = totalPt.add(calculateProfessionalTax(ctc, emp.getWorkState()));

            // TDS
            BigDecimal annualIncome = ctc.multiply(new BigDecimal("12"));
            totalTds = totalTds.add(calculateTds(annualIncome));

            totalBasic = totalBasic.add(basic);
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
        payrollRun.setStatus(PayrollRun.Status.PROCESSED);

        return payrollRunRepository.save(payrollRun);
    }

    private BigDecimal calculateProfessionalTax(BigDecimal salary, String state) {
        if (state == null) return BigDecimal.ZERO;

        if (state.equalsIgnoreCase("Maharashtra")) {
            if (salary.compareTo(new BigDecimal("7500")) <= 0) return BigDecimal.ZERO;
            else if (salary.compareTo(new BigDecimal("10000")) <= 0) return new BigDecimal("175");
            else return new BigDecimal("200");
        }
        return BigDecimal.ZERO;
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

    public List<ComplianceRecord> getViolations(UUID payrollRunId) {
        return complianceRecordRepository
                .findByPayrollRunIdAndIsResolvedFalse(payrollRunId);
    }

    public PayrollRun getPayrollRun(UUID payrollRunId) {
        return payrollRunRepository.findById(payrollRunId)
                .orElseThrow(() -> new RuntimeException("Payroll run not found"));
    }

    public List<PayrollRun> getPayrollHistory(UUID companyId) {
        return payrollRunRepository.findByCompanyId(companyId);
    }

    public ComplianceRecord resolveViolation(UUID violationId) {
        ComplianceRecord v = complianceRecordRepository.findById(violationId)
                .orElseThrow(() -> new RuntimeException("Violation not found"));
        v.setIsResolved(true);
        return complianceRecordRepository.save(v);
    }
}