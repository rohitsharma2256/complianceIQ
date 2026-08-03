package com.complianceiq.service.pdf;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Saare statutory documents ka common base.
 *
 * A4 = 210 x 297 mm (iText mein 595 x 842 points @ 72 pt/inch).
 * PDF vector hota hai - 300 DPI pe print/render karne pe apne aap
 * 2480 x 3508 px banta hai. Alag se DPI set karne ki zarurat nahi.
 */
public abstract class PdfBase {

    /* ---------- Page ---------- */
    protected static final PageSize A4          = PageSize.A4;            // 210 x 297 mm
    protected static final PageSize A4_LANDSCAPE = PageSize.A4.rotate();  // 297 x 210 mm

    protected static final float MARGIN = 20f;   // ~7 mm - govt forms mein compact hota hai

    /* ---------- Colours (govt style - minimal) ---------- */
    protected static final DeviceRgb LIGHT = new DeviceRgb(240, 240, 240);
    protected static final DeviceRgb GREY  = new DeviceRgb(110, 110, 110);

    /* ---------- Borders ---------- */
    protected static final SolidBorder LINE  = new SolidBorder(ColorConstants.BLACK, 0.5f);
    protected static final SolidBorder THICK = new SolidBorder(ColorConstants.BLACK, 1.2f);

    /* ==================================================================
       DOCUMENT SETUP
       ================================================================== */
    protected Document newA4(PdfWriter writer) {
        Document doc = new Document(new PdfDocument(writer), A4);
        doc.setMargins(MARGIN, MARGIN, MARGIN, MARGIN);
        return doc;
    }

    protected Document newA4Landscape(PdfWriter writer) {
        Document doc = new Document(new PdfDocument(writer), A4_LANDSCAPE);
        doc.setMargins(MARGIN, MARGIN, MARGIN, MARGIN);
        return doc;
    }

    /** Poora document ek outer bordered box mein - govt forms ka style */
    protected Table outerBox() {
        return new Table(UnitValue.createPercentArray(new float[]{1}))
                .useAllAvailableWidth().setBorder(THICK);
    }

    protected Table grid(float... widths) {
        return new Table(UnitValue.createPercentArray(widths)).useAllAvailableWidth();
    }

    /* ==================================================================
       CELL HELPERS - har cell bordered (govt grid)
       ================================================================== */
    protected void kv(Table t, String label, String value) {
        t.addCell(new Cell().add(new Paragraph(label).setFontSize(7.5f).setBold())
                .setPadding(3.5f).setBorder(LINE));
        t.addCell(new Cell().add(new Paragraph(value).setFontSize(7.5f))
                .setPadding(3.5f).setBorder(LINE));
    }

    protected void head(Table t, String s) {
        t.addCell(new Cell().add(new Paragraph(s).setFontSize(7.5f).setBold())
                .setBackgroundColor(LIGHT).setPadding(3.5f).setBorder(LINE));
    }

    protected void headR(Table t, String s) {
        t.addCell(new Cell().add(new Paragraph(s).setFontSize(7.5f).setBold()
                        .setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(LIGHT).setPadding(3.5f).setBorder(LINE));
    }

    protected void val(Table t, String s) {
        t.addCell(new Cell().add(new Paragraph(s).setFontSize(8))
                .setPadding(3.5f).setBorder(LINE));
    }

    protected void valC(Table t, String s) {
        t.addCell(new Cell().add(new Paragraph(s).setFontSize(8)
                        .setTextAlignment(TextAlignment.CENTER))
                .setPadding(3.5f).setBorder(LINE));
    }

    protected void valR(Table t, String s) {
        t.addCell(new Cell().add(new Paragraph(s).setFontSize(8)
                        .setTextAlignment(TextAlignment.RIGHT))
                .setPadding(3.5f).setBorder(LINE));
    }

    protected void total(Table t, String s) {
        t.addCell(new Cell().add(new Paragraph(s).setFontSize(8.5f).setBold())
                .setBackgroundColor(LIGHT).setPadding(3.5f).setBorder(LINE));
    }

    protected void totalR(Table t, String s) {
        t.addCell(new Cell().add(new Paragraph(s).setFontSize(8.5f).setBold()
                        .setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(LIGHT).setPadding(3.5f).setBorder(LINE));
    }

    /** Section ka title bar - "PART A", "ANNEXURE I" jaise */
    protected Paragraph titleBar(String s) {
        return new Paragraph(s).setFontSize(9).setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setBackgroundColor(LIGHT).setPadding(3)
                .setBorderTop(LINE).setBorderBottom(LINE);
    }

    protected Paragraph footerNote(String s) {
        return new Paragraph(s).setFontSize(7).setItalic().setFontColor(GREY)
                .setTextAlignment(TextAlignment.CENTER)
                .setBorderTop(LINE).setPaddingTop(3).setPaddingBottom(4);
    }

    /* ==================================================================
       VALUE HELPERS
       ================================================================== */
    protected BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    /** Indian format: 1,23,456.00 */
    protected String amt(BigDecimal v) {
        if (v == null) return "0.00";
        java.text.NumberFormat nf =
                java.text.NumberFormat.getInstance(new java.util.Locale("en", "IN"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(v);
    }

    protected String dash(String s) { return (s == null || s.isBlank()) ? "-" : s; }

    protected String prefix(String p, String s) {
        return (s == null || s.isBlank()) ? "" : p + s;
    }

    protected String joinNonBlank(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            if (sb.length() > 0) sb.append(sep);
            sb.append(p);
        }
        return sb.toString();
    }

    /* ==================================================================
       NUMBER TO WORDS - Indian system (lakh / crore)
       ================================================================== */
    private static final String[] ONES = {"", "One", "Two", "Three", "Four", "Five",
            "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen",
            "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
    private static final String[] TENS = {"", "", "Twenty", "Thirty", "Forty",
            "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};

    protected String rupeesInWords(BigDecimal amount) {
        long n = amount.setScale(0, RoundingMode.HALF_UP).longValue();
        if (n == 0) return "Rupees Zero Only";
        if (n < 0)  return "Amount is negative";

        StringBuilder sb = new StringBuilder("Rupees");
        long crore = n / 10000000;  n %= 10000000;
        long lakh  = n / 100000;    n %= 100000;
        long thou  = n / 1000;      n %= 1000;
        long hund  = n / 100;       n %= 100;

        if (crore > 0) sb.append(two(crore)).append(" Crore");
        if (lakh  > 0) sb.append(two(lakh)).append(" Lakh");
        if (thou  > 0) sb.append(two(thou)).append(" Thousand");
        if (hund  > 0) sb.append(" ").append(ONES[(int) hund]).append(" Hundred");
        if (n     > 0) sb.append(two(n));
        return sb.append(" Only").toString();
    }

    private String two(long n) {
        if (n == 0) return "";
        if (n < 20) return " " + ONES[(int) n];
        String s = " " + TENS[(int) (n / 10)];
        if (n % 10 > 0) s += " " + ONES[(int) (n % 10)];
        return s;
    }
}