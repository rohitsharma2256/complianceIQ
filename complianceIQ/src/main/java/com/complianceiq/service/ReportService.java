package com.complianceiq.service;

import com.complianceiq.model.Company;
import com.complianceiq.model.ComplianceRecord;
import com.complianceiq.model.PayrollRun;
import com.complianceiq.repository.EmployeeRepository;
import com.complianceiq.service.pdf.PdfBase;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Payroll Compliance Audit Report.
 *
 * A4 LANDSCAPE (297 x 210 mm), single page, 3-column grid - jaise CA firms
 * client ko dete hain. Numbers rule engine se aate hain, AI sirf summary
 * likhta hai (aur woh optional hai - fail hone pe report phir bhi banegi).
 */
@Service
@RequiredArgsConstructor
public class ReportService extends PdfBase {

    private final ComplianceCheckService complianceCheckService;
    private final EmployeeRepository employeeRepository;
    private final AIComplianceService aiComplianceService;
    private final AttendanceService attendanceService;

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public byte[] generateComplianceReport(UUID payrollRunId) {

        PayrollRun run = complianceCheckService.getPayrollRun(payrollRunId);
        List<ComplianceRecord> violations = complianceCheckService.getViolations(payrollRunId);
        Company co = run.getCompany();

        int month = run.getMonth();
        int year  = run.getYear();

        /* ---------- Violations ko severity ke hisaab se baanto ---------- */
        List<ComplianceRecord> blockers = violations.stream()
                .filter(v -> v.getSeverity() == ComplianceRecord.Severity.HIGH).toList();
        List<ComplianceRecord> advisories = violations.stream()
                .filter(v -> v.getSeverity() != ComplianceRecord.Severity.HIGH).toList();

        /* ---------- Statutory totals ---------- */
        BigDecimal epfE = nz(run.getTotalEpfEmployee());
        BigDecimal epfR = nz(run.getTotalEpfEmployer());
        BigDecimal esiE = nz(run.getTotalEsiEmployee());
        BigDecimal esiR = nz(run.getTotalEsiEmployer());
        BigDecimal tds  = nz(run.getTotalTds());
        BigDecimal pt   = nz(run.getTotalProfessionalTax());
        BigDecimal totalPayable = epfE.add(epfR).add(esiE).add(esiR).add(tds).add(pt);

        /* ---------- Data completeness ---------- */
        var employees = employeeRepository.findByCompanyIdAndIsActiveTrue(co.getId());
        int total = employees.size();
        long withPan  = employees.stream().filter(e -> notBlank(e.getPan())).count();
        long withUan  = employees.stream().filter(e -> notBlank(e.getUanNumber())).count();
        long withEsic = employees.stream().filter(e -> notBlank(e.getEsicIpNumber())).count();
        long withBank = employees.stream().filter(e -> notBlank(e.getBankAccountNumber())).count();

        /* ---------- Attendance ---------- */
        AttendanceService.AttendanceSummary att = null;
        try {
            att = attendanceService.getSummary(co.getId(), month, year);
        } catch (Exception ignored) { }

        String verdict = blockers.isEmpty()
                ? (advisories.isEmpty() ? "COMPLIANT" : "COMPLIANT WITH OBSERVATIONS")
                : "NOT READY TO FILE";

        /* ================= PDF ================= */
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Document doc = newA4Landscape(new PdfWriter(baos))) {

            Table outer = outerBox();
            Cell box = new Cell().setPadding(0).setBorder(Border.NO_BORDER);

            /* ---------- HEADER ---------- */
            box.add(new Paragraph(co.getCompanyName().toUpperCase())
                    .setFontSize(13).setBold().setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(5).setMarginBottom(1));

            String addr = joinNonBlank(", ", co.getAddressLine1(), co.getAddressLine2(),
                    joinNonBlank(" - ", co.getCity(), co.getPincode()));
            String ids = joinNonBlank("    ",
                    prefix("PAN: ", co.getPan()),
                    prefix("TAN: ", co.getTanNumber()),
                    prefix("EPF Code: ", co.getEpfRegistrationNumber()),
                    prefix("ESIC Code: ", co.getEsicRegistrationNumber()),
                    prefix("State: ", co.getState()));
            if (!addr.isBlank())
                box.add(new Paragraph(addr).setFontSize(7)
                        .setTextAlignment(TextAlignment.CENTER).setMarginBottom(1));
            if (!ids.isBlank())
                box.add(new Paragraph(ids).setFontSize(6.5f)
                        .setTextAlignment(TextAlignment.CENTER).setMarginBottom(3));

            box.add(titleBar("PAYROLL COMPLIANCE AUDIT REPORT  -  "
                    + Month.of(month) + " " + year));

            /* ---------- META STRIP ---------- */
            Table meta = grid(1, 1, 1, 1);
            kv(meta, "Report Date", LocalDate.now().format(DMY));
            kv(meta, "Payroll Status", String.valueOf(run.getStatus()));
            box.add(meta);

            /* ================= ROW 1 : 3 columns ================= */
            Table r1 = grid(1.1f, 1.5f, 1.6f);

            /* --- 1. COMPLIANCE SCORE --- */
            Cell c1 = panel("1.  COMPLIANCE SCORE");
            Table score = grid(1.4f, 1);
            kv(score, "Employees Covered", String.valueOf(run.getTotalEmployees()));
            kv(score, "Filing Blockers",   String.valueOf(blockers.size()));
            kv(score, "Observations",      String.valueOf(advisories.size()));
            kv(score, "Verdict",           verdict);
            c1.add(score);
            r1.addCell(c1);

            /* --- 2. STATUTORY LIABILITY --- */
            Cell c2 = panel("2.  STATUTORY LIABILITY (Rs)");
            Table liab = grid(1.2f, 1, 1, 1);
            head(liab, "Head"); headR(liab, "Employee"); headR(liab, "Employer"); headR(liab, "Total");
            liabRow(liab, "EPF", epfE, epfR);
            liabRow(liab, "ESI", esiE, esiR);
            liabRow(liab, "TDS", tds, null);
            liabRow(liab, "Prof. Tax", pt, null);
            total(liab, "TOTAL PAYABLE");
            totalR(liab, "");
            totalR(liab, "");
            totalR(liab, amt(totalPayable));
            c2.add(liab);
            c2.add(new Paragraph("Due dates: EPF & ESI by the 15th, TDS by the 7th of "
                    + "the following month. PT and LWF as per state schedule.")
                    .setFontSize(6.5f).setPadding(3));
            r1.addCell(c2);

            /* --- 3. FILING BLOCKERS --- */
            Cell c3 = panel("3.  FILING BLOCKERS  (" + blockers.size() + ")");
            if (blockers.isEmpty()) {
                c3.add(new Paragraph("No blockers. Statutory returns can be filed "
                        + "for this period.").setFontSize(8).setPadding(4));
            } else {
                Table bt = grid(1);
                blockers.stream().limit(6).forEach(v ->
                        bt.addCell(new Cell().add(
                                        new Paragraph(v.getDescription()).setFontSize(7).setBold())
                                .add(new Paragraph("Action: " + dash(v.getRecommendedFix()))
                                        .setFontSize(6.5f))
                                .setPadding(3).setBorder(LINE)));
                if (blockers.size() > 6)
                    bt.addCell(new Cell().add(new Paragraph("... and "
                            + (blockers.size() - 6) + " more. See the system for the full list.")
                            .setFontSize(6.5f).setItalic()).setPadding(3).setBorder(LINE));
                c3.add(bt);
            }
            r1.addCell(c3);
            box.add(r1);

            /* ================= ROW 2 : 3 columns ================= */
            Table r2 = grid(1.1f, 1.5f, 1.6f);

            /* --- 4. ATTENDANCE & LOP --- */
            Cell c4 = panel("4.  ATTENDANCE & LOP");
            Table atg = grid(1.4f, 1);
            if (att != null) {
                kv(atg, "Records Entered", att.recordsEntered() + " / " + att.totalEmployees());
                kv(atg, "Records Missing", String.valueOf(att.recordsMissing()));
                kv(atg, "Employees with LOP", String.valueOf(att.employeesWithLop()));
                kv(atg, "Total LOP Days", String.valueOf(att.totalLopDays()));
            } else {
                kv(atg, "Attendance", "Not available");
            }
            c4.add(atg);
            if (att != null && att.recordsMissing() > 0)
                c4.add(new Paragraph("Full attendance assumed where records are missing.")
                        .setFontSize(6.5f).setPadding(3));
            r2.addCell(c4);

            /* --- 5. DATA COMPLETENESS --- */
            Cell c5 = panel("5.  DATA COMPLETENESS");
            Table dc = grid(1.3f, 0.8f, 2f);
            head(dc, "Identifier"); head(dc, "Present"); head(dc, "Impact if missing");
            dcRow(dc, "PAN",     withPan,  total, "Form 16 blocked; TDS at 20%");
            dcRow(dc, "UAN",     withUan,  total, "EPF ECR export will fail");
            dcRow(dc, "ESIC IP", withEsic, total, "ESI return cannot be filed");
            dcRow(dc, "Bank A/C", withBank, total, "Salary transfer blocked");
            c5.add(dc);
            r2.addCell(c5);

            /* --- 6. OBSERVATIONS --- */
            Cell c6 = panel("6.  OBSERVATIONS & ADVISORIES  (" + advisories.size() + ")");
            if (advisories.isEmpty()) {
                c6.add(new Paragraph("No observations for this period.")
                        .setFontSize(8).setPadding(4));
            } else {
                Table ot = grid(1);
                advisories.stream().limit(6).forEach(v ->
                        ot.addCell(new Cell().add(new Paragraph(
                                "[" + v.getSeverity() + "] " + v.getDescription())
                                .setFontSize(6.8f)).setPadding(3).setBorder(LINE)));
                if (advisories.size() > 6)
                    ot.addCell(new Cell().add(new Paragraph("... and "
                            + (advisories.size() - 6) + " more.")
                            .setFontSize(6.5f).setItalic()).setPadding(3).setBorder(LINE));
                c6.add(ot);
            }
            r2.addCell(c6);
            box.add(r2);

            /* ---------- 7. EXECUTIVE SUMMARY (AI - optional) ---------- */
            String summary = safeAiSummary(run, violations.size());
            if (summary != null && !summary.isBlank()) {
                Table es = grid(1);
                es.addCell(new Cell()
                        .add(new Paragraph("7.  EXECUTIVE SUMMARY")
                                .setFontSize(7.5f).setBold())
                        .add(new Paragraph(summary).setFontSize(7))
                        .setPadding(4).setBorder(LINE));
                box.add(es);
            }

            /* ---------- SIGN-OFF ---------- */
            Table sign = grid(1, 1, 1);
            kv(sign, "Prepared by", dash(co.getSignatoryName()));
            kv(sign, "Designation", dash(co.getSignatoryDesignation()));
            box.add(sign);

            box.add(footerNote("This report is generated from the payroll data recorded in "
                    + "ComplianceIQ. Figures should be verified by a qualified Chartered "
                    + "Accountant before statutory filing. Computer-generated - no signature required."));

            outer.addCell(box);
            doc.add(outer);

        } catch (Exception e) {
            throw new RuntimeException("Error generating report: " + e.getMessage(), e);
        }

        // try-with-resources BAND hone ke BAAD - tab PDF footer/xref likha jaata hai
        return baos.toByteArray();
    }

    /* ==================================================================
       HELPERS
       ================================================================== */

    /** Ek panel = heading + content, bordered cell mein */
    private Cell panel(String heading) {
        Cell c = new Cell().setPadding(0).setBorder(LINE);
        c.add(new Paragraph(heading).setFontSize(7.5f).setBold()
                .setBackgroundColor(LIGHT).setPadding(3).setBorderBottom(LINE));
        return c;
    }

    private void liabRow(Table t, String head, BigDecimal employee, BigDecimal employer) {
        BigDecimal tot = nz(employee).add(nz(employer));
        val(t, head);
        valR(t, amt(employee));
        valR(t, employer == null ? "-" : amt(employer));
        valR(t, amt(tot));
    }

    private void dcRow(Table t, String label, long present, int total, String impact) {
        val(t, label);
        valC(t, present + "/" + total + (present < total ? "  !" : ""));
        t.addCell(new Cell().add(new Paragraph(present < total ? impact : "Complete")
                .setFontSize(6.5f)).setPadding(3).setBorder(LINE));
    }

    /**
     * AI summary optional hai. Rate limit ya koi bhi failure ho toh report
     * phir bhi banni chahiye - AI report ko block nahi kar sakta.
     */
    private String safeAiSummary(PayrollRun run, int violationCount) {
        try {
            return aiComplianceService.generateComplianceSummary(
                    run.getTotalEmployees(),
                    "Rs " + amt(nz(run.getTotalEpfEmployee())),
                    "Rs " + amt(nz(run.getTotalEsiEmployee())),
                    "Rs " + amt(nz(run.getTotalTds())),
                    "Rs " + amt(nz(run.getTotalProfessionalTax())),
                    violationCount);
        } catch (Exception e) {
            return null;      // AI down -> report bina summary ke banegi
        }
    }

    private boolean notBlank(String s) { return s != null && !s.isBlank(); }
}