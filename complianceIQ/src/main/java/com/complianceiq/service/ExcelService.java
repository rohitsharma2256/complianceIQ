package com.complianceiq.service;

import com.complianceiq.model.Company;
import com.complianceiq.model.Employee;
import com.complianceiq.repository.CompanyRepository;
import com.complianceiq.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExcelService {

    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;

    // ESI eligibility GROSS wages pe hai, CTC pe nahi
    private static final BigDecimal ESI_WAGE_LIMIT = new BigDecimal("21000");

    /*
     * EXCEL TEMPLATE - COLUMN ORDER (row 1 = header, data row 2 se)
     *  0  Employee Code       1  Full Name          2  Email
     *  3  Phone               4  PAN                5  UAN
     *  6  ESIC IP Number      7  Date of Birth      8  Date of Joining
     *  9  Designation        10  Department        11  Work State
     * 12  Bank Name          13  Account Number    14  IFSC
     * 15  Basic Salary       16  HRA               17  Conveyance
     * 18  Special Allowance  19  Medical Allowance 20  Other Allowance
     * 21  Total CTC          22  Tax Regime (OLD/NEW)
     */
    public String uploadEmployees(UUID companyId, MultipartFile file) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        List<Employee> employees = new ArrayList<>();
        int skippedRows = 0;

        try (Workbook workbook = WorkbookFactory.create(
                new ByteArrayInputStream(file.getBytes()))) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // Full name column 1 pe hai - blank toh row skip
                String name = getString(row, 1);
                if (name == null || name.isBlank()) {
                    skippedRows++;
                    continue;
                }

                Employee emp = Employee.builder()
                        .company(company)
                        /* ---------- identity ---------- */
                        .employeeCode(getString(row, 0))
                        .fullName(name)
                        .email(getString(row, 2))
                        .phone(getString(row, 3))
                        .pan(getString(row, 4))
                        .uanNumber(getString(row, 5))
                        .esicIpNumber(getString(row, 6))
                        /* ---------- dates ---------- */
                        .dateOfBirth(getDate(row, 7))
                        .dateOfJoining(getDate(row, 8))
                        /* ---------- organisation ---------- */
                        .designation(getString(row, 9))
                        .department(getString(row, 10))
                        // work state na ho toh company ka state (PT/LWF isi pe depend)
                        .workState(orDefault(getString(row, 11), company.getState()))
                        /* ---------- bank ---------- */
                        .bankName(getString(row, 12))
                        .bankAccountNumber(getString(row, 13))
                        .bankIfsc(getString(row, 14))
                        /* ---------- salary structure ---------- */
                        .basicSalary(getDecimal(row, 15))
                        .hra(getDecimal(row, 16))
                        .conveyanceAllowance(getDecimal(row, 17))
                        .specialAllowance(getDecimal(row, 18))
                        .medicalAllowance(getDecimal(row, 19))
                        .otherAllowance(getDecimal(row, 20))
                        .totalCtc(getDecimal(row, 21))
                        /* ---------- flags ---------- */
                        .taxRegime("OLD".equalsIgnoreCase(getString(row, 22))
                                ? Employee.TaxRegime.OLD : Employee.TaxRegime.NEW)
                        .pfApplicable(true)
                        .ptApplicable(true)
                        .isActive(true)
                        .build();

                // ESI eligibility GROSS pe decide hoti hai, CTC pe nahi
                emp.setIsEsiApplicable(
                        emp.getMonthlyGross().compareTo(ESI_WAGE_LIMIT) <= 0);

                employees.add(emp);          // <-- yeh line missing thi
            }

            if (employees.isEmpty()) {
                return "No valid employee rows found in the file.";
            }

            employeeRepository.saveAll(employees);

            return String.format(
                    "Success! %d employees uploaded. %d empty rows skipped.",
                    employees.size(), skippedRows);

        } catch (Exception e) {
            return "Error reading Excel file: " + e.getMessage();
        }
    }

    /* ==================================================================
       CELL READERS
       ================================================================== */

    private String getString(Row row, int idx) {
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        String v = switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield cell.getStringCellValue().trim(); }
                catch (Exception e) { yield null; }
            }
            default -> null;
        };
        return (v == null || v.isBlank()) ? null : v;
    }

    private BigDecimal getDecimal(Row row, int idx) {
        Cell cell = row.getCell(idx);
        if (cell == null) return BigDecimal.ZERO;
        return switch (cell.getCellType()) {
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING  -> {
                try {
                    // "Rs 25,000" jaise values bhi handle ho jaayein
                    String s = cell.getStringCellValue().replaceAll("[^0-9.]", "");
                    yield s.isEmpty() ? BigDecimal.ZERO : new BigDecimal(s);
                } catch (NumberFormatException e) {
                    yield BigDecimal.ZERO;
                }
            }
            default -> BigDecimal.ZERO;
        };
    }

    private LocalDate getDate(Row row, int idx) {
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC
                    && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            String s = getString(row, idx);
            return s == null ? null : LocalDate.parse(s);   // yyyy-MM-dd
        } catch (Exception e) {
            return null;      // galat date format se poora upload fail na ho
        }
    }

    private String orDefault(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v;
    }
}