package com.complianceiq.service;

import com.complianceiq.dto.EmployeeRequest;
import com.complianceiq.dto.EmployeeUpdateRequest;
import com.complianceiq.model.AuditLog;
import com.complianceiq.model.Company;
import com.complianceiq.model.Employee;
import com.complianceiq.repository.CompanyRepository;
import com.complianceiq.repository.EmployeeRepository;
import com.complianceiq.security.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final AuditService auditService;
    private final AdminService adminService;

    // ESI eligibility GROSS wages pe hai (CTC pe nahi) - statutory rule
    private static final BigDecimal ESI_WAGE_LIMIT = new BigDecimal("21000");

    /* ==================================================================
       CREATE
       ================================================================== */
    public Employee addEmployee(EmployeeRequest req) {

        Company company = companyRepository.findById(req.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found"));

        // Tenant isolation - dusri firm ki company mein employee add na kar sake
        adminService.requireOwnCompany(company);

        Employee emp = Employee.builder()
                .company(company)
                /* ---------- identity ---------- */
                .fullName(req.getFullName())
                .employeeCode(req.getEmployeeCode())
                .email(req.getEmail())
                .phone(req.getPhone())
                .pan(req.getPan())
                .uanNumber(req.getUanNumber())
                .esicIpNumber(req.getEsicIpNumber())
                .aadhaarNumber(req.getAadhaarNumber())
                /* ---------- dates ---------- */
                .dateOfBirth(req.getDateOfBirth())
                .dateOfJoining(req.getDateOfJoining())
                /* ---------- organisation ---------- */
                .designation(req.getDesignation())
                .department(req.getDepartment())
                .workLocation(req.getWorkLocation())
                // work state na diya ho toh company ka state (PT/LWF isi pe depend karta hai)
                .workState(req.getWorkState() != null && !req.getWorkState().isBlank()
                        ? req.getWorkState() : company.getState())
                /* ---------- bank ---------- */
                .bankName(req.getBankName())
                .bankAccountNumber(req.getBankAccountNumber())
                .bankIfsc(req.getBankIfsc())
                /* ---------- salary structure ---------- */
                .basicSalary(nz(req.getBasicSalary()))
                .hra(nz(req.getHra()))
                .conveyanceAllowance(nz(req.getConveyanceAllowance()))
                .specialAllowance(nz(req.getSpecialAllowance()))
                .medicalAllowance(nz(req.getMedicalAllowance()))
                .otherAllowance(nz(req.getOtherAllowance()))
                .totalCtc(nz(req.getTotalCtc()))
                /* ---------- flags ---------- */
                .pfApplicable(req.getPfApplicable() == null || req.getPfApplicable())
                .ptApplicable(req.getPtApplicable() == null || req.getPtApplicable())
                .taxRegime(req.getTaxRegime() != null ? req.getTaxRegime() : Employee.TaxRegime.NEW)
                .isActive(true)
                .build();

        // ESI eligibility GROSS pe decide hoti hai, CTC pe nahi
        emp.setIsEsiApplicable(emp.getMonthlyGross().compareTo(ESI_WAGE_LIMIT) <= 0);

        Employee saved = employeeRepository.save(emp);

        auditService.log(AuditLog.Action.CREATE, "EMPLOYEE", saved.getId(),
                "Added employee " + saved.getFullName()
                        + (saved.getEmployeeCode() == null
                        ? "" : " (" + saved.getEmployeeCode() + ")"));

        return saved;
    }

    /* ==================================================================
       UPDATE  -  null check ke saath (partial update safe)
       ================================================================== */
    public Employee updateEmployee(UUID employeeId, EmployeeUpdateRequest req) {

        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        adminService.requireOwnEmployee(emp);

        // Audit diff ke liye purane values - save se PEHLE capture karna zaroori
        Object oldBasic  = emp.getBasicSalary();
        Object oldCtc    = emp.getTotalCtc();
        Object oldState  = emp.getWorkState();
        Object oldRegime = emp.getTaxRegime();

        /* ---------- identity ---------- */
        if (req.getFullName() != null)          emp.setFullName(req.getFullName());
        if (req.getEmployeeCode() != null)      emp.setEmployeeCode(req.getEmployeeCode());
        if (req.getEmail() != null)             emp.setEmail(req.getEmail());
        if (req.getPhone() != null)             emp.setPhone(req.getPhone());
        if (req.getPan() != null)               emp.setPan(req.getPan());
        if (req.getUanNumber() != null)         emp.setUanNumber(req.getUanNumber());
        if (req.getEsicIpNumber() != null)      emp.setEsicIpNumber(req.getEsicIpNumber());
        if (req.getAadhaarNumber() != null)     emp.setAadhaarNumber(req.getAadhaarNumber());

        /* ---------- dates ---------- */
        if (req.getDateOfBirth() != null)       emp.setDateOfBirth(req.getDateOfBirth());
        if (req.getDateOfJoining() != null)     emp.setDateOfJoining(req.getDateOfJoining());

        /* ---------- organisation ---------- */
        if (req.getDesignation() != null)       emp.setDesignation(req.getDesignation());
        if (req.getDepartment() != null)        emp.setDepartment(req.getDepartment());
        if (req.getWorkLocation() != null)      emp.setWorkLocation(req.getWorkLocation());
        if (req.getWorkState() != null)         emp.setWorkState(req.getWorkState());

        /* ---------- bank ---------- */
        if (req.getBankName() != null)          emp.setBankName(req.getBankName());
        if (req.getBankAccountNumber() != null) emp.setBankAccountNumber(req.getBankAccountNumber());
        if (req.getBankIfsc() != null)          emp.setBankIfsc(req.getBankIfsc());

        /* ---------- salary structure ---------- */
        if (req.getBasicSalary() != null)         emp.setBasicSalary(req.getBasicSalary());
        if (req.getHra() != null)                 emp.setHra(req.getHra());
        if (req.getConveyanceAllowance() != null) emp.setConveyanceAllowance(req.getConveyanceAllowance());
        if (req.getSpecialAllowance() != null)    emp.setSpecialAllowance(req.getSpecialAllowance());
        if (req.getMedicalAllowance() != null)    emp.setMedicalAllowance(req.getMedicalAllowance());
        if (req.getOtherAllowance() != null)      emp.setOtherAllowance(req.getOtherAllowance());
        if (req.getTotalCtc() != null)            emp.setTotalCtc(req.getTotalCtc());

        /* ---------- flags ---------- */
        if (req.getPfApplicable() != null)      emp.setPfApplicable(req.getPfApplicable());
        if (req.getPtApplicable() != null)      emp.setPtApplicable(req.getPtApplicable());
        if (req.getTaxRegime() != null)         emp.setTaxRegime(req.getTaxRegime());

        // Salary badli toh ESI eligibility dobara evaluate karo (GROSS pe)
        emp.setIsEsiApplicable(emp.getMonthlyGross().compareTo(ESI_WAGE_LIMIT) <= 0);

        Employee saved = employeeRepository.save(emp);

        // Field-level diff - CA ko yeh dikhta hai audit log mein
        String changes = auditService.joinDiffs(
                auditService.diff("basicSalary", oldBasic,  saved.getBasicSalary()),
                auditService.diff("totalCtc",    oldCtc,    saved.getTotalCtc()),
                auditService.diff("workState",   oldState,  saved.getWorkState()),
                auditService.diff("taxRegime",   oldRegime, saved.getTaxRegime()));

        auditService.logChange(AuditLog.Action.UPDATE, "EMPLOYEE", saved.getId(),
                "Updated employee " + saved.getFullName(), changes);

        return saved;
    }

    /* ==================================================================
       READ / DELETE
       ================================================================== */
    public List<Employee> getEmployeesByCompany(UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));
        adminService.requireOwnCompany(company);
        return employeeRepository.findByCompanyIdAndIsActiveTrue(companyId);
    }

    public Employee getEmployee(UUID employeeId) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        adminService.requireOwnEmployee(emp);
        return emp;
    }

    /** Soft delete - historical payroll records break nahi hote */
    public void deleteEmployee(UUID employeeId) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        adminService.requireOwnEmployee(emp);

        emp.setIsActive(false);
        employeeRepository.save(emp);

        auditService.log(AuditLog.Action.DELETE, "EMPLOYEE", emp.getId(),
                "Removed employee " + emp.getFullName());
    }

    /* ---------- helper ---------- */
    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}