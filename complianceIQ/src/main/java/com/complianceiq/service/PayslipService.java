package com.complianceiq.service;

import com.complianceiq.model.Attendance;
import com.complianceiq.model.Company;
import com.complianceiq.model.Employee;
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
import java.math.RoundingMode;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Government-style Indian pay slip on A4 (210 x 297 mm), single page.
 *
 * Rates ya amounts yahan calculate NAHI hote - sab StatutoryRuleService
 * (DB-driven) aur AttendanceService se aate hain. Yeh sirf render karta hai.
 */
@Service
@RequiredArgsConstructor
public class PayslipService extends PdfBase {

    private final EmployeeRepository employeeRepository;
    private final StatutoryRuleService statutoryRuleService;
    private final AttendanceService attendanceService;

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public byte[] generatePayslip(UUID employeeId, int month, int year) {

        /* ---------- VALIDATION ---------- */
        // Future period ki payslip exist hi nahi karti - UI dropdown ke alawa
        // server pe bhi rokna zaroori hai (URL se bypass na ho)
        if (month < 1 || month > 12)
            throw new RuntimeException("Invalid month: " + month);
        if (YearMonth.of(year, month).isAfter(YearMonth.now()))
            throw new RuntimeException("Pay slip is not available for a future period ("
                    + Month.of(month) + " " + year + ").");

        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        Company co = emp.getCompany();

        /* ---------- ATTENDANCE / LOP ---------- */
        Attendance att    = attendanceService.getOrDefault(emp, month, year);
        BigDecimal factor = att.getAttendanceFactor();          // 25/26 = 0.9615

        /* ---------- CONTRACTED (full month) ---------- */
        BigDecimal fullGross = emp.getMonthlyGross();

        /* ---------- EARNED (LOP ke baad) ---------- */
        BigDecimal basic  = pro(emp.getBasicSalary(),         factor);
        BigDecimal hra    = pro(emp.getHra(),                 factor);
        BigDecimal conv   = pro(emp.getConveyanceAllowance(), factor);
        BigDecimal spl    = pro(emp.getSpecialAllowance(),    factor);
        BigDecimal med    = pro(emp.getMedicalAllowance(),    factor);
        BigDecimal other  = pro(emp.getOtherAllowance(),      factor);
        BigDecimal gross  = basic.add(hra).add(conv).add(spl).add(med).add(other);
        BigDecimal lopAmt = fullGross.subtract(gross);

        String state = emp.getApplicableState();

        /* ---------- DEDUCTIONS (rule engine se, hardcode nahi) ---------- */
        var pf = Boolean.TRUE.equals(emp.getPfApplicable())
                ? statutoryRuleService.calculatePf(basic)
                : new StatutoryRuleService.PfResult(BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, false);

        // ESI eligibility CONTRACTED gross pe, contribution EARNED gross pe
        var esiCheck = statutoryRuleService.calculateEsi(fullGross);
        var esi = esiCheck.applicable()
                ? statutoryRuleService.calculateEsi(gross)
                : new StatutoryRuleService.EsiResult(BigDecimal.ZERO, BigDecimal.ZERO, false);

        // PT full gross pe - LOP se slab nahi badalta
        BigDecimal pt = Boolean.TRUE.equals(emp.getPtApplicable())
                ? statutoryRuleService.calculateProfessionalTax(state, fullGross)
                : BigDecimal.ZERO;

        var lwf = statutoryRuleService.calculateLwf(state);

        BigDecimal totalDed = pf.employee().add(esi.employee()).add(pt).add(lwf.employee());
        BigDecimal netPay   = gross.subtract(totalDed).setScale(0, RoundingMode.HALF_UP);

        /* ================= PDF - A4, single page, bordered grid ================= */
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Document doc = newA4(new PdfWriter(baos))) {

            // Poora pay slip ek outer bordered box mein - govt format
            Table outer = outerBox();
            Cell box = new Cell().setPadding(0).setBorder(Border.NO_BORDER);

            /* ---------- 1. ORGANISATION HEADER ---------- */
            box.add(new Paragraph(co.getCompanyName().toUpperCase())
                    .setFontSize(13).setBold().setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(6).setMarginBottom(1));

            String address = joinNonBlank(", ", co.getAddressLine1(), co.getAddressLine2(),
                    joinNonBlank(" - ", co.getCity(), co.getPincode()));
            if (!address.isBlank())
                box.add(new Paragraph(address).setFontSize(7.5f)
                        .setTextAlignment(TextAlignment.CENTER).setMarginBottom(1));

            String ids = joinNonBlank("    ",
                    prefix("PAN: ", co.getPan()),
                    prefix("TAN: ", co.getTanNumber()),
                    prefix("EPF Code: ", co.getEpfRegistrationNumber()),
                    prefix("ESIC Code: ", co.getEsicRegistrationNumber()));
            if (!ids.isBlank())
                box.add(new Paragraph(ids).setFontSize(7)
                        .setTextAlignment(TextAlignment.CENTER).setMarginBottom(3));

            box.add(titleBar("PAY SLIP FOR THE MONTH OF " + Month.of(month) + " " + year));

            /* ---------- 2. EMPLOYEE DETAILS ---------- */
            Table info = grid(1.15f, 1.6f, 1.15f, 1.6f);
            kv(info, "Employee Code",   dash(emp.getEmployeeCode()));
            kv(info, "PAN",             dash(emp.getPan()));
            kv(info, "Employee Name",   dash(emp.getFullName()));
            kv(info, "UAN",             dash(emp.getUanNumber()));
            kv(info, "Designation",     dash(emp.getDesignation()));
            kv(info, "ESIC IP No.",     dash(emp.getEsicIpNumber()));
            kv(info, "Department",      dash(emp.getDepartment()));
            kv(info, "Date of Joining",
                    emp.getDateOfJoining() == null ? "-" : emp.getDateOfJoining().format(DMY));
            kv(info, "Bank A/C",        maskedBank(emp));
            kv(info, "Work State",      dash(state));
            box.add(info);

            /* ---------- 3. ATTENDANCE ---------- */
            Table at = grid(1, 1, 1, 1, 1);
            head(at, "Working Days"); head(at, "Days Present"); head(at, "Paid Leave");
            head(at, "Loss of Pay");  head(at, "Paid Days");
            valC(at, str(att.getWorkingDays()));
            valC(at, str(att.getPresentDays()));
            valC(at, str(att.getPaidLeaveDays()));
            valC(at, str(att.getLopDays()));
            valC(at, str(att.getPaidDays()));
            box.add(at);

            /* ---------- 4. EARNINGS | DEDUCTIONS ---------- */
            Table money = grid(2.2f, 1.3f, 2.2f, 1.3f);
            head(money, "EARNINGS");    headR(money, "AMOUNT (Rs)");
            head(money, "DEDUCTIONS");  headR(money, "AMOUNT (Rs)");

            payRow(money, "Basic Pay",            basic, "Provident Fund (EPF)", pf.employee());
            payRow(money, "House Rent Allowance", hra,   "ESI Contribution",     esi.employee());
            payRow(money, "Conveyance Allowance", conv,  "Professional Tax",     pt);
            payRow(money, "Special Allowance",    spl,   "Labour Welfare Fund",  lwf.employee());
            payRow(money, "Medical Allowance",    med,   "",                     null);
            payRow(money, "Other Allowance",      other, "",                     null);

            total(money, "GROSS EARNINGS");   totalR(money, amt(gross));
            total(money, "TOTAL DEDUCTIONS"); totalR(money, amt(totalDed));
            box.add(money);

            /* ---------- 5. NET PAY ---------- */
            Table net = grid(2.2f, 1.3f, 3.5f);
            net.addCell(new Cell().add(new Paragraph("NET PAY").setFontSize(10).setBold())
                    .setPadding(5).setBorder(LINE));
            net.addCell(new Cell().add(new Paragraph("Rs " + amt(netPay))
                            .setFontSize(11).setBold().setTextAlignment(TextAlignment.RIGHT))
                    .setPadding(5).setBorder(LINE));
            net.addCell(new Cell().add(new Paragraph(rupeesInWords(netPay))
                            .setFontSize(8).setItalic())
                    .setPadding(5).setBorder(LINE));
            box.add(net);

            /* ---------- 6. EMPLOYER CONTRIBUTION (deduct nahi hota) ---------- */
            Table er = grid(1.6f, 1, 1, 1);
            head(er, "EMPLOYER CONTRIBUTION"); head(er, "EPF"); head(er, "ESI"); head(er, "LWF");
            valC(er, "(not deducted from salary)");
            valC(er, "Rs " + amt(pf.employer()));
            valC(er, "Rs " + amt(esi.employer()));
            valC(er, "Rs " + amt(lwf.employer()));
            box.add(er);

            /* ---------- 7. NOTES ---------- */
            StringBuilder notes = new StringBuilder();
            int n = 1;
            if (pf.cappedAtCeiling())
                notes.append(n++).append(". EPF computed on the statutory wage ceiling of Rs ")
                        .append(amt(pf.pfWage())).append(", not on full basic pay.    ");
            if (!esiCheck.applicable())
                notes.append(n++).append(". ESI not applicable - gross wages exceed "
                        + "the Rs 21,000 threshold.    ");
            if (pt.compareTo(BigDecimal.ZERO) == 0 && state != null)
                notes.append(n++).append(". Professional Tax is nil for ").append(state)
                        .append(" (not levied, or a nil slab applies).    ");
            if (lopAmt.compareTo(BigDecimal.ZERO) > 0)
                notes.append(n).append(". Loss of Pay for ").append(str(att.getLopDays()))
                        .append(" day(s): Rs ").append(amt(lopAmt))
                        .append(" deducted from gross earnings.    ");

            if (notes.length() > 0)
                box.add(new Paragraph("Notes: " + notes)
                        .setFontSize(7).setPadding(4).setBorderTop(LINE));

            box.add(footerNote("This is a computer-generated pay slip and does not "
                    + "require a signature."));

            outer.addCell(box);
            doc.add(outer);

        } catch (Exception e) {
            throw new RuntimeException("Error generating pay slip: " + e.getMessage(), e);
        }

        // try-with-resources BAND hone ke BAAD - tab PDF footer/xref likha jaata hai.
        // Andar return karne se file adhoori rehti thi -> "Failed to load PDF document"
        return baos.toByteArray();
    }

    /* ==================================================================
       BULK PAY SLIPS - ZIP
       ================================================================== */
    public byte[] generateBulkPayslips(UUID companyId, int month, int year) {
        List<Employee> employees = employeeRepository.findByCompanyIdAndIsActiveTrue(companyId);
        if (employees.isEmpty()) throw new RuntimeException("No active employees found");

        ByteArrayOutputStream zipOut = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(zipOut)) {
            for (Employee emp : employees) {
                byte[] pdf = generatePayslip(emp.getId(), month, year);
                String code = emp.getEmployeeCode() == null ? "" : emp.getEmployeeCode() + "_";
                String name = emp.getFullName().replaceAll("[^a-zA-Z0-9]", "_");
                zip.putNextEntry(new ZipEntry(code + name + "_" + month + "_" + year + ".pdf"));
                zip.write(pdf);
                zip.closeEntry();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error creating ZIP: " + e.getMessage(), e);
        }
        return zipOut.toByteArray();
    }

    /* ==================================================================
       PAY SLIP SPECIFIC HELPERS
       (kv, head, headR, valC, total, totalR, titleBar, footerNote,
        grid, outerBox, amt, dash, prefix, joinNonBlank, nz,
        rupeesInWords  -> sab PdfBase se aate hain)
       ================================================================== */

    /** Ek row: earnings left, deductions right */
    private void payRow(Table t, String earnLabel, BigDecimal earnAmt,
                        String dedLabel, BigDecimal dedAmt) {
        t.addCell(new Cell().add(new Paragraph(earnLabel).setFontSize(8))
                .setPadding(3).setBorder(LINE));
        t.addCell(new Cell().add(new Paragraph(earnAmt == null ? "" : amt(earnAmt))
                        .setFontSize(8).setTextAlignment(TextAlignment.RIGHT))
                .setPadding(3).setBorder(LINE));
        t.addCell(new Cell().add(new Paragraph(dedLabel).setFontSize(8))
                .setPadding(3).setBorder(LINE));
        t.addCell(new Cell().add(new Paragraph(dedAmt == null ? "" : amt(dedAmt))
                        .setFontSize(8).setTextAlignment(TextAlignment.RIGHT))
                .setPadding(3).setBorder(LINE));
    }

    /** LOP factor lagao aur 2 decimal pe round karo */
    private BigDecimal pro(BigDecimal full, BigDecimal factor) {
        return nz(full).multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    private String str(Object v) {
        if (v == null) return "0";
        if (v instanceof BigDecimal b) return b.stripTrailingZeros().toPlainString();
        return v.toString();
    }

    /** Account number masked - pay slip pe poora number nahi dikhana chahiye */
    private String maskedBank(Employee e) {
        String bank = e.getBankName();
        String acc  = e.getBankAccountNumber();
        if (acc == null || acc.isBlank()) return dash(bank);
        String last4 = acc.length() > 4 ? acc.substring(acc.length() - 4) : acc;
        return (bank == null || bank.isBlank() ? "" : bank + " ") + "****" + last4;
    }
}