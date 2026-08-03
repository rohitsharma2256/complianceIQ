package com.complianceiq.service;

import com.complianceiq.model.Company;
import com.complianceiq.model.Employee;
import com.complianceiq.repository.EmployeeRepository;
import com.complianceiq.service.pdf.PdfBase;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Form 16 - Part A (TDS summary) + Part B (salary & tax computation).
 *
 * IMPORTANT: Asli Form 16 Part A TRACES portal se digitally signed aata hai.
 * Yeh REFERENCE COPY hai jo CA ko figures verify karne ke liye deta hai -
 * TRACES certificate ka substitute NAHI hai. Document pe clearly likha hai.
 *
 * A4 portrait, 2 pages (Part A, Part B) - official format portrait hi hota hai.
 */
@Service
@RequiredArgsConstructor
public class Form16Service extends PdfBase {

    private final EmployeeRepository employeeRepository;
    private final StatutoryRuleService statutoryRuleService;

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final BigDecimal STD_DEDUCTION = new BigDecimal("75000");
    private static final BigDecimal CESS_RATE     = new BigDecimal("0.04");

    /**
     * @param financialYear starting year, e.g. 2025 => FY 2025-26, AY 2026-27
     */
    public byte[] generateForm16(UUID employeeId, int financialYear) {

        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        Company co = emp.getCompany();

        if (emp.getPan() == null || emp.getPan().isBlank())
            throw new RuntimeException("Form 16 cannot be issued without the employee's PAN.");
        if (co.getTanNumber() == null || co.getTanNumber().isBlank())
            throw new RuntimeException("Form 16 requires the employer's TAN. "
                    + "Add it in Company details.");

        String fy = financialYear + "-" + String.valueOf(financialYear + 1).substring(2);
        String ay = (financialYear + 1) + "-" + String.valueOf(financialYear + 2).substring(2);

        /* ---------- ANNUAL FIGURES (monthly x 12) ---------- */
        BigDecimal mBasic = nz(emp.getBasicSalary());
        BigDecimal mHra   = nz(emp.getHra());
        BigDecimal mGross = emp.getMonthlyGross();
        BigDecimal mOther = mGross.subtract(mBasic).subtract(mHra);

        BigDecimal twelve = new BigDecimal("12");
        BigDecimal aBasic = mBasic.multiply(twelve);
        BigDecimal aHra   = mHra.multiply(twelve);
        BigDecimal aOther = mOther.multiply(twelve);
        BigDecimal aGross = mGross.multiply(twelve);

        /* ---------- DEDUCTIONS ---------- */
        var pf = statutoryRuleService.calculatePf(mBasic);
        BigDecimal aPf = pf.employee().multiply(twelve);

        BigDecimal mPt = statutoryRuleService.calculateProfessionalTax(
                emp.getApplicableState(), mGross);
        BigDecimal aPt = mPt.multiply(twelve);

        boolean oldRegime = emp.getTaxRegime() == Employee.TaxRegime.OLD;

        /* ---------- TAX COMPUTATION ---------- */
        // Section 16: standard deduction + professional tax (PT sirf old regime mein)
        BigDecimal sec16 = STD_DEDUCTION.add(oldRegime ? aPt : BigDecimal.ZERO);
        BigDecimal incomeUnderSalary = aGross.subtract(sec16).max(BigDecimal.ZERO);

        // Chapter VI-A: 80C etc. - old regime mein hi milta hai
        BigDecimal chapterVIA = oldRegime ? aPf.min(new BigDecimal("150000")) : BigDecimal.ZERO;
        BigDecimal taxableIncome = incomeUnderSalary.subtract(chapterVIA).max(BigDecimal.ZERO);

        BigDecimal taxBeforeRebate = oldRegime ? oldRegimeTax(taxableIncome)
                : newRegimeTax(taxableIncome);
        BigDecimal rebate87A = rebate(taxableIncome, taxBeforeRebate, oldRegime);
        BigDecimal taxAfterRebate = taxBeforeRebate.subtract(rebate87A).max(BigDecimal.ZERO);
        BigDecimal cess = taxAfterRebate.multiply(CESS_RATE).setScale(0, RoundingMode.HALF_UP);
        BigDecimal totalTax = taxAfterRebate.add(cess);
        BigDecimal monthlyTds = totalTax.divide(twelve, 0, RoundingMode.HALF_UP);

        /* ================= PDF ================= */
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Document doc = newA4(new PdfWriter(baos))) {

            /* ============ PAGE 1 - PART A ============ */
            Table outerA = outerBox();
            Cell a = new Cell().setPadding(0).setBorder(Border.NO_BORDER);

            a.add(new Paragraph("FORM NO. 16").setFontSize(12).setBold()
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(5));
            a.add(new Paragraph("[See rule 31(1)(a)]").setFontSize(7)
                    .setTextAlignment(TextAlignment.CENTER));
            a.add(new Paragraph("Certificate under section 203 of the Income-tax Act, 1961 "
                    + "for tax deducted at source on salary")
                    .setFontSize(7.5f).setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(3));

            a.add(titleBar("PART A  -  REFERENCE COPY (not a TRACES certificate)"));

            /* ---- Employer / Employee ---- */
            Table pa = grid(1.2f, 1.6f, 1.2f, 1.6f);
            kv(pa, "Name of Employer",     dash(co.getCompanyName()));
            kv(pa, "Name of Employee",     dash(emp.getFullName()));
            kv(pa, "Employer Address",     dash(joinNonBlank(", ",
                    co.getAddressLine1(), co.getCity(), co.getPincode())));
            kv(pa, "Employee Designation", dash(emp.getDesignation()));
            kv(pa, "PAN of Deductor",      dash(co.getPan()));
            kv(pa, "PAN of Employee",      dash(emp.getPan()));
            kv(pa, "TAN of Deductor",      dash(co.getTanNumber()));
            kv(pa, "Employee Reference",   dash(emp.getEmployeeCode()));
            a.add(pa);

            /* ---- Assessment year / period ---- */
            Table per = grid(1, 1, 1, 1);
            head(per, "Assessment Year"); head(per, "Financial Year");
            head(per, "Period From");     head(per, "Period To");
            valC(per, ay);
            valC(per, fy);
            valC(per, "01-04-" + financialYear);
            valC(per, "31-03-" + (financialYear + 1));
            a.add(per);

            /* ---- Certificate details (TRACES se aate hain, yahan blank) ---- */
            Table cert = grid(1.2f, 1.6f, 1.2f, 1.6f);
            kv(cert, "Certificate No.", "____________  (from TRACES)");
            kv(cert, "Last updated on", "____________");
            a.add(cert);

            /* ---- Quarterly TDS summary ---- */
            a.add(titleBar("SUMMARY OF TAX DEDUCTED AT SOURCE"));
            Table q = grid(0.8f, 1.4f, 1.2f, 1.2f, 1.2f);
            head(q, "Quarter"); head(q, "Period");
            headR(q, "Amount Paid (Rs)"); headR(q, "Tax Deducted (Rs)");
            headR(q, "Tax Deposited (Rs)");

            BigDecimal qGross = mGross.multiply(new BigDecimal("3"));
            BigDecimal qTds   = monthlyTds.multiply(new BigDecimal("3"));
            // Q4 mein rounding difference adjust - quarterly ka total annual se match kare
            BigDecimal q4Tds  = totalTax.subtract(qTds.multiply(new BigDecimal("3")));

            qRow(q, "Q1", "Apr - Jun " + financialYear,       qGross, qTds);
            qRow(q, "Q2", "Jul - Sep " + financialYear,       qGross, qTds);
            qRow(q, "Q3", "Oct - Dec " + financialYear,       qGross, qTds);
            qRow(q, "Q4", "Jan - Mar " + (financialYear + 1), qGross, q4Tds);

            total(q, "TOTAL"); total(q, "");
            totalR(q, amt(aGross)); totalR(q, amt(totalTax)); totalR(q, amt(totalTax));
            a.add(q);

            a.add(new Paragraph("Note: This is a system-generated reference copy prepared "
                    + "from payroll records. The statutory Form 16 Part A must be downloaded "
                    + "from the TRACES portal with a digital signature. Quarterly figures "
                    + "shown here are computed on a uniform monthly basis and should be "
                    + "reconciled with the actual TDS returns filed.")
                    .setFontSize(6.5f).setPadding(4).setBorderTop(LINE));

            a.add(verificationBlock(co));
            outerA.addCell(a);
            doc.add(outerA);

            /* ============ PAGE 2 - PART B ============ */
            doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            Table outerB = outerBox();
            Cell b = new Cell().setPadding(0).setBorder(Border.NO_BORDER);

            b.add(new Paragraph("FORM NO. 16").setFontSize(12).setBold()
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(5));
            b.add(new Paragraph("Details of Salary Paid and any other income and tax deducted")
                    .setFontSize(7.5f).setTextAlignment(TextAlignment.CENTER).setMarginBottom(3));

            b.add(titleBar("PART B  -  ANNEXURE TO FORM 16  |  FY " + fy + "  |  AY " + ay));
            b.add(new Paragraph("Part B is prepared by the employer and is issued along "
                    + "with Part A downloaded from the TRACES portal.")
                    .setFontSize(6.5f).setTextAlignment(TextAlignment.CENTER)
                    .setPaddingTop(2).setPaddingBottom(2));

            Table id2 = grid(1.2f, 1.6f, 1.2f, 1.6f);
            kv(id2, "Employee Name", dash(emp.getFullName()));
            kv(id2, "PAN",           dash(emp.getPan()));
            kv(id2, "Tax Regime",    oldRegime ? "Old Regime" : "New Regime (default)");
            kv(id2, "Date of Joining",
                    emp.getDateOfJoining() == null ? "-" : emp.getDateOfJoining().format(DMY));
            b.add(id2);

            /* ---- Salary and tax computation ---- */
            Table gs = grid(0.5f, 3.5f, 1.3f);
            head(gs, "S.No"); head(gs, "Particulars"); headR(gs, "Amount (Rs)");

            fRow(gs, "1",   "Gross Salary", null);
            fRow(gs, "(a)", "Salary as per section 17(1) - Basic", aBasic);
            fRow(gs, "(b)", "House Rent Allowance", aHra);
            fRow(gs, "(c)", "Other allowances and perquisites", aOther);
            fTotal(gs, "", "Total Gross Salary", aGross);

            fRow(gs, "2", "Less: Allowances exempt under section 10", BigDecimal.ZERO);
            fRow(gs, "3", "Deductions under section 16", null);
            fRow(gs, "(a)", "Standard deduction u/s 16(ia)", STD_DEDUCTION);
            fRow(gs, "(b)", "Tax on employment u/s 16(iii) - Professional Tax",
                    oldRegime ? aPt : BigDecimal.ZERO);
            fTotal(gs, "4", "Income chargeable under the head 'Salaries'", incomeUnderSalary);

            fRow(gs, "5", "Deductions under Chapter VI-A", null);
            fRow(gs, "(a)", "Section 80C - Employee's EPF contribution",
                    oldRegime ? aPf : BigDecimal.ZERO);
            fTotal(gs, "6", "Aggregate deductible amount under Chapter VI-A", chapterVIA);
            fTotal(gs, "7", "Total taxable income", taxableIncome);

            fRow(gs, "8",  "Tax on total income", taxBeforeRebate);
            fRow(gs, "9",  "Rebate under section 87A", rebate87A);
            fRow(gs, "10", "Health and Education Cess @ 4%", cess);
            fTotal(gs, "11", "Total tax payable", totalTax);
            fRow(gs, "12", "Less: Tax deducted at source", totalTax);
            fTotal(gs, "13", "Tax payable / (refundable)", BigDecimal.ZERO);
            b.add(gs);

            if (!oldRegime)
                b.add(new Paragraph("The employee has opted for the new tax regime under "
                        + "section 115BAC. Chapter VI-A deductions and the section 16(iii) "
                        + "professional tax deduction are therefore not available.")
                        .setFontSize(6.5f).setPadding(4).setBorderTop(LINE));

            b.add(new Paragraph("Note: This Part B is computed from payroll records on a "
                    + "uniform monthly basis and does not include income from other sources, "
                    + "house property, or investment declarations submitted by the employee. "
                    + "Verify against actual declarations and Form 12BB before issuing.")
                    .setFontSize(6.5f).setPadding(4).setBorderTop(LINE));

            b.add(verificationBlock(co));
            outerB.addCell(b);
            doc.add(outerB);

        } catch (Exception e) {
            throw new RuntimeException("Error generating Form 16: " + e.getMessage(), e);
        }

        // try-with-resources BAND hone ke BAAD - tab PDF footer/xref likha jaata hai
        return baos.toByteArray();
    }

    /* ==================================================================
       TAX SLABS
       ================================================================== */

    /** New regime FY 2025-26 */
    private BigDecimal newRegimeTax(BigDecimal income) {
        BigDecimal tax = BigDecimal.ZERO;
        tax = tax.add(slab(income, "400000",  "800000",  "0.05"));
        tax = tax.add(slab(income, "800000",  "1200000", "0.10"));
        tax = tax.add(slab(income, "1200000", "1600000", "0.15"));
        tax = tax.add(slab(income, "1600000", "2000000", "0.20"));
        tax = tax.add(slab(income, "2000000", "2400000", "0.25"));
        tax = tax.add(slab(income, "2400000", null,      "0.30"));
        return tax.setScale(0, RoundingMode.HALF_UP);
    }

    /** Old regime (below 60 years) */
    private BigDecimal oldRegimeTax(BigDecimal income) {
        BigDecimal tax = BigDecimal.ZERO;
        tax = tax.add(slab(income, "250000",  "500000",  "0.05"));
        tax = tax.add(slab(income, "500000",  "1000000", "0.20"));
        tax = tax.add(slab(income, "1000000", null,      "0.30"));
        return tax.setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal slab(BigDecimal income, String from, String to, String rate) {
        BigDecimal lo = new BigDecimal(from);
        if (income.compareTo(lo) <= 0) return BigDecimal.ZERO;
        BigDecimal hi = (to == null) ? income : new BigDecimal(to).min(income);
        return hi.subtract(lo).multiply(new BigDecimal(rate));
    }

    /** 87A rebate: new regime up to 12L (Rs 60,000), old regime up to 5L (Rs 12,500) */
    private BigDecimal rebate(BigDecimal taxable, BigDecimal tax, boolean oldRegime) {
        if (oldRegime)
            return taxable.compareTo(new BigDecimal("500000")) <= 0
                    ? tax.min(new BigDecimal("12500")) : BigDecimal.ZERO;
        return taxable.compareTo(new BigDecimal("1200000")) <= 0
                ? tax.min(new BigDecimal("60000")) : BigDecimal.ZERO;
    }

    /* ==================================================================
       PDF HELPERS  (kv, head, headR, val, valC, valR, total, totalR,
       titleBar, grid, outerBox, amt, dash, nz, joinNonBlank -> PdfBase se)
       ================================================================== */

    private void qRow(Table t, String q, String period, BigDecimal paid, BigDecimal tds) {
        valC(t, q);
        val(t, period);
        valR(t, amt(paid));
        valR(t, amt(tds));
        valR(t, amt(tds));
    }

    private void fRow(Table t, String sn, String particulars, BigDecimal amount) {
        valC(t, sn);
        val(t, particulars);
        valR(t, amount == null ? "" : amt(amount));
    }

    private void fTotal(Table t, String sn, String particulars, BigDecimal amount) {
        total(t, sn);
        total(t, particulars);
        totalR(t, amt(amount));
    }

    private Table verificationBlock(Company co) {
        Table v = grid(1, 1);
        v.addCell(new Cell()
                .add(new Paragraph("VERIFICATION").setFontSize(7).setBold())
                .add(new Paragraph("I, " + dash(co.getSignatoryName())
                        + ", working in the capacity of "
                        + dash(co.getSignatoryDesignation())
                        + ", do hereby certify that the information given above is true, "
                        + "complete and correct, and is based on the books of account, "
                        + "documents and other available records.").setFontSize(6.5f))
                .setPadding(4).setBorder(LINE));
        v.addCell(new Cell()
                .add(new Paragraph("Place: " + dash(co.getCity())).setFontSize(7))
                .add(new Paragraph("Date: " + LocalDate.now().format(DMY)).setFontSize(7))
                .add(new Paragraph("\n\n").setFontSize(7))
                .add(new Paragraph("Signature of person responsible for deduction of tax")
                        .setFontSize(6.5f).setBorderTop(LINE))
                .setPadding(4).setBorder(LINE));
        return v;
    }
}