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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExcelService {

    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;

    private static final BigDecimal ESI_WAGE_LIMIT = new BigDecimal("21000");

    public String uploadEmployees(UUID companyId, MultipartFile file) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        List<Employee> employees = new ArrayList<>();
        int skippedRows = 0;

        // FIX: bytes se InputStream banao
        try (Workbook workbook = WorkbookFactory.create(
                new ByteArrayInputStream(file.getBytes()))) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String name = getStringValue(row.getCell(0));
                if (name == null || name.isBlank()) {
                    skippedRows++;
                    continue;
                }

                BigDecimal basic = getNumberValue(row.getCell(1));
                BigDecimal hra = getNumberValue(row.getCell(2));
                BigDecimal special = getNumberValue(row.getCell(3));
                BigDecimal ctc = getNumberValue(row.getCell(4));
                String pan = getStringValue(row.getCell(5));
                String state = getStringValue(row.getCell(6));

                boolean esiApplicable = ctc.compareTo(ESI_WAGE_LIMIT) <= 0;

                Employee emp = Employee.builder()
                        .company(company)
                        .fullName(name)
                        .basicSalary(basic)
                        .hra(hra)
                        .specialAllowance(special)
                        .totalCtc(ctc)
                        .panNumber(pan)
                        .workState(state)
                        .isEpfApplicable(true)
                        .isEsiApplicable(esiApplicable)
                        .isActive(true)
                        .build();

                employees.add(emp);
            }

            employeeRepository.saveAll(employees);

            return String.format(
                    "Success! %d employees uploaded. %d empty rows skipped.",
                    employees.size(), skippedRows);

        } catch (Exception e) {
            return "Error reading Excel file: " + e.getMessage();
        }
    }

    private String getStringValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> null;
        };
    }

    private BigDecimal getNumberValue(Cell cell) {
        if (cell == null) return BigDecimal.ZERO;
        return switch (cell.getCellType()) {
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING -> {
                try {
                    yield new BigDecimal(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    yield BigDecimal.ZERO;
                }
            }
            default -> BigDecimal.ZERO;
        };
    }
}