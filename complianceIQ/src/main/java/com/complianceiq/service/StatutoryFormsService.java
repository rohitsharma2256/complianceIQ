package com.complianceiq.service;

import com.complianceiq.model.Employee;
import com.complianceiq.model.PayrollRun;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StatutoryFormsService {

    private final EmployeeRepository employeeRepository;
    private final ComplianceCheckService complianceCheckService;

    private static final DeviceRgb HEADER_COLOR = new DeviceRgb(47, 84, 150);
    private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(240, 240, 240);

    // ─── FORM 16 (TDS Certificate for Employee) ───
    public byte[] generateForm16(UUID employeeId, int financialYear) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        BigDecimal annualSalary = emp.getTotalCtc()
                .multiply(new BigDecimal("12"));
        BigDecimal standardDeduction = new BigDecimal("75000");
        BigDecimal taxableIncome = annualSalary.subtract(standardDeduction);
        BigDecimal annualTds = calculateAnnualTds(taxableIncome);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);

            doc.add(new Paragraph("FORM 16")
                    .setFontSize(20).setBold()
                    .setFontColor(HEADER_COLOR)
                    .setTextAlignment(TextAlignment.CENTER));

            doc.add(new Paragraph("Certificate under Section 203 of " +
                    "Income Tax Act, 1961")
                    .setFontSize(9)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER));

            doc.add(new Paragraph("Financial Year: " + financialYear +
                    "-" + (financialYear + 1))
                    .setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("\n"));

            // Employer + Employee details
            Table detailsTable = new Table(UnitValue.createPercentArray(
                    new float[]{1, 1})).useAllAvailableWidth();

            addRow(detailsTable, "Employer",
                    emp.getCompany().getCompanyName());
            addRow(detailsTable, "TAN",
                    emp.getCompany().getTanNumber() != null ?
                            emp.getCompany().getTanNumber() : "-");
            addRow(detailsTable, "Employee Name", emp.getFullName());
            addRow(detailsTable, "PAN",
                    emp.getPanNumber() != null ? emp.getPanNumber() : "-");
            addRow(detailsTable, "Designation",
                    emp.getDesignation() != null ? emp.getDesignation() : "-");
            doc.add(detailsTable);
            doc.add(new Paragraph("\n"));

            // Salary + Tax details
            doc.add(new Paragraph("Details of Salary Paid and Tax Deducted")
                    .setFontSize(14).setBold().setFontColor(HEADER_COLOR));

            Table taxTable = new Table(UnitValue.createPercentArray(
                    new float[]{2, 1})).useAllAvailableWidth();

            addHeaderRow(taxTable, "Particulars", "Amount (Rs)");
            addRow(taxTable, "Gross Annual Salary",
                    annualSalary.toString());
            addRow(taxTable, "Less: Standard Deduction",
                    standardDeduction.toString());
            addRow(taxTable, "Taxable Income",
                    taxableIncome.toString());
            addRow(taxTable, "Total Tax Deducted (TDS)",
                    annualTds.toString());
            doc.add(taxTable);

            doc.add(new Paragraph("\n\n"));
            doc.add(new Paragraph("This is a computer-generated Form 16. " +
                    "Verify with your CA before submission.")
                    .setFontSize(8).setFontColor(ColorConstants.GRAY)
                    .setItalic());

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating Form 16: " +
                    e.getMessage());
        }
    }

    // ─── ECR FILE (EPF Electronic Challan cum Return) ───
    // Text file format for EPF portal upload
    public String generateECR(UUID companyId, int month, int year) {
        List<Employee> employees = employeeRepository
                .findByCompanyIdAndIsActiveTrue(companyId);

        StringBuilder ecr = new StringBuilder();

        // ECR format: UAN#~#Name#~#Gross#~#EPF Wages#~#EPS Wages#~#
        //             EE Share#~#EPS#~#ER Share#~#NCP Days#~#Refund
        for (Employee emp : employees) {
            if (!Boolean.TRUE.equals(emp.getIsEpfApplicable())) continue;

            BigDecimal basic = emp.getBasicSalary();
            BigDecimal epfWages = basic;
            BigDecimal eeShare = basic.multiply(new BigDecimal("0.12"))
                    .setScale(0, RoundingMode.HALF_UP);
            BigDecimal eps = basic.multiply(new BigDecimal("0.0833"))
                    .setScale(0, RoundingMode.HALF_UP);
            BigDecimal erShare = eeShare.subtract(eps);

            ecr.append(emp.getUanNumber() != null ?
                            emp.getUanNumber() : "000000000000")
                    .append("#~#")
                    .append(emp.getFullName())
                    .append("#~#")
                    .append(emp.getTotalCtc())
                    .append("#~#")
                    .append(epfWages)
                    .append("#~#")
                    .append(epfWages)
                    .append("#~#")
                    .append(eeShare)
                    .append("#~#")
                    .append(eps)
                    .append("#~#")
                    .append(erShare)
                    .append("#~#0#~#0")
                    .append("\n");
        }

        return ecr.toString();
    }

    // ─── CHALLAN SUMMARY (PF/ESI/TDS Payment) ───
    public byte[] generateChallan(UUID payrollRunId) {
        PayrollRun run = complianceCheckService.getPayrollRun(payrollRunId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);

            doc.add(new Paragraph("PAYMENT CHALLAN SUMMARY")
                    .setFontSize(18).setBold()
                    .setFontColor(HEADER_COLOR)
                    .setTextAlignment(TextAlignment.CENTER));

            doc.add(new Paragraph(run.getCompany().getCompanyName() +
                    " | Month " + run.getMonth() + "/" + run.getYear())
                    .setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("\n"));

            Table challanTable = new Table(UnitValue.createPercentArray(
                    new float[]{2, 1, 1})).useAllAvailableWidth();

            addHeaderRow3(challanTable, "Statutory Payment",
                    "Amount (Rs)", "Due Date");

            BigDecimal totalEpf = run.getTotalEpfEmployee()
                    .add(run.getTotalEpfEmployer());
            BigDecimal totalEsi = run.getTotalEsiEmployee()
                    .add(run.getTotalEsiEmployer());

            addChallanRow(challanTable, "EPF (Employee + Employer)",
                    totalEpf.toString(), "15th");
            addChallanRow(challanTable, "ESI (Employee + Employer)",
                    totalEsi.toString(), "15th");
            addChallanRow(challanTable, "TDS",
                    run.getTotalTds().toString(), "7th");
            addChallanRow(challanTable, "Professional Tax",
                    run.getTotalProfessionalTax().toString(), "As per state");

            doc.add(challanTable);

            BigDecimal grandTotal = totalEpf.add(totalEsi)
                    .add(run.getTotalTds())
                    .add(run.getTotalProfessionalTax());

            doc.add(new Paragraph("\n"));
            doc.add(new Paragraph("GRAND TOTAL: Rs " + grandTotal)
                    .setFontSize(14).setBold()
                    .setTextAlignment(TextAlignment.RIGHT));

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating challan: " +
                    e.getMessage());
        }
    }

    // Annual TDS calculation
    private BigDecimal calculateAnnualTds(BigDecimal taxableIncome) {
        if (taxableIncome.compareTo(new BigDecimal("300000")) <= 0) {
            return BigDecimal.ZERO;
        } else if (taxableIncome.compareTo(new BigDecimal("700000")) <= 0) {
            return taxableIncome.subtract(new BigDecimal("300000"))
                    .multiply(new BigDecimal("0.05"))
                    .setScale(0, RoundingMode.HALF_UP);
        } else {
            return new BigDecimal("20000")
                    .add(taxableIncome.subtract(new BigDecimal("700000"))
                            .multiply(new BigDecimal("0.10")))
                    .setScale(0, RoundingMode.HALF_UP);
        }
    }

    // Helpers
    private void addRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setBold())
                .setBackgroundColor(LIGHT_GRAY));
        table.addCell(new Cell().add(new Paragraph(value)));
    }

    private void addHeaderRow(Table table, String c1, String c2) {
        table.addCell(headerCell(c1));
        table.addCell(headerCell(c2));
    }

    private void addHeaderRow3(Table table, String c1, String c2, String c3) {
        table.addCell(headerCell(c1));
        table.addCell(headerCell(c2));
        table.addCell(headerCell(c3));
    }

    private void addChallanRow(Table table, String label,
                               String amount, String due) {
        table.addCell(new Cell().add(new Paragraph(label)));
        table.addCell(new Cell().add(new Paragraph(amount)));
        table.addCell(new Cell().add(new Paragraph(due)));
    }

    private Cell headerCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold()
                        .setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(HEADER_COLOR);
    }
}