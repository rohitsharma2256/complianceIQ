package com.complianceiq.service;

import com.complianceiq.model.Employee;
import com.complianceiq.repository.EmployeeRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class PayslipService {

    private final EmployeeRepository employeeRepository;

    private static final DeviceRgb HEADER_COLOR = new DeviceRgb(47, 84, 150);
    private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(240, 240, 240);
    private static final DeviceRgb GREEN = new DeviceRgb(29, 158, 117);

    private static final BigDecimal EPF_RATE = new BigDecimal("0.12");
    private static final BigDecimal ESI_EMP_RATE = new BigDecimal("0.0075");
    private static final BigDecimal ESI_LIMIT = new BigDecimal("21000");

    public byte[] generatePayslip(UUID employeeId, int month, int year) {

        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // Calculations
        BigDecimal basic = emp.getBasicSalary();
        BigDecimal hra = emp.getHra() != null ? emp.getHra() : BigDecimal.ZERO;
        BigDecimal special = emp.getSpecialAllowance() != null ?
                emp.getSpecialAllowance() : BigDecimal.ZERO;
        BigDecimal gross = emp.getTotalCtc();

        // Deductions
        BigDecimal epf = basic.multiply(EPF_RATE)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal esi = BigDecimal.ZERO;
        if (gross.compareTo(ESI_LIMIT) <= 0) {
            esi = gross.multiply(ESI_EMP_RATE)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal pt = calculatePT(gross, emp.getWorkState());
        BigDecimal totalDeductions = epf.add(esi).add(pt);
        BigDecimal netPay = gross.subtract(totalDeductions);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);

            // Header
            doc.add(new Paragraph("PAYSLIP")
                    .setFontSize(20).setBold()
                    .setFontColor(HEADER_COLOR)
                    .setTextAlignment(TextAlignment.CENTER));

            doc.add(new Paragraph(emp.getCompany().getCompanyName())
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER));

            doc.add(new Paragraph("For the month of " +
                    Month.of(month).name() + " " + year)
                    .setFontSize(10)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER));

            doc.add(new Paragraph("\n"));

            // Employee details
            Table empTable = new Table(UnitValue.createPercentArray(
                    new float[]{1, 1})).useAllAvailableWidth();
            addRow(empTable, "Employee Name", emp.getFullName());
            addRow(empTable, "Employee Code",
                    emp.getEmployeeCode() != null ? emp.getEmployeeCode() : "-");
            addRow(empTable, "Designation",
                    emp.getDesignation() != null ? emp.getDesignation() : "-");
            addRow(empTable, "PAN",
                    emp.getPanNumber() != null ? emp.getPanNumber() : "-");
            addRow(empTable, "UAN",
                    emp.getUanNumber() != null ? emp.getUanNumber() : "-");
            doc.add(empTable);
            doc.add(new Paragraph("\n"));

            // Earnings + Deductions side by side
            Table mainTable = new Table(UnitValue.createPercentArray(
                    new float[]{1, 1})).useAllAvailableWidth();

            // Earnings column
            Cell earningsCell = new Cell();
            earningsCell.add(new Paragraph("EARNINGS").setBold()
                    .setFontColor(ColorConstants.WHITE)
                    .setBackgroundColor(HEADER_COLOR));
            earningsCell.add(lineItem("Basic Salary", basic));
            earningsCell.add(lineItem("HRA", hra));
            earningsCell.add(lineItem("Special Allowance", special));
            earningsCell.add(new Paragraph("Gross: Rs " + gross)
                    .setBold());
            mainTable.addCell(earningsCell);

            // Deductions column
            Cell dedCell = new Cell();
            dedCell.add(new Paragraph("DEDUCTIONS").setBold()
                    .setFontColor(ColorConstants.WHITE)
                    .setBackgroundColor(HEADER_COLOR));
            dedCell.add(lineItem("EPF (12%)", epf));
            dedCell.add(lineItem("ESI (0.75%)", esi));
            dedCell.add(lineItem("Professional Tax", pt));
            dedCell.add(new Paragraph("Total: Rs " + totalDeductions)
                    .setBold());
            mainTable.addCell(dedCell);

            doc.add(mainTable);
            doc.add(new Paragraph("\n"));

            // Net Pay
            doc.add(new Paragraph("NET PAY: Rs " + netPay)
                    .setFontSize(16).setBold()
                    .setFontColor(GREEN)
                    .setTextAlignment(TextAlignment.CENTER));

            doc.add(new Paragraph("\n\n"));

            // Disclaimer
            doc.add(new Paragraph("This is a computer-generated payslip. " +
                    "Please verify with your CA for final figures.")
                    .setFontSize(8)
                    .setFontColor(ColorConstants.GRAY)
                    .setItalic());

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating payslip: " +
                    e.getMessage());
        }
    }

    private BigDecimal calculatePT(BigDecimal salary, String state) {
        if (state == null) return BigDecimal.ZERO;
        if (state.equalsIgnoreCase("Maharashtra")) {
            if (salary.compareTo(new BigDecimal("7500")) <= 0)
                return BigDecimal.ZERO;
            else if (salary.compareTo(new BigDecimal("10000")) <= 0)
                return new BigDecimal("175");
            else return new BigDecimal("200");
        }
        return BigDecimal.ZERO;
    }

    private void addRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setBold())
                .setBackgroundColor(LIGHT_GRAY));
        table.addCell(new Cell().add(new Paragraph(value)));
    }

    private Paragraph lineItem(String label, BigDecimal amount) {
        return new Paragraph(label + ": Rs " + amount).setFontSize(10);
    }

    // NEW — bulk payslips as ZIP
    public byte[] generateBulkPayslips(UUID companyId, int month, int year) {
        List<Employee> employees = employeeRepository
                .findByCompanyIdAndIsActiveTrue(companyId);

        if (employees.isEmpty()) {
            throw new RuntimeException("No active employees found");
        }

        ByteArrayOutputStream zipOut = new ByteArrayOutputStream();

        try (ZipOutputStream zip = new ZipOutputStream(zipOut)) {
            for (Employee emp : employees) {
                byte[] pdf = generatePayslip(emp.getId(), month, year);

                String filename = emp.getFullName()
                        .replaceAll("[^a-zA-Z0-9]", "_") + "_payslip.pdf";

                zip.putNextEntry(new ZipEntry(filename));
                zip.write(pdf);
                zip.closeEntry();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error creating ZIP: " + e.getMessage());
        }

        return zipOut.toByteArray();
    }
}