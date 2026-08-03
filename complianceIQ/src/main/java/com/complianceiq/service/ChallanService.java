package com.complianceiq.service;

import com.complianceiq.model.Company;
import com.complianceiq.model.PayrollRun;
import com.complianceiq.service.pdf.PdfBase;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.IBlockElement;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Statutory payment challans - EPF, ESI aur TDS.
 *
 * Yeh PAYMENT ADVICE hain, na ki paid challans. Actual challan portal se
 * generate hota hai (EPFO/ESIC/NSDL) aur usme CIN/CRN number aata hai.
 * Yeh CA ko batata hai kitna, kis head mein, kab tak jama karna hai.
 *
 * A4 portrait, single page, government-style bordered grid.
 */
@Service
@RequiredArgsConstructor
public class ChallanService extends PdfBase {

    private final ComplianceCheckService complianceCheckService;

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final BigDecimal HUNDRED        = new BigDecimal("100");
    private static final BigDecimal EPF_RATE       = new BigDecimal("12");
    private static final BigDecimal EPS_RATE       = new BigDecimal("8.33");
    private static final BigDecimal EPF_ADMIN_RATE = new BigDecimal("0.50");
    private static final BigDecimal EPF_ADMIN_MIN  = new BigDecimal("500");
    private static final BigDecimal EDLI_RATE      = new BigDecimal("0.50");
    private static final BigDecimal ESI_ER_RATE    = new BigDecimal("3.25");

    public enum ChallanType { EPF, ESI, TDS }

    /* ==================================================================
       ENTRY POINT
       ================================================================== */
    public byte[] generateChallan(UUID payrollRunId, ChallanType type) {
        PayrollRun run = complianceCheckService.getPayrollRun(payrollRunId);
        return switch (type) {
            case EPF -> epfChallan(run);
            case ESI -> esiChallan(run);
            case TDS -> tdsChallan(run);
        };
    }

    public String buildFileName(UUID payrollRunId, ChallanType type) {
        PayrollRun run = complianceCheckService.getPayrollRun(payrollRunId);
        return type + "_challan_" + run.getMonth() + "_" + run.getYear() + ".pdf";
    }

    /* ==================================================================
       1. EPF CHALLAN  -  account-head wise (EPFO format)
       ================================================================== */
    private byte[] epfChallan(PayrollRun run) {
        Company co = run.getCompany();

        BigDecimal epfEE = nz(run.getTotalEpfEmployee());
        BigDecimal epfER = nz(run.getTotalEpfEmployer());

        // EPF wages employer contribution se reverse (12% laga tha)
        BigDecimal epfWages = epfER.compareTo(BigDecimal.ZERO) > 0
                ? epfER.multiply(HUNDRED).divide(EPF_RATE, 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal ac10 = pct(epfWages, EPS_RATE);                          // Pension
        BigDecimal ac01 = epfEE.add(epfER.subtract(ac10).max(BigDecimal.ZERO));
        BigDecimal ac02 = pct(epfWages, EPF_ADMIN_RATE).max(
                epfWages.compareTo(BigDecimal.ZERO) > 0 ? EPF_ADMIN_MIN : BigDecimal.ZERO);
        BigDecimal ac21 = pct(epfWages, EDLI_RATE);                         // EDLI
        BigDecimal ac22 = BigDecimal.ZERO;                                  // EDLI admin (waived)

        BigDecimal total = ac01.add(ac02).add(ac10).add(ac21).add(ac22);

        List<IBlockElement> body = new ArrayList<>();

        Table t = grid(1, 3, 1.4f);
        head(t, "A/C No."); head(t, "Description"); headR(t, "Amount (Rs)");
        acRow(t, "A/C 01", "EPF Contribution (employee 12% + employer share)", ac01);
        acRow(t, "A/C 02", "EPF Administrative Charges (0.50%, min Rs 500)", ac02);
        acRow(t, "A/C 10", "Pension Fund - EPS (employer 8.33%)", ac10);
        acRow(t, "A/C 21", "EDLI Contribution (0.50%)", ac21);
        acRow(t, "A/C 22", "EDLI Administrative Charges", ac22);
        total(t, ""); total(t, "TOTAL AMOUNT PAYABLE"); totalR(t, amt(total));
        body.add(t);

        Table w = grid(1.4f, 1, 1.4f, 1);
        kv(w, "Total EPF Wages",   amt(epfWages));
        kv(w, "Employees Covered", String.valueOf(run.getTotalEmployees()));
        kv(w, "Employee Share",    amt(epfEE));
        kv(w, "Employer Share",    amt(epfER));
        body.add(w);

        return render(run, "EMPLOYEES' PROVIDENT FUND ORGANISATION",
                "COMBINED CHALLAN FOR EPF / EPS / EDLI",
                "EPF Code: " + dash(co.getEpfRegistrationNumber()),
                total, dueDate(run, 15), body);
    }

    /* ==================================================================
       2. ESI CHALLAN
       ================================================================== */
    private byte[] esiChallan(PayrollRun run) {
        Company co = run.getCompany();

        BigDecimal esiEE = nz(run.getTotalEsiEmployee());
        BigDecimal esiER = nz(run.getTotalEsiEmployer());
        BigDecimal total = esiEE.add(esiER);

        // ESI wages employer share (3.25%) se reverse
        BigDecimal esiWages = esiER.compareTo(BigDecimal.ZERO) > 0
                ? esiER.multiply(HUNDRED).divide(ESI_ER_RATE, 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<IBlockElement> body = new ArrayList<>();

        Table t = grid(3, 1, 1.4f);
        head(t, "Particulars"); headR(t, "Rate"); headR(t, "Amount (Rs)");
        esiRow(t, "Employee's Contribution", "0.75%", esiEE);
        esiRow(t, "Employer's Contribution", "3.25%", esiER);
        total(t, "TOTAL AMOUNT PAYABLE"); total(t, "4.00%"); totalR(t, amt(total));
        body.add(t);

        Table w = grid(1.4f, 1, 1.4f, 1);
        kv(w, "Total ESI Wages", amt(esiWages));
        kv(w, "Wage Ceiling", "Rs 21,000 per month (gross)");
        body.add(w);

        if (total.compareTo(BigDecimal.ZERO) == 0)
            body.add(new Paragraph("No ESI liability for this period - no employee drew "
                    + "monthly gross wages at or below the Rs 21,000 threshold.")
                    .setFontSize(7).setPadding(4).setBorderTop(LINE));

        return render(run, "EMPLOYEES' STATE INSURANCE CORPORATION",
                "CHALLAN FOR PAYMENT OF ESI CONTRIBUTION",
                "ESIC Code: " + dash(co.getEsicRegistrationNumber()),
                total, dueDate(run, 15), body);
    }

    /* ==================================================================
       3. TDS CHALLAN  -  ITNS 281
       ================================================================== */
    private byte[] tdsChallan(PayrollRun run) {
        Company co = run.getCompany();
        BigDecimal tds = nz(run.getTotalTds());

        List<IBlockElement> body = new ArrayList<>();

        Table d = grid(1.3f, 1.5f, 1.3f, 1.5f);
        kv(d, "Type of Payment",   "(200) TDS payable by taxpayer");
        kv(d, "Nature of Payment", "(92B) Salary - Section 192");
        kv(d, "Assessment Year",   assessmentYear(run));
        kv(d, "Deductee Type",     "(0021) Non-Company Deductees");
        body.add(d);

        Table t = grid(3, 1.4f);
        head(t, "Particulars"); headR(t, "Amount (Rs)");
        val(t, "Income Tax");     valR(t, amt(tds));
        val(t, "Surcharge");      valR(t, "0.00");
        val(t, "Education Cess"); valR(t, "0.00");
        val(t, "Interest");       valR(t, "0.00");
        val(t, "Penalty");        valR(t, "0.00");
        total(t, "TOTAL AMOUNT PAYABLE"); totalR(t, amt(tds));
        body.add(t);

        body.add(new Paragraph("Note: Health and Education Cess is already included in the "
                + "computed TDS. Interest under section 201(1A) at 1.5% per month applies "
                + "if the amount is deposited after the due date.")
                .setFontSize(6.5f).setPadding(4).setBorderTop(LINE));

        return render(run, "INCOME TAX DEPARTMENT",
                "CHALLAN NO. / ITNS 281  -  TDS ON SALARY",
                "TAN: " + dash(co.getTanNumber()),
                tds, dueDate(run, 7), body);
    }

    /* ==================================================================
       COMMON RENDERER - teeno challan ka structure same hai
       ================================================================== */
    private byte[] render(PayrollRun run, String authority, String title,
                          String codeLine, BigDecimal total, LocalDate due,
                          List<IBlockElement> body) {

        Company co = run.getCompany();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (Document doc = newA4(new PdfWriter(baos))) {

            Table outer = outerBox();
            Cell box = new Cell().setPadding(0).setBorder(Border.NO_BORDER);

            /* ---- Authority header ---- */
            box.add(new Paragraph(authority).setFontSize(11).setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(6).setMarginBottom(1));
            box.add(new Paragraph("Government of India").setFontSize(7)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(3));
            box.add(titleBar(title));

            /* ---- Establishment details ---- */
            Table est = grid(1.3f, 1.7f, 1.3f, 1.7f);
            kv(est, "Establishment", dash(co.getCompanyName()));
            kv(est, "Registration",  codeLine);
            kv(est, "Address", dash(joinNonBlank(", ",
                    co.getAddressLine1(), co.getCity(), co.getPincode())));
            kv(est, "PAN", dash(co.getPan()));
            kv(est, "Wage Month", Month.of(run.getMonth()) + " " + run.getYear());
            kv(est, "Due Date", due.format(DMY));
            box.add(est);

            /* ---- Challan-specific body ---- */
            for (IBlockElement e : body) {
                box.add(e);
            }

            /* ---- Amount in words ---- */
            Table w = grid(1);
            w.addCell(new Cell()
                    .add(new Paragraph("Amount in words: " + rupeesInWords(total))
                            .setFontSize(8).setBold())
                    .setPadding(5).setBorder(LINE));
            box.add(w);

            /* ---- Payment details (portal bharega) ---- */
            Table pay = grid(1.3f, 1.7f, 1.3f, 1.7f);
            kv(pay, "Mode of Payment", "____________");
            kv(pay, "Bank / Branch",   "____________");
            kv(pay, "CIN / CRN",       "____________");
            kv(pay, "Date of Payment", "____________");
            box.add(pay);

            /* ---- Sign-off ---- */
            Table sign = grid(1, 1);
            sign.addCell(new Cell()
                    .add(new Paragraph("Prepared by: " + dash(co.getSignatoryName()))
                            .setFontSize(7))
                    .add(new Paragraph("Designation: " + dash(co.getSignatoryDesignation()))
                            .setFontSize(7))
                    .add(new Paragraph("Date: " + LocalDate.now().format(DMY)).setFontSize(7))
                    .setPadding(4).setBorder(LINE));
            sign.addCell(new Cell()
                    .add(new Paragraph("\n\n").setFontSize(7))
                    .add(new Paragraph("Authorised Signatory").setFontSize(7)
                            .setTextAlignment(TextAlignment.CENTER).setBorderTop(LINE))
                    .setPadding(4).setBorder(LINE));
            box.add(sign);

            box.add(footerNote("This is a payment advice generated from payroll records. "
                    + "The statutory challan with the CIN/CRN must be generated and paid "
                    + "through the respective government portal. "
                    + "Computer-generated - no signature required."));

            outer.addCell(box);
            doc.add(outer);

        } catch (Exception e) {
            throw new RuntimeException("Error generating challan: " + e.getMessage(), e);
        }

        // try-with-resources BAND hone ke BAAD - tab PDF footer/xref likha jaata hai
        return baos.toByteArray();
    }

    /* ==================================================================
       HELPERS
       ================================================================== */

    /** Wage month ke agle mahine ki given tareekh */
    private LocalDate dueDate(PayrollRun run, int day) {
        return LocalDate.of(run.getYear(), run.getMonth(), 1)
                .plusMonths(1).withDayOfMonth(day);
    }

    /** Wage month ke hisaab se AY (financial year April se shuru) */
    private String assessmentYear(PayrollRun run) {
        int fyStart = run.getMonth() >= 4 ? run.getYear() : run.getYear() - 1;
        return (fyStart + 1) + "-" + String.valueOf(fyStart + 2).substring(2);
    }

    private BigDecimal pct(BigDecimal base, BigDecimal rate) {
        if (base == null || rate == null) return BigDecimal.ZERO;
        return base.multiply(rate).divide(HUNDRED, 0, RoundingMode.HALF_UP);
    }

    private void acRow(Table t, String ac, String desc, BigDecimal amount) {
        valC(t, ac);
        val(t, desc);
        valR(t, amt(amount));
    }

    private void esiRow(Table t, String particulars, String rate, BigDecimal amount) {
        val(t, particulars);
        valR(t, rate);
        valR(t, amt(amount));
    }
}