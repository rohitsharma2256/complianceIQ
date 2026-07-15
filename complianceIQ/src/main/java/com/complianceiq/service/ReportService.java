package com.complianceiq.service;

import com.complianceiq.model.ComplianceRecord;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ComplianceCheckService complianceCheckService;
    private final EmployeeRepository employeeRepository;
    private final AIComplianceService aiComplianceService;

    private static final DeviceRgb HEADER_COLOR = new DeviceRgb(47, 84, 150);
    private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(240, 240, 240);

    public byte[] generateComplianceReport(UUID payrollRunId) {
        PayrollRun run = complianceCheckService.getPayrollRun(payrollRunId);
        List<ComplianceRecord> violations =
                complianceCheckService.getViolations(payrollRunId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);

            // ─── HEADER ───
            doc.add(new Paragraph("COMPLIANCE AUDIT REPORT")
                    .setFontSize(20)
                    .setBold()
                    .setFontColor(HEADER_COLOR)
                    .setTextAlignment(TextAlignment.CENTER));

            doc.add(new Paragraph("ComplianceIQ - AI Powered Payroll Compliance")
                    .setFontSize(10)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER));

            doc.add(new Paragraph("\n"));

            // ─── AI EXECUTIVE SUMMARY ───
            doc.add(new Paragraph("Executive Summary")
                    .setFontSize(14).setBold().setFontColor(HEADER_COLOR));

            String aiSummary = aiComplianceService.generateComplianceSummary(
                    run.getTotalEmployees(),
                    "Rs " + run.getTotalEpfEmployee(),
                    "Rs " + run.getTotalEsiEmployee(),
                    "Rs " + run.getTotalTds(),
                    "Rs " + run.getTotalProfessionalTax(),
                    violations.size()
            );

            doc.add(new Paragraph(aiSummary)
                    .setFontSize(10)
                    .setFontColor(ColorConstants.DARK_GRAY));

            doc.add(new Paragraph("\n"));

            // ─── COMPANY DETAILS ───
            doc.add(new Paragraph("Company Details")
                    .setFontSize(14).setBold().setFontColor(HEADER_COLOR));

            Table companyTable = new Table(UnitValue.createPercentArray(
                    new float[]{1, 2}))
                    .useAllAvailableWidth();

            addRow(companyTable, "Company Name",
                    run.getCompany().getCompanyName());
            addRow(companyTable, "State",
                    run.getCompany().getState());
            addRow(companyTable, "Period",
                    "Month " + run.getMonth() + " / " + run.getYear());
            addRow(companyTable, "Report Date",
                    LocalDate.now().toString());
            addRow(companyTable, "Total Employees",
                    String.valueOf(run.getTotalEmployees()));
            addRow(companyTable, "Status",
                    run.getStatus().toString());

            doc.add(companyTable);
            doc.add(new Paragraph("\n"));

            // ─── STATUTORY SUMMARY ───
            doc.add(new Paragraph("Statutory Contributions Summary")
                    .setFontSize(14).setBold().setFontColor(HEADER_COLOR));

            Table statTable = new Table(UnitValue.createPercentArray(
                    new float[]{2, 1}))
                    .useAllAvailableWidth();

            addHeaderRow(statTable, "Component", "Amount (Rs)");
            addRow(statTable, "EPF - Employee (12%)",
                    format(run.getTotalEpfEmployee()));
            addRow(statTable, "EPF - Employer (12%)",
                    format(run.getTotalEpfEmployer()));
            addRow(statTable, "ESI - Employee (0.75%)",
                    format(run.getTotalEsiEmployee()));
            addRow(statTable, "ESI - Employer (3.25%)",
                    format(run.getTotalEsiEmployer()));
            addRow(statTable, "TDS", format(run.getTotalTds()));
            addRow(statTable, "Professional Tax",
                    format(run.getTotalProfessionalTax()));

            doc.add(statTable);
            doc.add(new Paragraph("\n"));

            // ─── VIOLATIONS ───
            doc.add(new Paragraph("Compliance Violations")
                    .setFontSize(14).setBold().setFontColor(HEADER_COLOR));

            if (violations.isEmpty()) {
                doc.add(new Paragraph("No violations found. All employees compliant.")
                        .setFontColor(new DeviceRgb(29, 158, 117)));
            } else {
                doc.add(new Paragraph(violations.size() + " violation(s) found:")
                        .setFontColor(ColorConstants.RED).setBold());

                Table vTable = new Table(UnitValue.createPercentArray(
                        new float[]{1, 2, 2}))
                        .useAllAvailableWidth();

                addHeaderRow3(vTable, "Type", "Issue", "Recommended Fix");

                for (ComplianceRecord v : violations) {
                    vTable.addCell(cell(v.getViolationType().toString()));
                    vTable.addCell(cell(v.getDescription()));
                    vTable.addCell(cell(v.getRecommendedFix()));
                }
                doc.add(vTable);
            }

            doc.add(new Paragraph("\n\n"));

            // ─── DISCLAIMER ───
            doc.add(new Paragraph("Disclaimer: This report is generated for " +
                    "informational purposes. Please verify with a qualified " +
                    "Chartered Accountant before final statutory filings.")
                    .setFontSize(8)
                    .setFontColor(ColorConstants.GRAY)
                    .setItalic());

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF: " + e.getMessage());
        }
    }

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

    private Cell headerCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold()
                        .setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(HEADER_COLOR);
    }

    private Cell cell(String text) {
        return new Cell().add(new Paragraph(
                text != null ? text : "-").setFontSize(9));
    }

    private String format(Object value) {
        if (value == null) return "0.00";
        return value.toString();
    }
}